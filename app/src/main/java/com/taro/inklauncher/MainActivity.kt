package com.taro.inklauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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

    /** Diisi tahap widget (AppWidgetHost start/stopListening). */
    private fun onStartHooks() = Unit
    private fun onStopHooks() = Unit
}
