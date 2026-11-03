package com.taro.inklauncher

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.BatteryManager
import android.os.Process
import android.provider.MediaStore
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taro.inklauncher.data.AppEntry
import com.taro.inklauncher.data.Backup
import com.taro.inklauncher.data.BackupJson
import com.taro.inklauncher.data.GlanceProvider
import com.taro.inklauncher.data.DOCK_SLOTS
import com.taro.inklauncher.data.HomeItem
import com.taro.inklauncher.data.HomeLayout
import com.taro.inklauncher.data.ItemType
import com.taro.inklauncher.data.LauncherPrefs
import com.taro.inklauncher.data.Prefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface MoveSource {
    data class Grid(val itemId: String) : MoveSource
    data class Dock(val index: Int) : MoveSource
}

sealed interface DropTarget {
    data class Cell(val page: Int, val x: Int, val y: Int) : DropTarget
    data class Dock(val index: Int) : DropTarget
}

enum class SettingsPage { ROOT, APPEARANCE, ICONS, GESTURES, MIUI_FIX, HIDDEN, BACKUP, ABOUT }

sealed interface Screen {
    data class Settings(val page: SettingsPage = SettingsPage.ROOT) : Screen
    data object WidgetPicker : Screen
    data object CustomWidgets : Screen
    data class WidgetEditor(val id: String) : Screen
}

data class BatteryState(val level: Int = -1, val charging: Boolean = false)

