package com.taro.inklauncher

import android.app.Application
import com.taro.inklauncher.data.AppRepository
import com.taro.inklauncher.data.IconProcessor
import com.taro.inklauncher.data.LauncherPrefs
import com.taro.inklauncher.data.LayoutStore
import com.taro.inklauncher.data.Prefs
import com.taro.inklauncher.widget.WidgetHostManager
import com.taro.inklauncher.widget.custom.CustomWidgetStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

/** Wadah dependensi sederhana (tanpa DI library agar tetap ringan). */
class InkApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var prefs: Prefs
        private set
    lateinit var prefsState: StateFlow<LauncherPrefs>
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var iconProcessor: IconProcessor
        private set
    lateinit var layoutStore: LayoutStore
        private set
    lateinit var widgetHost: WidgetHostManager
        private set
    lateinit var customWidgets: CustomWidgetStore
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        // Baca sekali secara sinkron agar tema tidak berkedip saat start.
        val initial = runBlocking { prefs.flow.first() }
        prefsState = prefs.flow.stateIn(appScope, SharingStarted.Eagerly, initial)
        appRepository = AppRepository(this, appScope)
        iconProcessor = IconProcessor(this, appRepository)
        layoutStore = LayoutStore(this, appScope)
        widgetHost = WidgetHostManager(this)
        customWidgets = CustomWidgetStore(this, appScope)
        appRepository.start()
    }
}
