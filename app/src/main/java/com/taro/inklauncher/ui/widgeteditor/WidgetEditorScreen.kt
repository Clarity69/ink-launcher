package com.taro.inklauncher.ui.widgeteditor

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.R
import com.taro.inklauncher.data.MaterialSymbols
import com.taro.inklauncher.ui.common.AppIconImage
import com.taro.inklauncher.ui.common.ChipRow
import com.taro.inklauncher.ui.common.IconAction
import com.taro.inklauncher.ui.common.InkChip
import com.taro.inklauncher.ui.common.InkMenu
import com.taro.inklauncher.ui.common.InkScaffold
import com.taro.inklauncher.ui.common.InkTextField
import com.taro.inklauncher.ui.common.LabeledSlider
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.common.MenuAction
import com.taro.inklauncher.ui.common.MsIcon
import com.taro.inklauncher.ui.common.SectionTitle
import com.taro.inklauncher.ui.common.SettingRow
import com.taro.inklauncher.ui.common.Stepper
import com.taro.inklauncher.ui.common.SwitchRow
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.LocalInk
import com.taro.inklauncher.widget.custom.BoxShape
import com.taro.inklauncher.widget.custom.CustomWidget
import com.taro.inklauncher.widget.custom.FontKind
import com.taro.inklauncher.widget.custom.HAlign
import com.taro.inklauncher.widget.custom.InkColor
import com.taro.inklauncher.widget.custom.Layer
import com.taro.inklauncher.widget.custom.LayerType
import com.taro.inklauncher.widget.custom.PosUnit
import com.taro.inklauncher.widget.custom.ProgressSource
import com.taro.inklauncher.widget.custom.TapAction
import com.taro.inklauncher.widget.custom.TokenResolver
import com.taro.inklauncher.widget.custom.newId
import com.taro.inklauncher.widget.custom.rememberTokenContext
import kotlinx.coroutines.delay

private fun typeIcon(t: LayerType): Int = when (t) {
    LayerType.TEXT -> R.drawable.ic_ms_text_fields
    LayerType.BOX -> R.drawable.ic_ms_crop_square
    LayerType.ICON -> R.drawable.ic_ms_star
    LayerType.DOT_CLOCK -> R.drawable.ic_ms_schedule
    LayerType.DIVIDER -> R.drawable.ic_ms_horizontal_rule
    LayerType.DOT_PROGRESS -> R.drawable.ic_ms_battery_full
}

@Composable
private fun typeLabel(t: LayerType): String = stringResource(
    when (t) {
        LayerType.TEXT -> R.string.layer_text
        LayerType.BOX -> R.string.layer_box
        LayerType.ICON -> R.string.layer_icon
        LayerType.DOT_CLOCK -> R.string.layer_dot_clock
        LayerType.DIVIDER -> R.string.layer_divider
        LayerType.DOT_PROGRESS -> R.string.layer_dot_progress
    },
)

private fun newLayer(t: LayerType, name: String): Layer = when (t) {
    LayerType.TEXT -> Layer(type = t, name = name, text = "{time}", x = 5f, y = 10f, w = 90f, h = 40f, fontSize = 32f)
    LayerType.BOX -> Layer(type = t, name = name, x = 5f, y = 10f, w = 90f, h = 80f)
    LayerType.ICON -> Layer(type = t, name = name, icon = "star", x = 40f, y = 25f, w = 20f, h = 50f, align = HAlign.CENTER)
    LayerType.DOT_CLOCK -> Layer(type = t, name = name, x = 5f, y = 10f, w = 90f, h = 60f, align = HAlign.CENTER)
    LayerType.DIVIDER -> Layer(type = t, name = name, x = 5f, y = 48f, w = 90f, h = 4f)
    LayerType.DOT_PROGRESS -> Layer(type = t, name = name, x = 10f, y = 40f, w = 80f, h = 20f, align = HAlign.CENTER)
}

