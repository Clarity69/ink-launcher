package com.taro.inklauncher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.taro.inklauncher.data.HomeItem
import com.taro.inklauncher.data.ItemType
import com.taro.inklauncher.ui.LauncherRoot

/**
 * Activity HOME. launchMode singleTask + stateNotNeeded + excludeFromRecents (lihat manifest)
 * agar gestur home/back/recents sistem (termasuk MIUI) tetap berperilaku benar.
 */
class MainActivity : ComponentActivity() {

    val vm: LauncherViewModel by viewModels()

    private var batteryRegistered = false
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { vm.onBattery(it) }
        }
    }

    private val widgetHost get() = (application as InkApp).widgetHost

    private data class PendingWidget(val id: Int, val info: AppWidgetProviderInfo, val spanX: Int, val spanY: Int)
    private var pending: PendingWidget? = null

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == RESULT_OK) configureOrFinish() else cancelPending()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Callback paling bawah: di layar utama, back tidak melakukan apa-apa.
        // Callback Compose (laci/menu/pengaturan) yang aktif selalu didahulukan,
        // sehingga predictive back tetap bekerja untuk layar-layar tersebut.
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )
        setContent { LauncherRoot(vm) }
        handleIntent(intent, isNew = false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent, isNew = true)
    }

    private fun handleIntent(intent: Intent?, isNew: Boolean) {
        if (intent == null || intent.action != Intent.ACTION_MAIN) return
        val fromLauncherIcon = intent.hasCategory(Intent.CATEGORY_LAUNCHER) ||
            intent.component?.className?.endsWith(".SettingsLauncher") == true
        when {
            fromLauncherIcon -> vm.openSettingsFromLauncher()
            // Tombol/gestur home saat launcher sudah tampil: tutup laci/menu, kembali ke halaman utama.
            isNew -> vm.goHome()
        }
    }

    override fun onStart() {
        super.onStart()
        onStartHooks()
        vm.setVisible(true)
        if (!batteryRegistered) {
            ContextCompat.registerReceiver(
                this,
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )?.let { vm.onBattery(it) }
            batteryRegistered = true
        }
    }

    override fun onStop() {
        super.onStop()
        onStopHooks()
        vm.setVisible(false)
        if (batteryRegistered) {
            runCatching { unregisterReceiver(batteryReceiver) }
            batteryRegistered = false
        }
    }

    override fun onDestroy() {
        widgetHost.clearViews()
        super.onDestroy()
    }

    private fun onStartHooks() = widgetHost.startListening()
    private fun onStopHooks() = widgetHost.stopListening()

    // ---------- Alur tambah widget sistem ----------
    // bindAppWidgetIdIfAllowed -> fallback ACTION_APPWIDGET_BIND -> activity konfigurasi (bila ada).

    fun addWidget(info: AppWidgetProviderInfo, spanX: Int, spanY: Int) {
        cancelPending()
        val id = widgetHost.allocateId()
        pending = PendingWidget(id, info, spanX, spanY)
        if (widgetHost.bindIfAllowed(id, info)) {
            configureOrFinish()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            try {
                bindWidget.launch(intent)
            } catch (e: ActivityNotFoundException) {
                cancelPending()
            }
        }
    }

    private fun configureOrFinish() {
        val p = pending ?: return
        if (p.info.configure != null) {
            try {
                widgetHost.host.startAppWidgetConfigureActivityForResult(this, p.id, 0, REQUEST_CONFIGURE, null)
            } catch (e: Exception) {
                // Beberapa widget tetap berfungsi tanpa konfigurasi.
                finishPending()
            }
        } else {
            finishPending()
        }
    }

    @Deprecated("Dipakai oleh AppWidgetHost.startAppWidgetConfigureActivityForResult")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CONFIGURE) {
            if (resultCode == RESULT_OK) finishPending() else cancelPending()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun finishPending() {
        val p = pending ?: return
        pending = null
        val ok = vm.addItemAtFreeSpot(HomeItem(type = ItemType.WIDGET, appWidgetId = p.id, w = p.spanX, h = p.spanY))
        if (!ok) widgetHost.deleteId(p.id)
        vm.goHome()
    }

    private fun cancelPending() {
        pending?.let { widgetHost.deleteId(it.id) }
        pending = null
    }

    companion object {
        private const val REQUEST_CONFIGURE = 0x1C0
    }
}
