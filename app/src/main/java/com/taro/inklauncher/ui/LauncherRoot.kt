package com.taro.inklauncher.ui

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.Screen
import com.taro.inklauncher.data.ThemeMode
import com.taro.inklauncher.ui.common.LocalIconProcessor
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.drawer.AppDrawer
import com.taro.inklauncher.ui.home.HomeScreen
import com.taro.inklauncher.ui.settings.SettingsScreen
import com.taro.inklauncher.ui.theme.InkMotion
import com.taro.inklauncher.ui.theme.InkTheme
import com.taro.inklauncher.ui.theme.LocalInk
import com.taro.inklauncher.ui.widgeteditor.CustomWidgetsScreen
import com.taro.inklauncher.ui.widgeteditor.WidgetEditorScreen
import com.taro.inklauncher.ui.widgeteditor.WidgetPickerScreen

@Composable
fun LauncherRoot(vm: LauncherViewModel) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val dark = when (prefs.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val context = LocalContext.current

    LaunchedEffect(dark) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        val style = if (dark) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    LaunchedEffect(Unit) {
        vm.messages.collect { res -> Toast.makeText(context, res, Toast.LENGTH_SHORT).show() }
    }

    CompositionLocalProvider(
        LocalPrefs provides prefs,
        LocalIconProcessor provides vm.iconProcessor,
    ) {
        InkTheme(dark = dark) {
            val ink = LocalInk.current
            val drawerOpen by vm.drawerOpen.collectAsStateWithLifecycle()
            val screens by vm.screens.collectAsStateWithLifecycle()
            val canGoBack by vm.canGoBack.collectAsStateWithLifecycle()

            BackHandler(enabled = canGoBack) { vm.back() }

            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (prefs.showWallpaper) Modifier else Modifier.background(ink.bg)),
            ) {
                HomeScreen(vm)
                AppDrawer(vm, drawerOpen)
                AnimatedContent(
                    targetState = screens.lastOrNull(),
                    transitionSpec = { fadeIn(tween(InkMotion.SHORT)) togetherWith fadeOut(tween(InkMotion.SHORT)) },
                    label = "screens",
                ) { screen ->
                    if (screen != null) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(ink.bg)
                                .pointerInput(Unit) { detectTapGestures { } },
                        ) {
                            when (screen) {
                                is Screen.Settings -> SettingsScreen(vm, screen.page)
                                Screen.WidgetPicker -> WidgetPickerScreen(vm)
                                Screen.CustomWidgets -> CustomWidgetsScreen(vm)
                                is Screen.WidgetEditor -> WidgetEditorScreen(vm, screen.id)
                            }
                        }
                    }
                }
            }
        }
    }
}
