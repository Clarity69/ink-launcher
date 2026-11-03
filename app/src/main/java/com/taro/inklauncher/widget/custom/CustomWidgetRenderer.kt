package com.taro.inklauncher.widget.custom

import android.app.AlarmManager
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.R
import com.taro.inklauncher.data.HomeItem
import com.taro.inklauncher.data.MaterialSymbols
import com.taro.inklauncher.system.LaunchHelper
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.InkColors
import com.taro.inklauncher.ui.theme.InkSerif
import com.taro.inklauncher.ui.theme.LocalInk
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

fun InkColor.resolve(ink: InkColors): Color = when (this) {
    InkColor.INK -> ink.fg
    InkColor.PAPER -> ink.bg
    InkColor.MUTED -> ink.muted
    InkColor.TRANSPARENT -> Color.Transparent
}

private fun nextAlarm(context: Context, locale: Locale): String {
    val t = runCatching {
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime
    }.getOrNull() ?: return ""
    val p = DateFormat.getBestDateTimePattern(locale, "EEEHHmm")
    return SimpleDateFormat(p, locale).format(Date(t))
}

@Composable
fun rememberTokenContext(vm: LauncherViewModel): TokenContext {
    val now by vm.now.collectAsStateWithLifecycle()
    val battery by vm.battery.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val chargingLabel = stringResource(R.string.token_charging)
    return remember(now, battery, locale, chargingLabel) {
        TokenContext(now, battery.level, battery.charging, nextAlarm(context, locale), locale, chargingLabel)
    }
}

/** Menjalankan aksi tap sebuah layer. */
fun runLayerAction(vm: LauncherViewModel, context: Context, layer: Layer) {
    when (layer.action) {
        TapAction.NONE -> Unit
        TapAction.OPEN_APP -> {
            val key = layer.actionApp ?: return
            val entry = vm.appMap.value[key]
            if (entry != null) {
                vm.launch(entry)
            } else {
                LaunchHelper.openPackage(context, key.substringBefore('/'))
            }
        }
        TapAction.GOOGLE_SEARCH -> LaunchHelper.googleSearch(context)
        TapAction.GOOGLE_LENS -> LaunchHelper.lens(context)
        TapAction.CLOCK -> LaunchHelper.clock(context)
        TapAction.CALENDAR -> LaunchHelper.calendar(context)
    }
}

private data class LayerBox(val x: Dp, val y: Dp, val w: Dp, val h: Dp)

private fun layerBox(layer: Layer, width: Dp, height: Dp): LayerBox =
    if (layer.unit == PosUnit.PERCENT) {
        LayerBox(width * (layer.x / 100f), height * (layer.y / 100f), width * (layer.w / 100f), height * (layer.h / 100f))
    } else {
        LayerBox(layer.x.dp, layer.y.dp, layer.w.dp, layer.h.dp)
    }

/**
 * Merender widget kustom langsung dengan Compose (bukan AppWidget).
 * [onLayerTap] dipanggil untuk layer teratas yang memiliki aksi di titik sentuh.
 */
