package com.taro.inklauncher.ui.widgeteditor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.R
import com.taro.inklauncher.Screen
import com.taro.inklauncher.ui.common.IconAction
import com.taro.inklauncher.ui.common.InkMenu
import com.taro.inklauncher.ui.common.InkScaffold
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.common.MenuAction
import com.taro.inklauncher.ui.common.SectionTitle
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.LocalInk
import com.taro.inklauncher.widget.custom.CustomWidget
import com.taro.inklauncher.widget.custom.CustomWidgetView
import com.taro.inklauncher.widget.custom.HAlign
import com.taro.inklauncher.widget.custom.Layer
import com.taro.inklauncher.widget.custom.LayerType
import com.taro.inklauncher.widget.custom.Presets
import com.taro.inklauncher.widget.custom.TokenContext
import com.taro.inklauncher.widget.custom.newId
import com.taro.inklauncher.widget.custom.rememberTokenContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Perkiraan rasio tinggi/lebar satu sel grid layar utama. */
@Composable
fun cellAspect(): Float {
    val cfg = LocalConfiguration.current
    val prefs = LocalPrefs.current
    val cellW = cfg.screenWidthDp.toFloat() / prefs.columns
    val cellH = (cfg.screenHeightDp - 320).coerceAtLeast(200).toFloat() / prefs.rows
    return (cellH / cellW).coerceIn(0.6f, 2f)
}

/** Pratinjau widget pada ukuran aslinya, diperkecil bila perlu agar muat. */
@Composable
fun WidgetPreview(
    widget: CustomWidget,
    tokens: TokenContext,
    limitHeight: Dp,
    modifier: Modifier = Modifier,
    selectedLayerId: String? = null,
    onTapLayer: ((Layer?) -> Unit)? = null,
) {
    val prefs = LocalPrefs.current
    val ink = LocalInk.current
    val aspect = cellAspect()
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val cellW = LocalConfiguration.current.screenWidthDp.dp / prefs.columns
        val trueW = cellW * widget.w
        val trueH = cellW * aspect * widget.h
        val scale = minOf(1f, maxWidth / trueW, limitHeight / trueH)
        Box(Modifier.size(trueW * scale, trueH * scale), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .requiredSize(trueW, trueH)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .border(1.dp, ink.muted, RoundedCornerShape(8.dp)),
            ) {
                CustomWidgetView(
                    widget = widget,
                    tokens = tokens,
                    modifier = Modifier.fillMaxSize(),
                    selectedLayerId = selectedLayerId,
                    onTapAny = onTapLayer,
                )
            }
        }
    }
}

@Composable
fun CustomWidgetsScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val ink = LocalInk.current
    val store = vm.customWidgets
    val widgets by store.widgets.collectAsStateWithLifecycle()
    val tokens = rememberTokenContext(vm)
    val scope = rememberCoroutineScope()
    val presets = remember { Presets.all() }
    var exporting by remember { mutableStateOf<CustomWidget?>(null) }

    val msgExported = stringResource(R.string.msg_exported)
    val msgImported = stringResource(R.string.msg_imported)
    val msgInvalid = stringResource(R.string.msg_invalid_file)
    val msgPresetAdded = stringResource(R.string.msg_preset_added)
    val copySuffix = stringResource(R.string.copy_suffix)
    val newName = stringResource(R.string.new_widget_name)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val w = exporting
        exporting = null
        if (uri != null && w != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(store.toJson(w).toByteArray()) }
                    }.isSuccess
                }
                Toast.makeText(context, if (ok) msgExported else msgInvalid, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
                }
                val ok = text != null && runCatching { store.importJson(text) }.isSuccess
                Toast.makeText(context, if (ok) msgImported else msgInvalid, Toast.LENGTH_SHORT).show()
            }
        }
    }

    InkScaffold(
        title = stringResource(R.string.custom_widgets_title),
        onBack = vm::popScreen,
        actions = {
            IconAction(R.drawable.ic_ms_download, stringResource(R.string.action_import)) {
                importLauncher.launch(arrayOf("*/*"))
            }
            IconAction(R.drawable.ic_ms_add, stringResource(R.string.action_add)) {
                val w = CustomWidget(
                    name = newName,
                    layers = listOf(
                        Layer(type = LayerType.TEXT, name = "Jam", text = "{time}", x = 5f, y = 10f, w = 90f, h = 50f, fontSize = 40f, align = HAlign.CENTER),
                        Layer(type = LayerType.TEXT, name = "Tanggal", text = "{day}, {date}", x = 5f, y = 65f, w = 90f, h = 25f, fontSize = 16f, align = HAlign.CENTER),
                    ),
                )
                store.save(w)
                vm.openScreen(Screen.WidgetEditor(w.id))
            }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { SectionTitle(stringResource(R.string.custom_widgets_mine)) }
            if (widgets.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.custom_widgets_empty),
                        color = ink.muted,
                        fontFamily = Figtree,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(widgets, key = { it.id }) { w ->
                var menu by remember { mutableStateOf(false) }
                WidgetCard(w, tokens, onClick = { menu = true }) {
                    InkMenu(
                        expanded = menu,
                        onDismiss = { menu = false },
                        actions = listOf(
                            MenuAction(stringResource(R.string.menu_add_home), R.drawable.ic_ms_add) { vm.addCustomToHome(w) },
                            MenuAction(stringResource(R.string.action_edit), R.drawable.ic_ms_edit) { vm.openScreen(Screen.WidgetEditor(w.id)) },
                            MenuAction(stringResource(R.string.action_duplicate), R.drawable.ic_ms_content_copy) { store.duplicate(w.id, copySuffix) },
                            MenuAction(stringResource(R.string.action_export), R.drawable.ic_ms_upload) {
                                exporting = w
                                val safe = w.name.replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_').ifEmpty { "widget" }
                                exportLauncher.launch("$safe.inkw")
                            },
                            MenuAction(stringResource(R.string.action_delete), R.drawable.ic_ms_delete) { vm.deleteCustomWidget(w.id) },
                        ),
                    )
                }
            }
            item { SectionTitle(stringResource(R.string.custom_widgets_presets)) }
            items(presets, key = { "preset_" + it.name }) { p ->
                WidgetCard(p, tokens, onClick = {
                    store.save(p.copy(id = newId(), layers = p.layers.map { it.copy(id = newId()) }))
                    Toast.makeText(context, msgPresetAdded, Toast.LENGTH_SHORT).show()
                })
            }
            item { Box(Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun WidgetCard(
    widget: CustomWidget,
    tokens: TokenContext,
    onClick: () -> Unit,
    menu: @Composable () -> Unit = {},
) {
    val ink = LocalInk.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.dp, ink.fg, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        WidgetPreview(widget, tokens, limitHeight = 200.dp, onTapLayer = { onClick() })
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(widget.name, color = ink.fg, fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Text("${widget.w} × ${widget.h}", color = ink.muted, fontFamily = Figtree, fontSize = 13.sp)
            }
            Box {
                IconAction(R.drawable.ic_ms_more_vert, null, onClick)
                menu()
            }
        }
    }
}
