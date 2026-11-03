package com.taro.inklauncher.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.R
import com.taro.inklauncher.Screen
import com.taro.inklauncher.SettingsPage
import com.taro.inklauncher.data.IconShape
import com.taro.inklauncher.data.Prefs
import com.taro.inklauncher.data.ThemeMode
import com.taro.inklauncher.system.LaunchHelper
import com.taro.inklauncher.ui.common.ChipRow
import com.taro.inklauncher.ui.common.InkChip
import com.taro.inklauncher.ui.common.InkScaffold
import com.taro.inklauncher.ui.common.LabeledSlider
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.common.SectionTitle
import com.taro.inklauncher.ui.common.SettingRow
import com.taro.inklauncher.ui.common.Stepper
import com.taro.inklauncher.ui.common.SwitchRow
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.LocalInk
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: LauncherViewModel, page: SettingsPage) {
    when (page) {
        SettingsPage.ROOT -> SettingsRoot(vm)
        SettingsPage.APPEARANCE -> AppearanceSettings(vm)
        SettingsPage.ICONS -> IconSettings(vm)
        SettingsPage.GESTURES -> GestureSettings(vm)
        SettingsPage.HIDDEN -> HiddenAppsScreen(vm)
        SettingsPage.BACKUP -> BackupScreen(vm)
        SettingsPage.ABOUT -> AboutScreen(vm)
    }
}

/** Helper: simpan satu nilai preferensi di coroutine. */
@Composable
fun rememberPrefSetter(vm: LauncherViewModel): (suspend () -> Unit) -> Unit {
    val scope = rememberCoroutineScope()
    return { block -> scope.launch { block() } }
}

fun <T> LauncherViewModel.setPref(scope: (suspend () -> Unit) -> Unit, key: Preferences.Key<T>, value: T) {
    scope { prefsStore.set(key, value) }
}

@Composable
private fun SettingsRoot(vm: LauncherViewModel) {
    val context = LocalContext.current
    InkScaffold(title = stringResource(R.string.settings_title), onBack = vm::popScreen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SettingRow(
                stringResource(R.string.settings_make_default),
                stringResource(R.string.settings_make_default_sub),
                R.drawable.ic_ms_home,
                onClick = { LaunchHelper.homeSettings(context) },
            )
            SettingRow(stringResource(R.string.settings_appearance), stringResource(R.string.settings_appearance_sub), R.drawable.ic_ms_palette, onClick = {
                vm.openScreen(Screen.Settings(SettingsPage.APPEARANCE))
            })
            SettingRow(stringResource(R.string.settings_icons), stringResource(R.string.settings_icons_sub), R.drawable.ic_ms_apps, onClick = {
                vm.openScreen(Screen.Settings(SettingsPage.ICONS))
            })
            SettingRow(stringResource(R.string.settings_gestures), stringResource(R.string.settings_gestures_sub), R.drawable.ic_ms_gesture, onClick = {
                vm.openScreen(Screen.Settings(SettingsPage.GESTURES))
            })
            SettingRow(stringResource(R.string.settings_hidden), stringResource(R.string.settings_hidden_sub), R.drawable.ic_ms_visibility_off, onClick = {
                vm.openScreen(Screen.Settings(SettingsPage.HIDDEN))
            })
            SettingRow(stringResource(R.string.menu_custom_widgets), stringResource(R.string.settings_custom_widgets_sub), R.drawable.ic_ms_dashboard_customize, onClick = {
                vm.openScreen(Screen.CustomWidgets)
            })
            SettingRow(stringResource(R.string.settings_backup), stringResource(R.string.settings_backup_sub), R.drawable.ic_ms_backup, onClick = {
                vm.openScreen(Screen.Settings(SettingsPage.BACKUP))
            })
            SettingRow(stringResource(R.string.settings_about), null, R.drawable.ic_ms_info, onClick = {
                vm.openScreen(Screen.Settings(SettingsPage.ABOUT))
            })
        }
    }
}