@Composable
fun CustomWidgetView(
    widget: CustomWidget,
    tokens: TokenContext,
    modifier: Modifier = Modifier,
    selectedLayerId: String? = null,
    onLayerTap: ((Layer) -> Unit)? = null,
    onTapAny: ((Layer?) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) {
    val ink = LocalInk.current
    val density = LocalDensity.current
    val currentWidget by rememberUpdatedState(widget)
    val currentOnLayerTap by rememberUpdatedState(onLayerTap)
    val currentOnTapAny by rememberUpdatedState(onTapAny)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    BoxWithConstraints(
        modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { off ->
                    val wDp = with(density) { size.width.toDp() }
                    val hDp = with(density) { size.height.toDp() }
                    val hit = currentWidget.layers.lastOrNull { l ->
                        val b = layerBox(l, wDp, hDp)
                        val r = Rect(
                            with(density) { b.x.toPx() }, with(density) { b.y.toPx() },
                            with(density) { (b.x + b.w).toPx() }, with(density) { (b.y + b.h).toPx() },
                        )
                        r.contains(off) && (currentOnTapAny != null || l.action != TapAction.NONE)
                    }
                    currentOnTapAny?.invoke(hit)
                    if (hit != null) currentOnLayerTap?.invoke(hit)
                },
                onLongPress = { currentOnLongPress?.invoke() },
            )
        },
    ) {
        val width = maxWidth
        val height = maxHeight
        for (layer in widget.layers) {
            val b = layerBox(layer, width, height)
            Box(
                Modifier
                    .offset(b.x, b.y)
                    .size(b.w.coerceAtLeast(1.dp), b.h.coerceAtLeast(1.dp))
                    .then(
                        if (layer.id == selectedLayerId) Modifier.border(1.dp, ink.muted, RoundedCornerShape(2.dp)) else Modifier,
                    ),
            ) {
                LayerContent(layer, tokens, ink)
            }
        }
    }
}

private fun alignmentOf(a: HAlign): Alignment = when (a) {
    HAlign.START -> Alignment.CenterStart
    HAlign.CENTER -> Alignment.Center
    HAlign.END -> Alignment.CenterEnd
}

@Composable
private fun LayerContent(layer: Layer, tokens: TokenContext, ink: InkColors) {
    val color = layer.color.resolve(ink)
    when (layer.type) {
        LayerType.TEXT -> {
            Box(Modifier.fillMaxSize(), contentAlignment = alignmentOf(layer.align)) {
                Text(
                    text = TokenResolver.resolve(layer.text, tokens),
                    color = color,
                    fontSize = layer.fontSize.sp,
                    lineHeight = (layer.fontSize * 1.1f).sp,
                    fontFamily = when (layer.font) {
                        FontKind.SANS -> Figtree
                        FontKind.SERIF -> InkSerif
                        FontKind.MONO -> FontFamily.Monospace
                    },
                    fontWeight = if (layer.bold) FontWeight.Medium else FontWeight.Normal,
                    textAlign = when (layer.align) {
                        HAlign.START -> TextAlign.Start
                        HAlign.CENTER -> TextAlign.Center
                        HAlign.END -> TextAlign.End
                    },
                )
            }
        }
        LayerType.BOX -> {
            val density = LocalDensity.current
            val strokePx = with(density) { layer.stroke.dp.toPx() }
            val cornerPx = with(density) { layer.corner.dp.toPx() }
            Canvas(Modifier.fillMaxSize()) {
                val inset = if (layer.filled) 0f else strokePx / 2f
                val topLeft = Offset(inset, inset)
                val sz = Size((size.width - inset * 2).coerceAtLeast(0f), (size.height - inset * 2).coerceAtLeast(0f))
                val style = if (layer.filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = strokePx)
                when (layer.shape) {
                    BoxShape.RECT -> drawRoundRect(color, topLeft, sz, CornerRadius(cornerPx, cornerPx), style = style)
                    BoxShape.CAPSULE -> {
                        val r = min(sz.width, sz.height) / 2f
                        drawRoundRect(color, topLeft, sz, CornerRadius(r, r), style = style)
                    }
                    BoxShape.CIRCLE -> drawOval(color, topLeft, sz, style = style)
                }
            }
        }
        LayerType.ICON -> {
            Box(Modifier.fillMaxSize(), contentAlignment = alignmentOf(layer.align)) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = alignmentOf(layer.align)) {
                    val s = min(maxWidth.value, maxHeight.value).dp
                    Icon(
                        painter = painterResource(MaterialSymbols.res(layer.icon)),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(s),
                    )
                }
            }
        }
        LayerType.DOT_CLOCK -> {
            val text = TokenResolver.clockText(tokens, layer.hour24)
            val off = InkColor.MUTED.resolve(ink)
            Canvas(Modifier.fillMaxSize()) {
                drawDotText(text, color, if (layer.showOffDots) off else null, layer.align)
            }
        }
        LayerType.DIVIDER -> {
            val density = LocalDensity.current
            val strokePx = with(density) { layer.stroke.dp.toPx() }
            Canvas(Modifier.fillMaxSize()) {
                if (size.width >= size.height) {
                    drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokePx)
                } else {
                    drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokePx)
                }
            }
        }
        LayerType.DOT_PROGRESS -> {
            val value = progressValue(layer.source, tokens)
            val density = LocalDensity.current
            val ringPx = with(density) { 1.dp.toPx() }
            Canvas(Modifier.fillMaxSize()) {
                val n = layer.dots.coerceIn(2, 40)
                val filled = (value * n).roundToInt().coerceIn(0, n)
                val slot = size.width / n
                val d = min(size.height, slot * 0.72f)
                val r = d / 2f
                val total = slot * n
                val startX = when (layer.align) {
                    HAlign.START -> 0f
                    HAlign.CENTER -> (size.width - total) / 2f
                    HAlign.END -> size.width - total
                }
                for (i in 0 until n) {
                    val c = Offset(startX + slot * i + slot / 2f, size.height / 2f)
                    if (i < filled) {
                        drawCircle(color, r, c)
                    } else {
                        drawCircle(color, r - ringPx / 2f, c, style = Stroke(ringPx))
                    }
                }
            }
        }
    }
}

