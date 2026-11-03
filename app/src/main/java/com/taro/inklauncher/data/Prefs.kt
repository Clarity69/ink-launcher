package com.taro.inklauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode { LIGHT, DARK, AUTO }
enum class IconShape { NONE, CIRCLE, SQUIRCLE }
enum class FallbackStyle { MATERIAL, GRAYSCALE }

data class LauncherPrefs(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val columns: Int = 4,
    val rows: Int = 5,
    val iconSizeDp: Int = 44,
    val iconShape: IconShape = IconShape.NONE,
    val showSearchBar: Boolean = true,
    val showGlance: Boolean = true,
    val glanceCalendar: Boolean = false,
    val glanceWeather: Boolean = false,
    val pageCount: Int = 1,
    val dockCount: Int = 4,
    val drawerAutoKeyboard: Boolean = true,
    val fallbackStyle: FallbackStyle = FallbackStyle.MATERIAL,
    val forceGrayscale: Set<String> = emptySet(),
    val hiddenApps: Set<String> = emptySet(),
    val gestureSwipeUp: Boolean = true,
    val gestureSwipeDown: Boolean = true,
    val gestureDoubleTap: Boolean = false,
    val showWallpaper: Boolean = false,
)

private val Context.inkDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class Prefs(context: Context) {

    private val store = context.applicationContext.inkDataStore

    object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val COLUMNS = intPreferencesKey("columns")
        val ROWS = intPreferencesKey("rows")
        val ICON_SIZE = intPreferencesKey("icon_size_dp")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val SHOW_SEARCH = booleanPreferencesKey("show_search_bar")
        val SHOW_GLANCE = booleanPreferencesKey("show_glance")
        val GLANCE_CALENDAR = booleanPreferencesKey("glance_calendar")
        val GLANCE_WEATHER = booleanPreferencesKey("glance_weather")
        val PAGE_COUNT = intPreferencesKey("page_count")
        val DOCK_COUNT = intPreferencesKey("dock_count")
        val DRAWER_KEYBOARD = booleanPreferencesKey("drawer_auto_keyboard")
        val FALLBACK_STYLE = stringPreferencesKey("fallback_style")
        val FORCE_GRAYSCALE = stringSetPreferencesKey("force_grayscale")
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val GESTURE_UP = booleanPreferencesKey("gesture_swipe_up")
        val GESTURE_DOWN = booleanPreferencesKey("gesture_swipe_down")
        val GESTURE_DOUBLE_TAP = booleanPreferencesKey("gesture_double_tap")
        val SHOW_WALLPAPER = booleanPreferencesKey("show_wallpaper")
    }

    val flow: Flow<LauncherPrefs> = store.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
            val d = LauncherPrefs()
            LauncherPrefs(
                themeMode = enumOr(p[Keys.THEME], d.themeMode),
                columns = (p[Keys.COLUMNS] ?: d.columns).coerceIn(3, 6),
                rows = (p[Keys.ROWS] ?: d.rows).coerceIn(3, 8),
                iconSizeDp = (p[Keys.ICON_SIZE] ?: d.iconSizeDp).coerceIn(32, 64),
                iconShape = enumOr(p[Keys.ICON_SHAPE], d.iconShape),
                showSearchBar = p[Keys.SHOW_SEARCH] ?: d.showSearchBar,
                showGlance = p[Keys.SHOW_GLANCE] ?: d.showGlance,
                glanceCalendar = p[Keys.GLANCE_CALENDAR] ?: d.glanceCalendar,
                glanceWeather = p[Keys.GLANCE_WEATHER] ?: d.glanceWeather,
                pageCount = (p[Keys.PAGE_COUNT] ?: d.pageCount).coerceIn(1, 5),
                dockCount = (p[Keys.DOCK_COUNT] ?: d.dockCount).coerceIn(4, 5),
                drawerAutoKeyboard = p[Keys.DRAWER_KEYBOARD] ?: d.drawerAutoKeyboard,
                fallbackStyle = enumOr(p[Keys.FALLBACK_STYLE], d.fallbackStyle),
                forceGrayscale = p[Keys.FORCE_GRAYSCALE] ?: d.forceGrayscale,
                hiddenApps = p[Keys.HIDDEN_APPS] ?: d.hiddenApps,
                gestureSwipeUp = p[Keys.GESTURE_UP] ?: d.gestureSwipeUp,
                gestureSwipeDown = p[Keys.GESTURE_DOWN] ?: d.gestureSwipeDown,
                gestureDoubleTap = p[Keys.GESTURE_DOUBLE_TAP] ?: d.gestureDoubleTap,
                showWallpaper = p[Keys.SHOW_WALLPAPER] ?: d.showWallpaper,
            )
        }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    suspend fun toggleInSet(key: Preferences.Key<Set<String>>, value: String, add: Boolean) {
        store.edit { p ->
            val cur = p[key] ?: emptySet()
            p[key] = if (add) cur + value else cur - value
        }
    }

    /** Untuk cadangan: semua nilai sebagai string sederhana. */
    suspend fun exportMap(current: LauncherPrefs): Map<String, String> = mapOf(
        Keys.THEME.name to current.themeMode.name,
        Keys.COLUMNS.name to current.columns.toString(),
        Keys.ROWS.name to current.rows.toString(),
        Keys.ICON_SIZE.name to current.iconSizeDp.toString(),
        Keys.ICON_SHAPE.name to current.iconShape.name,
        Keys.SHOW_SEARCH.name to current.showSearchBar.toString(),
        Keys.SHOW_GLANCE.name to current.showGlance.toString(),
        Keys.PAGE_COUNT.name to current.pageCount.toString(),
        Keys.DOCK_COUNT.name to current.dockCount.toString(),
        Keys.DRAWER_KEYBOARD.name to current.drawerAutoKeyboard.toString(),
        Keys.FALLBACK_STYLE.name to current.fallbackStyle.name,
        Keys.FORCE_GRAYSCALE.name to current.forceGrayscale.joinToString("\n"),
        Keys.HIDDEN_APPS.name to current.hiddenApps.joinToString("\n"),
        Keys.GESTURE_UP.name to current.gestureSwipeUp.toString(),
        Keys.GESTURE_DOWN.name to current.gestureSwipeDown.toString(),
        Keys.GESTURE_DOUBLE_TAP.name to current.gestureDoubleTap.toString(),
        Keys.SHOW_WALLPAPER.name to current.showWallpaper.toString(),
    )

    suspend fun importMap(map: Map<String, String>) {
        store.edit { p ->
            fun str(k: Preferences.Key<String>) { map[k.name]?.let { p[k] = it } }
            fun int(k: Preferences.Key<Int>) { map[k.name]?.toIntOrNull()?.let { p[k] = it } }
            fun bool(k: Preferences.Key<Boolean>) { map[k.name]?.toBooleanStrictOrNull()?.let { p[k] = it } }
            fun strSet(k: Preferences.Key<Set<String>>) {
                map[k.name]?.let { v -> p[k] = v.split("\n").filter { it.isNotBlank() }.toSet() }
            }
            str(Keys.THEME); int(Keys.COLUMNS); int(Keys.ROWS); int(Keys.ICON_SIZE)
            str(Keys.ICON_SHAPE); bool(Keys.SHOW_SEARCH); bool(Keys.SHOW_GLANCE)
            int(Keys.PAGE_COUNT); int(Keys.DOCK_COUNT); bool(Keys.DRAWER_KEYBOARD)
            str(Keys.FALLBACK_STYLE); strSet(Keys.FORCE_GRAYSCALE); strSet(Keys.HIDDEN_APPS)
            bool(Keys.GESTURE_UP); bool(Keys.GESTURE_DOWN); bool(Keys.GESTURE_DOUBLE_TAP)
            bool(Keys.SHOW_WALLPAPER)
        }
    }

    private inline fun <reified E : Enum<E>> enumOr(value: String?, default: E): E =
        value?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default
}