/** Editor widget kustom ringan: kanvas sel grid + pratinjau langsung + daftar layer. */
@Composable
fun WidgetEditorScreen(vm: LauncherViewModel, id: String) {
    val store = vm.customWidgets
    val prefs = LocalPrefs.current
    val ink = LocalInk.current
    val tokens = rememberTokenContext(vm)
    val defaultName = stringResource(R.string.new_widget_name)
    var widget by remember(id) { mutableStateOf(store.get(id) ?: CustomWidget(id = id, name = defaultName)) }
    var selectedId by remember(id) { mutableStateOf(widget.layers.lastOrNull()?.id) }
    var addMenu by remember { mutableStateOf(false) }

    // Simpan otomatis (debounce) dan saat keluar.
    LaunchedEffect(widget) {
        delay(400)
        store.save(widget)
    }
    val latest by rememberUpdatedState(widget)
    DisposableEffect(id) {
        onDispose { store.save(latest) }
    }

    fun updateLayer(l: Layer) {
        widget = widget.copy(layers = widget.layers.map { if (it.id == l.id) l else it })
    }

    fun moveLayer(layerId: String, delta: Int) {
        val list = widget.layers.toMutableList()
        val i = list.indexOfFirst { it.id == layerId }
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        val tmp = list[i]
        list[i] = list[j]
        list[j] = tmp
        widget = widget.copy(layers = list)
    }

    val selected = widget.layers.firstOrNull { it.id == selectedId }

    InkScaffold(
        title = stringResource(R.string.editor_title),
        onBack = vm::popScreen,
        actions = {
            IconAction(R.drawable.ic_ms_check, stringResource(R.string.action_save)) {
                store.save(widget)
                vm.popScreen()
            }
        },
    ) {
        // Pratinjau langsung (tetap di atas).
        WidgetPreview(
            widget = widget,
            tokens = tokens,
            limitHeight = 220.dp,
            modifier = Modifier.padding(16.dp),
            selectedLayerId = selectedId,
            onTapLayer = { l -> if (l != null) selectedId = l.id },
        )
        HorizontalDivider(color = ink.muted, thickness = 1.dp)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            InkTextField(
                value = widget.name,
                onChange = { widget = widget.copy(name = it) },
                label = stringResource(R.string.editor_name),
            )
            Stepper(stringResource(R.string.editor_width_cells), widget.w, 1, prefs.columns) { widget = widget.copy(w = it) }
            Stepper(stringResource(R.string.editor_height_cells), widget.h, 1, prefs.rows) { widget = widget.copy(h = it) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionTitle(stringResource(R.string.editor_layers)) }
                Box {
                    TextButton(onClick = { addMenu = true }) {
                        MsIcon(R.drawable.ic_ms_add, null, size = 18.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.editor_add_layer), color = ink.fg)
                    }
                    val labels = LayerType.entries.map { it to typeLabel(it) }
                    InkMenu(
                        expanded = addMenu,
                        onDismiss = { addMenu = false },
                        actions = labels.map { (t, label) ->
                            MenuAction(label, typeIcon(t)) {
                                val l = newLayer(t, label)
                                widget = widget.copy(layers = widget.layers + l)
                                selectedId = l.id
                            }
                        },
                    )
                }
                Spacer(Modifier.width(8.dp))
            }

            widget.layers.forEachIndexed { index, layer ->
                LayerRow(
                    layer = layer,
                    label = layer.name.ifBlank { typeLabel(layer.type) },
                    selected = layer.id == selectedId,
                    canUp = index < widget.layers.size - 1,
                    canDown = index > 0,
                    onSelect = { selectedId = layer.id },
                    onUp = { moveLayer(layer.id, +1) },
                    onDown = { moveLayer(layer.id, -1) },
                    onDuplicate = {
                        val copy = layer.copy(id = newId())
                        widget = widget.copy(layers = widget.layers + copy)
                        selectedId = copy.id
                    },
                    onDelete = {
                        widget = widget.copy(layers = widget.layers.filterNot { it.id == layer.id })
                        if (selectedId == layer.id) selectedId = widget.layers.lastOrNull()?.id
                    },
                )
            }

            if (selected != null) {
                HorizontalDivider(color = ink.muted, thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))
                LayerProperties(vm, selected, ::updateLayer)
            }
            Spacer(Modifier.size(32.dp))
        }
    }
}