data class GlanceLine(@DrawableRes val icon: Int, val text: String)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val ink = application as InkApp
    val repo = ink.appRepository
    val iconProcessor = ink.iconProcessor
    val layoutStore = ink.layoutStore
    val widgetHost = ink.widgetHost
    val customWidgets = ink.customWidgets
    val prefsStore: Prefs = ink.prefs
    val prefs: StateFlow<LauncherPrefs> = ink.prefsState
    val layout: StateFlow<HomeLayout> = layoutStore.layout
    val apps: StateFlow<List<AppEntry>> = repo.apps

    val appMap: StateFlow<Map<String, AppEntry>> = apps
        .map { list -> list.associateBy { it.key } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val drawerApps: StateFlow<List<AppEntry>> = combine(apps, prefs) { list, p ->
        list.filter { it.key !in p.hiddenApps }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val drawerOpen = MutableStateFlow(false)
    val screens = MutableStateFlow<List<Screen>>(emptyList())
    val moving = MutableStateFlow<MoveSource?>(null)
    val resizing = MutableStateFlow<String?>(null)
    /** Posisi (koordinat root) menu long-press area kosong. */
    val homeMenu = MutableStateFlow<Offset?>(null)

    val canGoBack: StateFlow<Boolean> = combine(drawerOpen, screens, moving, resizing, homeMenu) { d, s, m, r, h ->
        d || s.isNotEmpty() || m != null || r != null || h != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _homeReset = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeReset: SharedFlow<Unit> = _homeReset

    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages

    val now = MutableStateFlow(System.currentTimeMillis())
    val battery = MutableStateFlow(BatteryState())
    val glanceLine = MutableStateFlow<GlanceLine?>(null)

    private var tickJob: Job? = null
    private var glanceJob: Job? = null
    private val glance = GlanceProvider(application)
    private var weatherLine: GlanceLine? = null
    private var lastWeatherAt = 0L
    var visible = false
        private set

    init {
        viewModelScope.launch {
            repo.packageChanged.collect { pkg ->
                iconProcessor.invalidatePackage(pkg)
                cleanupRemoved(pkg)
            }
        }
        viewModelScope.launch {
            repo.loaded.first { it }
            ensureDefaultLayout()
        }
        // Ukuran grid berubah -> rapikan item yang keluar batas.
        viewModelScope.launch {
            prefs.map { Triple(it.columns, it.rows, it.pageCount) }
                .distinctUntilChanged()
                .drop(1)
                .collect { (c, r, p) -> clampToGrid(c, r, p) }
        }
        // Fitur At a Glance diaktifkan/dimatikan -> segarkan segera.
        viewModelScope.launch {
            prefs.map { it.glanceCalendar to it.glanceWeather }
                .distinctUntilChanged()
                .drop(1)
                .collect { refreshGlance(force = true) }
        }
    }

    suspend fun refreshGlance(force: Boolean = false) {
        val p = prefs.value
        val calendarLine = if (p.glanceCalendar) glance.nextEvent() else null
        if (p.glanceWeather) {
            val t = System.currentTimeMillis()
            val stale = t - lastWeatherAt > 30 * 60_000L
            val retry = weatherLine == null && t - lastWeatherAt > 5 * 60_000L
            if (force || stale || retry) {
                lastWeatherAt = t
                glance.weather()?.let { weatherLine = it }
            }
        } else {
            weatherLine = null
            lastWeatherAt = 0L
        }
        glanceLine.value = calendarLine ?: weatherLine
    }

    fun toast(@StringRes res: Int) {
        _messages.tryEmit(res)
    }

    // ---------- Navigasi ----------

    fun goHome() {
        homeMenu.value = null
        moving.value = null
        resizing.value = null
        screens.value = emptyList()
        drawerOpen.value = false
        _homeReset.tryEmit(Unit)
    }

    fun back() {
        when {
            homeMenu.value != null -> homeMenu.value = null
            moving.value != null -> moving.value = null
            resizing.value != null -> resizing.value = null
            screens.value.isNotEmpty() -> screens.value = screens.value.dropLast(1)
            drawerOpen.value -> drawerOpen.value = false
        }
    }

    fun openDrawer() {
        if (moving.value != null || resizing.value != null) return
        homeMenu.value = null
        drawerOpen.value = true
    }

    fun closeDrawer() {
        drawerOpen.value = false
    }

    fun openScreen(screen: Screen) {
        homeMenu.value = null
        screens.value = screens.value + screen
    }

    fun popScreen() {
        screens.value = screens.value.dropLast(1)
    }

    fun openSettingsFromLauncher() {
        drawerOpen.value = false
        screens.value = listOf(Screen.Settings())
    }

    // ---------- Visibilitas & ticker ----------

    fun setVisible(v: Boolean) {
        visible = v
        tickJob?.cancel()
        glanceJob?.cancel()
        if (v) {
            glanceJob = viewModelScope.launch {
                while (isActive) {
                    refreshGlance()
                    delay(60_000L)
                }
            }
            tickJob = viewModelScope.launch {
                while (isActive) {
                    val t = System.currentTimeMillis()
                    now.value = t
                    delay(60_000L - (t % 60_000L) + 50L)
                }
            }
        }
    }

    fun onBattery(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        battery.value = BatteryState(if (level >= 0) level * 100 / scale else -1, charging)
    }

    // ---------- Aplikasi ----------

    fun launch(entry: AppEntry, bounds: Rect? = null) {
        repo.launch(entry, bounds)
        if (drawerOpen.value) {
            viewModelScope.launch {
                delay(350)
                drawerOpen.value = false
            }
        }
    }

    fun setHidden(entry: AppEntry, hidden: Boolean) {
        viewModelScope.launch { prefsStore.toggleInSet(Prefs.Keys.HIDDEN_APPS, entry.key, hidden) }
    }

    fun addToHome(key: String) {
        val p = prefs.value
        var added = false
        layoutStore.update { l ->
            val spot = l.firstFree(1, 1, p.columns, p.rows, p.pageCount) ?: return@update l
            added = true
            l.copy(items = l.items + HomeItem(type = ItemType.APP, appKey = key, page = spot.first, x = spot.second, y = spot.third))
        }
        toast(if (added) R.string.msg_added_home else R.string.msg_home_full)
    }

    fun removeItem(id: String) {
        val item = layout.value.items.firstOrNull { it.id == id } ?: return
        onItemRemoved(item)
        layoutStore.update { l -> l.copy(items = l.items.filterNot { it.id == id }) }
    }

    /** Dipanggil sebelum item dihapus: lepaskan AppWidget id agar tidak bocor. */
    private fun onItemRemoved(item: HomeItem) {
        if (item.type == ItemType.WIDGET && item.appWidgetId != -1) widgetHost.deleteId(item.appWidgetId)
    }

    fun deleteCustomWidget(id: String) {
        customWidgets.delete(id)
        layoutStore.update { l -> l.copy(items = l.items.filterNot { it.type == ItemType.CUSTOM && it.customId == id }) }
    }

    fun addCustomToHome(widget: com.taro.inklauncher.widget.custom.CustomWidget) {
        val p = prefs.value
        val ok = addItemAtFreeSpot(
            HomeItem(
                type = ItemType.CUSTOM,
                customId = widget.id,
                w = widget.w.coerceIn(1, p.columns),
                h = widget.h.coerceIn(1, p.rows),
            ),
        )
        if (ok) {
            toast(R.string.msg_added_home)
            goHome()
        }
    }

    fun startResize(id: String) {
        homeMenu.value = null
        resizing.value = id
    }

    fun removeFromDock(index: Int) {
        layoutStore.update { l ->
            val d = l.dockSafe().toMutableList()
            d[index] = null
            l.copy(dock = d)
        }
    }

    fun startMove(source: MoveSource) {
        homeMenu.value = null
        moving.value = source
    }

    fun dropMove(target: DropTarget) {
        val src = moving.value ?: return
        drop(src, target)
        moving.value = null
    }

    fun drop(source: MoveSource, target: DropTarget) {
        val p = prefs.value
        layoutStore.update { l -> applyDrop(l, source, target, p) ?: l }
    }

    private fun applyDrop(l: HomeLayout, s: MoveSource, t: DropTarget, p: LauncherPrefs): HomeLayout? {
        val dock = l.dockSafe().toMutableList()
        val cols = p.columns
        val rows = p.rows
        return when (s) {
            is MoveSource.Grid -> {
                val item = l.items.firstOrNull { it.id == s.itemId } ?: return null
                when (t) {
                    is DropTarget.Cell -> {
                        if (l.isFree(t.page, t.x, t.y, item.w, item.h, cols, rows, ignoreId = item.id)) {
                            l.copy(items = l.items.map { if (it.id == item.id) it.copy(page = t.page, x = t.x, y = t.y) else it })
                        } else {
                            val other = l.itemAt(t.page, t.x, t.y)
                            if (other != null && other.id != item.id && item.w == 1 && item.h == 1 && other.w == 1 && other.h == 1) {
                                l.copy(
                                    items = l.items.map {
                                        when (it.id) {
                                            item.id -> it.copy(page = other.page, x = other.x, y = other.y)
                                            other.id -> it.copy(page = item.page, x = item.x, y = item.y)
                                            else -> it
                                        }
                                    },
                                )
                            } else {
                                null
                            }
                        }
                    }
                    is DropTarget.Dock -> {
                        if (item.type != ItemType.APP || t.index !in 0 until DOCK_SLOTS) return null
                        val prev = dock[t.index]
                        dock[t.index] = item.appKey
                        val items = if (prev != null) {
                            l.items.map { if (it.id == item.id) it.copy(appKey = prev) else it }
                        } else {
                            l.items.filterNot { it.id == item.id }
                        }
                        l.copy(items = items, dock = dock)
                    }
                }
            }
            is MoveSource.Dock -> {
                val key = dock.getOrNull(s.index) ?: return null
                when (t) {
                    is DropTarget.Dock -> {
                        if (t.index !in 0 until DOCK_SLOTS) return null
                        dock[s.index] = dock[t.index]
                        dock[t.index] = key
                        l.copy(dock = dock)
                    }
                    is DropTarget.Cell -> {
                        if (l.isFree(t.page, t.x, t.y, 1, 1, cols, rows)) {
                            dock[s.index] = null
                            l.copy(
                                items = l.items + HomeItem(type = ItemType.APP, appKey = key, page = t.page, x = t.x, y = t.y),
                                dock = dock,
                            )
                        } else {
                            val other = l.itemAt(t.page, t.x, t.y)
                            if (other != null && other.type == ItemType.APP && other.w == 1 && other.h == 1) {
                                dock[s.index] = other.appKey
                                l.copy(
                                    items = l.items.map { if (it.id == other.id) it.copy(appKey = key) else it },
                                    dock = dock,
                                )
                            } else {
                                null
                            }
                        }
                    }
                }
            }
        }
    }

    fun resizeItem(id: String, w: Int, h: Int) {
        val p = prefs.value
        layoutStore.update { l ->
            val item = l.items.firstOrNull { it.id == id } ?: return@update l
            val nw = w.coerceIn(1, p.columns - item.x)
            val nh = h.coerceIn(1, p.rows - item.y)
            if (!l.isFree(item.page, item.x, item.y, nw, nh, p.columns, p.rows, ignoreId = id)) return@update l
            l.copy(items = l.items.map { if (it.id == id) it.copy(w = nw, h = nh) else it })
        }
    }

    fun addItemAtFreeSpot(item: HomeItem): Boolean {
        val p = prefs.value
        var ok = false
        layoutStore.update { l ->
            val spot = l.firstFree(item.w, item.h, p.columns, p.rows, p.pageCount) ?: return@update l
            ok = true
            l.copy(items = l.items + item.copy(page = spot.first, x = spot.second, y = spot.third))
        }
        if (!ok) toast(R.string.msg_home_full)
        return ok
    }

    private fun cleanupRemoved(pkg: String) {
        val app = getApplication<Application>()
        val stillInstalled = runCatching {
            app.packageManager.getApplicationInfo(pkg, 0)
            true
        }.getOrDefault(false)
        if (stillInstalled) return
        fun isPkg(key: String?) = key != null && key.substringBefore('/') == pkg
        layoutStore.update { l ->
            l.copy(
                items = l.items.filterNot { it.type == ItemType.APP && isPkg(it.appKey) },
                dock = l.dockSafe().map { if (isPkg(it)) null else it },
            )
        }
    }

    // ---------- Tata letak bawaan ----------

    private fun ensureDefaultLayout() {
        if (layout.value.initialized) return
        val app = getApplication<Application>()
        val pm = app.packageManager
        val me = Process.myUserHandle()
        val list = apps.value

        fun pkgFor(intent: Intent): String? = runCatching {
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        }.getOrNull()?.takeIf { it != "android" }

        fun entryFor(vararg pkgs: String?): String? {
            for (p in pkgs) {
                if (p == null) continue
                list.firstOrNull { it.packageName == p && it.user == me }?.let { return it.key }
            }
            return null
        }

        val dial = entryFor(pkgFor(Intent(Intent.ACTION_DIAL)), "com.google.android.dialer", "com.android.dialer", "com.android.contacts")
        val sms = entryFor(pkgFor(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))), "com.google.android.apps.messaging", "com.android.mms")
        val browser = entryFor(
            pkgFor(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)),
            "com.android.chrome", "com.mi.globalbrowser",
        )
        val camera = entryFor(pkgFor(Intent(MediaStore.ACTION_IMAGE_CAPTURE)), "com.android.camera", "com.google.android.GoogleCamera")
        val dock = mutableListOf(dial, sms, browser, camera, null)

        val p = prefs.value
        val homeApps = listOfNotNull(
            entryFor("com.google.android.apps.photos", "com.miui.gallery"),
            entryFor("com.google.android.deskclock", "com.android.deskclock"),
            entryFor("com.google.android.calendar", "com.xiaomi.calendar", "com.android.calendar"),
            entryFor("com.android.vending"),
        ).filter { it !in dock }.take(p.columns)
        val y = p.rows - 1
        val items = homeApps.mapIndexed { i, key -> HomeItem(type = ItemType.APP, appKey = key, x = i, y = y) }
        layoutStore.update { l ->
            if (l.initialized) l else l.copy(items = l.items + items, dock = dock, initialized = true)
        }
    }

    // ---------- Cadangan ----------

    suspend fun exportBackup(): String {
        val backup = Backup(
            layout = layout.value.copy(items = layout.value.items.filterNot { it.type == ItemType.WIDGET }),
            customWidgets = customWidgets.widgets.value,
            prefs = prefsStore.exportMap(prefs.value),
        )
        return BackupJson.encodeToString(Backup.serializer(), backup)
    }

    suspend fun importBackup(text: String) {
        val backup = BackupJson.decodeFromString(Backup.serializer(), text)
        layout.value.items.filter { it.type == ItemType.WIDGET }.forEach { onItemRemoved(it) }
        layoutStore.replace(
            backup.layout.copy(
                items = backup.layout.items.filterNot { it.type == ItemType.WIDGET },
                initialized = true,
            ),
        )
        customWidgets.replaceAll(backup.customWidgets)
        prefsStore.importMap(backup.prefs)
        val p = prefsStore.flow.first()
        clampToGrid(p.columns, p.rows, p.pageCount)
    }

    fun clampToGrid(cols: Int, rows: Int, pages: Int) {
        layoutStore.update { l ->
            val kept = ArrayList<HomeItem>()
            val overflow = ArrayList<HomeItem>()
            for (it in l.items) {
                if (it.page < pages && it.x + it.w <= cols && it.y + it.h <= rows) kept += it else overflow += it
            }
            var cur = l.copy(items = kept)
            for (it in overflow) {
                val w = it.w.coerceAtMost(cols)
                val h = it.h.coerceAtMost(rows)
                val spot = cur.firstFree(w, h, cols, rows, pages)
                if (spot != null) {
                    cur = cur.copy(items = cur.items + it.copy(page = spot.first, x = spot.second, y = spot.third, w = w, h = h))
                } else {
                    onItemRemoved(it)
                }
            }
            cur
        }
    }
}
