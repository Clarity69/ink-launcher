package com.taro.inklauncher.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.BuildConfig
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.R
import com.taro.inklauncher.data.FallbackStyle
import com.taro.inklauncher.data.IconSource
import com.taro.inklauncher.data.Prefs
import com.taro.inklauncher.system.LaunchHelper
import com.taro.inklauncher.ui.common.AppIconImage
import com.taro.inklauncher.ui.common.ChipRow
import com.taro.inklauncher.ui.common.InkChip
import com.taro.inklauncher.ui.common.InkScaffold
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.common.SectionTitle
import com.taro.inklauncher.ui.common.SettingRow
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.LocalInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
private fun Paragraph(text: String) {
    val ink = LocalInk.current
    Text(
        text,
        color = ink.fg,
        fontFamily = Figtree,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

// ---------------- Ikon ----------------

@Composable
fun IconSettings(vm: LauncherViewModel) {
    val prefs = LocalPrefs.current
    val ink = LocalInk.current
    val context = LocalContext.current
    val set = rememberPrefSetter(vm)
    val apps by vm.apps.collectAsStateWithLifecycle()
    val refreshed = stringResource(R.string.msg_icons_refreshed)
    val version by vm.iconProcessor.version.collectAsStateWithLifecycle()
    val sources by produceState<Map<String, IconSource>>(emptyMap(), apps, prefs.fallbackStyle, prefs.forceGrayscale, version) {
        value = withContext(Dispatchers.Default) {
            apps.associate { it.key to vm.iconProcessor.sourceOf(it, prefs.fallbackStyle, it.packageName in prefs.forceGrayscale) }
        }
    }
    val sorted = remember(apps, sources) {
        apps.sortedBy { if (sources[it.key] == IconSource.THEMED) 1 else 0 }
    }
    val sThemed = stringResource(R.string.icon_source_themed)
    val sMaterial = stringResource(R.string.icon_source_material)
    val sGray = stringResource(R.string.icon_source_grayscale)
    val sLetter = stringResource(R.string.icon_source_letter)

    InkScaffold(title = stringResource(R.string.settings_icons), onBack = vm::popScreen) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                SectionTitle(stringResource(R.string.settings_fallback_style))
                ChipRow {
                    InkChip(stringResource(R.string.fallback_material), prefs.fallbackStyle == FallbackStyle.MATERIAL) {
                        vm.setPref(set, Prefs.Keys.FALLBACK_STYLE, FallbackStyle.MATERIAL.name)
                    }
                    InkChip(stringResource(R.string.fallback_grayscale), prefs.fallbackStyle == FallbackStyle.GRAYSCALE) {
                        vm.setPref(set, Prefs.Keys.FALLBACK_STYLE, FallbackStyle.GRAYSCALE.name)
                    }
                }
                SettingRow(stringResource(R.string.settings_refresh_icons), stringResource(R.string.settings_refresh_icons_sub), R.drawable.ic_ms_refresh, onClick = {
                    vm.iconProcessor.clear()
                    Toast.makeText(context, refreshed, Toast.LENGTH_SHORT).show()
                })
                SectionTitle(stringResource(R.string.settings_icon_sources))
                Text(
                    stringResource(R.string.settings_icon_sources_sub),
                    color = ink.muted,
                    fontFamily = Figtree,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(sorted, key = { it.key }) { e ->
                val forced = e.packageName in prefs.forceGrayscale
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(e, 32.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.label, color = ink.fg, fontFamily = Figtree, fontSize = 15.sp)
                        Text(
                            when (sources[e.key]) {
                                IconSource.THEMED -> sThemed
                                IconSource.MATERIAL -> sMaterial
                                IconSource.GRAYSCALE -> sGray
                                IconSource.LETTER -> sLetter
                                null -> "…"
                            },
                            color = ink.muted,
                            fontFamily = Figtree,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(checked = forced, onCheckedChange = { on ->
                        set { vm.prefsStore.toggleInSet(Prefs.Keys.FORCE_GRAYSCALE, e.packageName, on) }
                    })
                }
            }
        }
    }
}

// ---------------- Aplikasi tersembunyi ----------------

@Composable
fun HiddenAppsScreen(vm: LauncherViewModel) {
    val prefs = LocalPrefs.current
    val ink = LocalInk.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    val hidden = apps.filter { it.key in prefs.hiddenApps }
    InkScaffold(title = stringResource(R.string.settings_hidden), onBack = vm::popScreen) {
        if (hidden.isEmpty()) {
            Paragraph(stringResource(R.string.hidden_empty))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(hidden, key = { it.key }) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(e, 32.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(e.label, color = ink.fg, fontFamily = Figtree, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.setHidden(e, false) }) {
                        Text(stringResource(R.string.hidden_show), color = ink.fg)
                    }
                }
            }
        }
    }
}

// ---------------- Cadangan ----------------

@Composable
fun BackupScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val okBackup = stringResource(R.string.msg_backup_done)
    val okRestore = stringResource(R.string.msg_restore_done)
    val invalid = stringResource(R.string.msg_invalid_file)

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching {
                    val json = vm.exportBackup()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                    }
                }.isSuccess
                Toast.makeText(context, if (ok) okBackup else invalid, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = runCatching {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    } ?: error("kosong")
                    vm.importBackup(text)
                }.isSuccess
                Toast.makeText(context, if (ok) okRestore else invalid, Toast.LENGTH_SHORT).show()
            }
        }
    }

    InkScaffold(title = stringResource(R.string.settings_backup), onBack = vm::popScreen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Paragraph(stringResource(R.string.backup_note))
            SettingRow(stringResource(R.string.backup_create), stringResource(R.string.backup_create_sub), R.drawable.ic_ms_upload, onClick = {
                create.launch("inklauncher-backup.json")
            })
            SettingRow(stringResource(R.string.backup_restore), stringResource(R.string.backup_restore_sub), R.drawable.ic_ms_settings_backup_restore, onClick = {
                open.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            })
        }
    }
}

// ---------------- Tentang ----------------

@Composable
fun AboutScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val ink = LocalInk.current
    InkScaffold(title = stringResource(R.string.settings_about), onBack = vm::popScreen) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.app_name),
                color = ink.fg,
                fontFamily = Figtree,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
            )
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                color = ink.muted,
                fontFamily = Figtree,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Paragraph(stringResource(R.string.about_author))
            SectionTitle(stringResource(R.string.about_license_title))
            Paragraph(stringResource(R.string.about_license))
            SectionTitle(stringResource(R.string.about_attribution_title))
            Paragraph(stringResource(R.string.about_attr_lawnchair))
            Paragraph(stringResource(R.string.about_attr_symbols))
            Paragraph(stringResource(R.string.about_attr_fonts))
            SettingRow("GitHub", "https://github.com/Clarity69/ink-launcher", R.drawable.ic_ms_public, onClick = {
                LaunchHelper.url(context, "https://github.com/Clarity69/ink-launcher")
            })
            SettingRow("Lawnchair", "https://github.com/LawnchairLauncher/lawnchair", R.drawable.ic_ms_public, onClick = {
                LaunchHelper.url(context, "https://github.com/LawnchairLauncher/lawnchair")
            })
            SettingRow("GPL-3.0", "https://www.gnu.org/licenses/gpl-3.0.html", R.drawable.ic_ms_menu_book, onClick = {
                LaunchHelper.url(context, "https://www.gnu.org/licenses/gpl-3.0.html")
            })
        }
    }
}
