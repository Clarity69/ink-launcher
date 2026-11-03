package com.taro.inklauncher.system

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Perbaikan gestur layar penuh MIUI: beberapa versi MIUI mematikan gestur layar penuh
 * saat launcher pihak ketiga dijadikan default. Nilai global `force_fsg_nav_bar = 1`
 * memaksanya tetap aktif. Bergantung pada versi MIUI — bukan jaminan.
 */
object GestureFixer {

    const val KEY = "force_fsg_nav_bar"
    const val ADB_ON = "adb shell settings put global force_fsg_nav_bar 1"
    const val ADB_OFF = "adb shell settings put global force_fsg_nav_bar 0"
    const val ADB_GET = "adb shell settings get global force_fsg_nav_bar"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    const val REQUEST_CODE = 4242

    fun isShizukuInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    }.getOrDefault(false)

    fun isShizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    fun hasPermission(): Boolean = isShizukuRunning() && try {
        !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    fun requestPermission() {
        try {
            if (isShizukuRunning() && !Shizuku.isPreV11()) Shizuku.requestPermission(REQUEST_CODE)
        } catch (t: Throwable) {
            // abaikan
        }
    }

    /** Membaca nilai saat ini; null bila tidak bisa dibaca. */
    suspend fun readValue(context: Context): String? = withContext(Dispatchers.IO) {
        val direct = runCatching { Settings.Global.getString(context.contentResolver, KEY) }.getOrNull()
        if (direct != null) return@withContext direct
        if (hasPermission()) {
            exec("settings", "get", "global", KEY).getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }

    suspend fun setValue(value: Int): Result<String> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext Result.failure(IllegalStateException("Shizuku belum diizinkan"))
        exec("settings", "put", "global", KEY, value.toString())
    }

    /**
     * Shizuku.newProcess bersifat private di API 13, jadi dipanggil lewat refleksi.
     * Perintah berjalan sebagai shell (adb), cukup untuk `settings put global`.
     */
    private fun exec(vararg cmd: String): Result<String> = runCatching {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        val process = method.invoke(null, arrayOf(*cmd), null, null) as Process
        val out = process.inputStream.bufferedReader().use { it.readText() }
        val err = process.errorStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()
        if (code != 0) error(err.ifBlank { "exit $code" })
        out
    }
}
