package com.taro.inklauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.MoveSource
import com.taro.inklauncher.R
import com.taro.inklauncher.Screen
import com.taro.inklauncher.data.HomeItem
import com.taro.inklauncher.data.ItemType
import com.taro.inklauncher.ui.common.InkMenu
import com.taro.inklauncher.ui.common.MenuAction
import com.taro.inklauncher.ui.theme.LocalInk
import com.taro.inklauncher.widget.custom.CustomWidgetOnHome
import kotlin.math.roundToInt

/** Widget sistem & kustom di grid. */
@Composable
fun WidgetSlot(vm: LauncherViewModel, item: HomeItem, cellW: Dp, cellH: Dp) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().padding(4.dp)) {
        when (item.type) {
            ItemType.WIDGET -> SystemWidget(vm, item, cellW, cellH, onLongPress = { menu = true })
            ItemType.CUSTOM -> CustomWidgetOnHome(vm, item, onLongPress = { menu = true })
            ItemType.APP -> Unit
        }
        WidgetMenu(vm, item, menu) { menu = false }
    }
}

@Composable
private fun WidgetMenu(vm: LauncherViewModel, item: HomeItem, expanded: Boolean, onDismiss: () -> Unit) {
    val actions = buildList {
        add(MenuAction(stringResource(R.string.menu_move), R.drawable.ic_ms_open_with) { vm.startMove(MoveSource.Grid(item.id)) })
        add(MenuAction(stringResource(R.string.menu_resize), R.drawable.ic_ms_crop_square) { vm.startResize(item.id) })
        if (item.type == ItemType.CUSTOM && item.customId != null) {
            val cid = item.customId
            add(MenuAction(stringResource(R.string.action_edit), R.drawable.ic_ms_edit) { vm.openScreen(Screen.WidgetEditor(cid)) })
        }
        add(MenuAction(stringResource(R.string.menu_remove_widget), R.drawable.ic_ms_delete) { vm.removeItem(item.id) })
    }
    InkMenu(expanded = expanded, onDismiss = onDismiss, actions = actions)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SystemWidget(
    vm: LauncherViewModel,
    item: HomeItem,
    cellW: Dp,
    cellH: Dp,
    onLongPress: () -> Unit,
) {
    val ink = LocalInk.current
    val host = vm.widgetHost
    val available = remember(item.appWidgetId) { host.info(item.appWidgetId) != null }
    val wDp = (cellW * item.w).value.roundToInt()
    val hDp = (cellH * item.h).value.roundToInt()
    LaunchedEffect(item.appWidgetId, wDp, hDp) {
        host.updateSize(item.appWidgetId, wDp, hDp)
    }
    if (!available) {
        Box(
            Modifier
                .fillMaxSize()
                .border(1.dp, ink.muted, RoundedCornerShape(16.dp))
                .combinedClickable(onClick = {}, onLongClick = onLongPress),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.widget_unavailable), color = ink.muted, textAlign = TextAlign.Center)
        }
        return
    }
    AndroidView(
        factory = { ctx ->
            host.getView(ctx, item.appWidgetId) ?: android.widget.FrameLayout(ctx)
        },
        update = { v ->
            (v as? com.taro.inklauncher.widget.InkWidgetHostView)?.onLongPress = onLongPress
        },
        modifier = Modifier.fillMaxSize(),
    )
}

/** Bingkai ubah ukuran dengan pegangan; ukuran di-snap ke grid saat dilepas. */
@Composable
fun ResizeFrame(vm: LauncherViewModel, item: HomeItem, cellW: Dp, cellH: Dp, cols: Int, rows: Int) {
    val ink = LocalInk.current
    val density = LocalDensity.current
    val cellWpx = with(density) { cellW.toPx() }
    val cellHpx = with(density) { cellH.toPx() }
    var dw by remember(item.id) { mutableFloatStateOf(0f) }
    var dh by remember(item.id) { mutableFloatStateOf(0f) }

    val maxW = (cols - item.x) * cellWpx
    val maxH = (rows - item.y) * cellHpx
    val curW = (item.w * cellWpx + dw).coerceIn(cellWpx, maxW)
    val curH = (item.h * cellHpx + dh).coerceIn(cellHpx, maxH)

    fun commit() {
        val nw = (curW / cellWpx).roundToInt().coerceIn(1, cols - item.x)
        val nh = (curH / cellHpx).roundToInt().coerceIn(1, rows - item.y)
        dw = 0f
        dh = 0f
        vm.resizeItem(item.id, nw, nh)
    }

    Box(
        Modifier
            .offset(x = cellW * item.x, y = cellH * item.y)
            .size(with(density) { curW.toDp() }, with(density) { curH.toDp() })
            .border(2.dp, ink.fg, RoundedCornerShape(12.dp)),
    ) {
        ResizeHandle(Modifier.align(Alignment.CenterEnd), onDrag = { dx, _ -> dw += dx }, onEnd = ::commit)
        ResizeHandle(Modifier.align(Alignment.BottomCenter), onDrag = { _, dy -> dh += dy }, onEnd = ::commit)
        ResizeHandle(
            Modifier.align(Alignment.BottomEnd),
            onDrag = { dx, dy ->
                dw += dx
                dh += dy
            },
            onEnd = ::commit,
        )
    }
}

@Composable
private fun ResizeHandle(modifier: Modifier, onDrag: (Float, Float) -> Unit, onEnd: () -> Unit) {
    val ink = LocalInk.current
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnEnd by rememberUpdatedState(onEnd)
    Box(
        modifier
            .size(32.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { currentOnEnd() },
                    onDragCancel = { currentOnEnd() },
                ) { change, amount ->
                    change.consume()
                    currentOnDrag(amount.x, amount.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(ink.bg, CircleShape)
                .border(2.dp, ink.fg, CircleShape),
        )
    }
}