@Composable
private fun AppearanceSettings(vm: LauncherViewModel) {
    val prefs = LocalPrefs.current
    val context = LocalContext.current
    val set = rememberPrefSetter(vm)
    val denied = stringResource(R.string.msg_permission_denied)

    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.setPref(set, Prefs.Keys.GLANCE_CALENDAR, true) else Toast.makeText(context, denied, Toast.LENGTH_SHORT).show()
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.setPref(set, Prefs.Keys.GLANCE_WEATHER, true) else Toast.makeText(context, denied, Toast.LENGTH_SHORT).show()
    }

    InkScaffold(title = stringResource(R.string.settings_appearance), onBack = vm::popScreen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionTitle(stringResource(R.string.settings_theme))
            ChipRow {
                InkChip(stringResource(R.string.theme_light), prefs.themeMode == ThemeMode.LIGHT) { vm.setPref(set, Prefs.Keys.THEME, ThemeMode.LIGHT.name) }
                InkChip(stringResource(R.string.theme_dark), prefs.themeMode == ThemeMode.DARK) { vm.setPref(set, Prefs.Keys.THEME, ThemeMode.DARK.name) }
                InkChip(stringResource(R.string.theme_auto), prefs.themeMode == ThemeMode.AUTO) { vm.setPref(set, Prefs.Keys.THEME, ThemeMode.AUTO.name) }
            }
            SwitchRow(stringResource(R.string.settings_show_wallpaper), stringResource(R.string.settings_show_wallpaper_sub), prefs.showWallpaper) {
                vm.setPref(set, Prefs.Keys.SHOW_WALLPAPER, it)
            }

            SectionTitle(stringResource(R.string.settings_grid))
            Stepper(stringResource(R.string.settings_columns), prefs.columns, 3, 6) { vm.setPref(set, Prefs.Keys.COLUMNS, it) }
            Stepper(stringResource(R.string.settings_rows), prefs.rows, 3, 8) { vm.setPref(set, Prefs.Keys.ROWS, it) }
            Stepper(stringResource(R.string.settings_pages), prefs.pageCount, 1, 5) { vm.setPref(set, Prefs.Keys.PAGE_COUNT, it) }
            Stepper(stringResource(R.string.settings_dock_count), prefs.dockCount, 4, 5) { vm.setPref(set, Prefs.Keys.DOCK_COUNT, it) }

            SectionTitle(stringResource(R.string.settings_icon_look))
            LabeledSlider(stringResource(R.string.settings_icon_size), prefs.iconSizeDp.toFloat(), 32f..64f, {
                vm.setPref(set, Prefs.Keys.ICON_SIZE, it.toInt())
            }, { "${it.toInt()} dp" })
            ChipRow {
                InkChip(stringResource(R.string.shape_none), prefs.iconShape == IconShape.NONE) { vm.setPref(set, Prefs.Keys.ICON_SHAPE, IconShape.NONE.name) }
                InkChip(stringResource(R.string.shape_circle_outline), prefs.iconShape == IconShape.CIRCLE) { vm.setPref(set, Prefs.Keys.ICON_SHAPE, IconShape.CIRCLE.name) }
                InkChip(stringResource(R.string.shape_squircle_outline), prefs.iconShape == IconShape.SQUIRCLE) { vm.setPref(set, Prefs.Keys.ICON_SHAPE, IconShape.SQUIRCLE.name) }
            }

            SectionTitle(stringResource(R.string.settings_home))
            SwitchRow(stringResource(R.string.settings_show_search), null, prefs.showSearchBar) { vm.setPref(set, Prefs.Keys.SHOW_SEARCH, it) }
            SwitchRow(stringResource(R.string.settings_show_drawer_button), stringResource(R.string.settings_show_drawer_button_sub), prefs.showDrawerButton) {
                vm.setPref(set, Prefs.Keys.SHOW_DRAWER_BUTTON, it)
            }
            SwitchRow(stringResource(R.string.settings_show_glance), null, prefs.showGlance) { vm.setPref(set, Prefs.Keys.SHOW_GLANCE, it) }
            SwitchRow(stringResource(R.string.settings_glance_calendar), stringResource(R.string.settings_glance_calendar_sub), prefs.glanceCalendar) { on ->
                if (!on) {
                    vm.setPref(set, Prefs.Keys.GLANCE_CALENDAR, false)
                } else if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                    vm.setPref(set, Prefs.Keys.GLANCE_CALENDAR, true)
                } else {
                    calendarPermission.launch(Manifest.permission.READ_CALENDAR)
                }
            }
            SwitchRow(stringResource(R.string.settings_glance_weather), stringResource(R.string.settings_glance_weather_sub), prefs.glanceWeather) { on ->
                if (!on) {
                    vm.setPref(set, Prefs.Keys.GLANCE_WEATHER, false)
                } else if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    vm.setPref(set, Prefs.Keys.GLANCE_WEATHER, true)
                } else {
                    locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }

        }
    }
}

@Composable
private fun GestureSettings(vm: LauncherViewModel) {
    val prefs = LocalPrefs.current
    val context = LocalContext.current
    val ink = LocalInk.current
    val set = rememberPrefSetter(vm)
    InkScaffold(title = stringResource(R.string.settings_gestures), onBack = vm::popScreen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.gestures_note),
                color = ink.muted,
                fontFamily = Figtree,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp),
            )
            SwitchRow(stringResource(R.string.gesture_swipe_up), null, prefs.gestureSwipeUp) { vm.setPref(set, Prefs.Keys.GESTURE_UP, it) }
            SwitchRow(stringResource(R.string.gesture_swipe_down), null, prefs.gestureSwipeDown) { vm.setPref(set, Prefs.Keys.GESTURE_DOWN, it) }
            SwitchRow(stringResource(R.string.gesture_double_tap), stringResource(R.string.gesture_double_tap_sub), prefs.gestureDoubleTap) {
                vm.setPref(set, Prefs.Keys.GESTURE_DOUBLE_TAP, it)
            }
            val running = com.taro.inklauncher.system.LockAccessibilityService.isRunning
            SettingRow(
                stringResource(R.string.gesture_a11y),
                stringResource(if (running) R.string.gesture_a11y_on else R.string.gesture_a11y_off),
                R.drawable.ic_ms_lock,
                onClick = { LaunchHelper.accessibilitySettings(context) },
            )
        }
    }
}
