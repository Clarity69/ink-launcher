package com.taro.inklauncher.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.R
import com.taro.inklauncher.SettingsPage
import com.taro.inklauncher.system.LaunchHelper
import com.taro.inklauncher.ui.home.safeInsets
import com.taro.inklauncher.ui.theme.LocalInk

@Composable
fun SettingsScreen(vm: LauncherViewModel, page: SettingsPage) {
    val ink = LocalInk.current
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(safeInsets())
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), color = ink.fg)
        OutlinedButton(onClick = { LaunchHelper.homeSettings(context) }) {
            Text(stringResource(R.string.settings_make_default))
        }
    }
}