@Composable
private fun LayerRow(
    layer: Layer,
    label: String,
    selected: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onSelect: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val ink = LocalInk.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .border(if (selected) 2.dp else 1.dp, if (selected) ink.fg else ink.muted, RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MsIcon(typeIcon(layer.type), null, size = 20.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = ink.fg,
            fontFamily = Figtree,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 15.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (canUp) IconAction(R.drawable.ic_ms_arrow_upward, stringResource(R.string.editor_layer_up), onUp)
        if (canDown) IconAction(R.drawable.ic_ms_arrow_downward, stringResource(R.string.editor_layer_down), onDown)
        IconAction(R.drawable.ic_ms_content_copy, stringResource(R.string.action_duplicate), onDuplicate)
        IconAction(R.drawable.ic_ms_delete, stringResource(R.string.action_delete), onDelete)
    }
}

@Composable
private fun LayerProperties(vm: LauncherViewModel, l: Layer, update: (Layer) -> Unit) {
    val maxPos = if (l.unit == PosUnit.PERCENT) 100f else 400f
    val unitSuffix = if (l.unit == PosUnit.PERCENT) "%" else "dp"
    fun fmt(v: Float) = "${v.toInt()}$unitSuffix"

    InkTextField(value = l.name, onChange = { update(l.copy(name = it)) }, label = stringResource(R.string.editor_layer_name))

    SectionTitle(stringResource(R.string.editor_position))
    ChipRow {
        InkChip("%", l.unit == PosUnit.PERCENT) { update(l.copy(unit = PosUnit.PERCENT, x = l.x.coerceIn(0f, 100f), y = l.y.coerceIn(0f, 100f), w = l.w.coerceIn(1f, 100f), h = l.h.coerceIn(1f, 100f))) }
        InkChip("dp", l.unit == PosUnit.DP) { update(l.copy(unit = PosUnit.DP)) }
    }
    LabeledSlider("X", l.x, 0f..maxPos, { update(l.copy(x = it)) }, ::fmt)
    LabeledSlider("Y", l.y, 0f..maxPos, { update(l.copy(y = it)) }, ::fmt)
    LabeledSlider(stringResource(R.string.editor_w), l.w, 1f..maxPos, { update(l.copy(w = it)) }, ::fmt)
    LabeledSlider(stringResource(R.string.editor_h), l.h, 1f..maxPos, { update(l.copy(h = it)) }, ::fmt)

    if (l.type == LayerType.TEXT || l.type == LayerType.ICON || l.type == LayerType.DOT_CLOCK || l.type == LayerType.DOT_PROGRESS) {
        SectionTitle(stringResource(R.string.editor_align))
        ChipRow {
            InkChip(stringResource(R.string.align_start), l.align == HAlign.START) { update(l.copy(align = HAlign.START)) }
            InkChip(stringResource(R.string.align_center), l.align == HAlign.CENTER) { update(l.copy(align = HAlign.CENTER)) }
            InkChip(stringResource(R.string.align_end), l.align == HAlign.END) { update(l.copy(align = HAlign.END)) }
        }
    }

    SectionTitle(stringResource(R.string.editor_color))
    ChipRow {
        InkChip(stringResource(R.string.color_ink), l.color == InkColor.INK) { update(l.copy(color = InkColor.INK)) }
        InkChip(stringResource(R.string.color_paper), l.color == InkColor.PAPER) { update(l.copy(color = InkColor.PAPER)) }
        InkChip(stringResource(R.string.color_muted), l.color == InkColor.MUTED) { update(l.copy(color = InkColor.MUTED)) }
        InkChip(stringResource(R.string.color_transparent), l.color == InkColor.TRANSPARENT) { update(l.copy(color = InkColor.TRANSPARENT)) }
    }

    when (l.type) {
        LayerType.TEXT -> {
            SectionTitle(stringResource(R.string.layer_text))
            InkTextField(value = l.text, onChange = { update(l.copy(text = it)) }, label = stringResource(R.string.editor_text), singleLine = false)
            ChipRow {
                TokenResolver.TOKENS.forEach { token ->
                    InkChip(token, false) { update(l.copy(text = l.text + token)) }
                }
            }
            LabeledSlider(stringResource(R.string.editor_font_size), l.fontSize, 8f..120f, { update(l.copy(fontSize = it)) })
            ChipRow {
                InkChip("Sans", l.font == FontKind.SANS) { update(l.copy(font = FontKind.SANS)) }
                InkChip("Serif", l.font == FontKind.SERIF) { update(l.copy(font = FontKind.SERIF)) }
                InkChip("Mono", l.font == FontKind.MONO) { update(l.copy(font = FontKind.MONO)) }
            }
            SwitchRow(stringResource(R.string.editor_bold), checked = l.bold, onChange = { update(l.copy(bold = it)) })
        }
        LayerType.BOX -> {
            SectionTitle(stringResource(R.string.layer_box))
            ChipRow {
                InkChip(stringResource(R.string.shape_rect), l.shape == BoxShape.RECT) { update(l.copy(shape = BoxShape.RECT)) }
                InkChip(stringResource(R.string.shape_capsule), l.shape == BoxShape.CAPSULE) { update(l.copy(shape = BoxShape.CAPSULE)) }
                InkChip(stringResource(R.string.shape_circle), l.shape == BoxShape.CIRCLE) { update(l.copy(shape = BoxShape.CIRCLE)) }
            }
            SwitchRow(stringResource(R.string.editor_filled), checked = l.filled, onChange = { update(l.copy(filled = it)) })
            LabeledSlider(stringResource(R.string.editor_stroke), l.stroke, 0.5f..8f, { update(l.copy(stroke = it)) }, { "%.1f dp".format(it) })
            if (l.shape == BoxShape.RECT) {
                LabeledSlider(stringResource(R.string.editor_corner), l.corner, 0f..60f, { update(l.copy(corner = it)) })
            }
        }
        LayerType.ICON -> {
            SectionTitle(stringResource(R.string.layer_icon))
            val ink = LocalInk.current
            LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                items(MaterialSymbols.all.keys.toList()) { name ->
                    val sel = name == l.icon
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(44.dp)
                            .border(if (sel) 2.dp else 1.dp, if (sel) ink.fg else ink.muted, RoundedCornerShape(10.dp))
                            .clickable { update(l.copy(icon = name)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        MsIcon(MaterialSymbols.res(name), name)
                    }
                }
            }
        }
        LayerType.DOT_CLOCK -> {
            SectionTitle(stringResource(R.string.layer_dot_clock))
            SwitchRow(stringResource(R.string.editor_24h), checked = l.hour24, onChange = { update(l.copy(hour24 = it)) })
            SwitchRow(stringResource(R.string.editor_off_dots), checked = l.showOffDots, onChange = { update(l.copy(showOffDots = it)) })
        }
        LayerType.DIVIDER -> {
            SectionTitle(stringResource(R.string.layer_divider))
            LabeledSlider(stringResource(R.string.editor_stroke), l.stroke, 0.5f..8f, { update(l.copy(stroke = it)) }, { "%.1f dp".format(it) })
        }
        LayerType.DOT_PROGRESS -> {
            SectionTitle(stringResource(R.string.layer_dot_progress))
            ChipRow {
                InkChip(stringResource(R.string.progress_battery), l.source == ProgressSource.BATTERY) { update(l.copy(source = ProgressSource.BATTERY)) }
                InkChip(stringResource(R.string.progress_day), l.source == ProgressSource.DAY) { update(l.copy(source = ProgressSource.DAY)) }
                InkChip(stringResource(R.string.progress_year), l.source == ProgressSource.YEAR) { update(l.copy(source = ProgressSource.YEAR)) }
            }
            LabeledSlider(stringResource(R.string.editor_dots), l.dots.toFloat(), 3f..20f, { update(l.copy(dots = it.toInt())) })
        }
    }

    SectionTitle(stringResource(R.string.editor_action))
    ChipRow {
        TapAction.entries.forEach { a ->
            InkChip(actionLabel(a), l.action == a) { update(l.copy(action = a)) }
        }
    }
    if (l.action == TapAction.OPEN_APP) {
        var picking by remember { mutableStateOf(false) }
        val appMap by vm.appMap.collectAsStateWithLifecycle()
        val current = l.actionApp?.let { appMap[it] }
        SettingRow(
            title = stringResource(R.string.editor_pick_app),
            subtitle = current?.label ?: stringResource(R.string.editor_no_app),
            icon = R.drawable.ic_ms_apps,
            onClick = { picking = true },
        )
        if (picking) {
            AppPickerDialog(vm, onDismiss = { picking = false }) { key ->
                update(l.copy(actionApp = key))
                picking = false
            }
        }
    }
}

@Composable
private fun actionLabel(a: TapAction): String = stringResource(
    when (a) {
        TapAction.NONE -> R.string.action_none
        TapAction.OPEN_APP -> R.string.action_open_app
        TapAction.GOOGLE_SEARCH -> R.string.action_google_search
        TapAction.GOOGLE_LENS -> R.string.action_google_lens
        TapAction.CLOCK -> R.string.action_clock
        TapAction.CALENDAR -> R.string.action_calendar
    },
)

@Composable
fun AppPickerDialog(vm: LauncherViewModel, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val ink = LocalInk.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ink.fg) } },
        title = { Text(stringResource(R.string.editor_pick_app), color = ink.fg) },
        containerColor = ink.bg,
        tonalElevation = 0.dp,
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(apps, key = { it.key }) { e ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(e.key) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconImage(e, 28.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(e.label, color = ink.fg, fontFamily = Figtree)
                    }
                }
            }
        },
    )
}