private fun progressValue(source: ProgressSource, tokens: TokenContext): Float {
    val cal = Calendar.getInstance().apply { timeInMillis = tokens.now }
    return when (source) {
        ProgressSource.BATTERY -> if (tokens.batteryLevel >= 0) tokens.batteryLevel / 100f else 0f
        ProgressSource.DAY -> (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)) / 1440f
        ProgressSource.YEAR -> cal.get(Calendar.DAY_OF_YEAR) / cal.getActualMaximum(Calendar.DAY_OF_YEAR).toFloat()
    }
}

/** Menggambar teks dengan font titik 5×7 (gaya dot-matrix). */
private fun DrawScope.drawDotText(text: String, on: Color, off: Color?, align: HAlign) {
    val cols = DotMatrixFont.columns(text).coerceAtLeast(1)
    val pitch = min(size.width / cols, size.height / DotMatrixFont.ROWS)
    val r = pitch * 0.38f
    val totalW = pitch * cols
    val startX = when (align) {
        HAlign.START -> 0f
        HAlign.CENTER -> (size.width - totalW) / 2f
        HAlign.END -> size.width - totalW
    }
    val startY = (size.height - pitch * DotMatrixFont.ROWS) / 2f
    var col = 0
    text.forEachIndexed { idx, ch ->
        val g = DotMatrixFont.glyph(ch)
        val gw = g[0].length
        for (row in 0 until DotMatrixFont.ROWS) {
            val line = g[row]
            for (cx in 0 until gw) {
                val center = Offset(startX + (col + cx) * pitch + pitch / 2f, startY + row * pitch + pitch / 2f)
                if (line[cx] == '1') {
                    drawCircle(on, r, center)
                } else if (off != null) {
                    drawCircle(off, r * 0.5f, center)
                }
            }
        }
        col += gw
        if (idx < text.length - 1) col += 1
    }
}

/** Widget kustom yang ditempatkan di layar utama. */
@Composable
fun CustomWidgetOnHome(vm: LauncherViewModel, item: HomeItem, onLongPress: () -> Unit) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val widgets by vm.customWidgets.widgets.collectAsStateWithLifecycle()
    val widget = remember(widgets, item.customId) { widgets.firstOrNull { it.id == item.customId } }
    val tokens = rememberTokenContext(vm)
    if (widget == null) {
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, ink.muted, RoundedCornerShape(16.dp))
                .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.widget_unavailable), color = ink.muted)
        }
        return
    }
    CustomWidgetView(
        widget = widget,
        tokens = tokens,
        modifier = Modifier.fillMaxSize(),
        onLayerTap = { layer -> runLayerAction(vm, context, layer) },
        onLongPress = onLongPress,
    )
}
