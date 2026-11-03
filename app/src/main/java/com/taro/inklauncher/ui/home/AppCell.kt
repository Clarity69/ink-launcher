package com.taro.inklauncher.ui.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.MoveSource
import com.taro.inklauncher.R
import com.taro.inklauncher.data.AppEntry
import com.taro.inklauncher.system.LaunchHelper
import com.taro.inklauncher.ui.common.AppIconImage
import com.taro.inklauncher.ui.common.InkMenu
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.common.MenuAction
import kotlin.math.roundToInt

/** Ikon aplikasi di layar utama/dock: tap = buka, tahan = menu, tahan+geser = pindahkan. */
@Composable
fun AppCell(
    vm: LauncherViewModel,
    entry: AppEntry,
    source: MoveSource,
    drag: DragController,
    modifier: Modifier = Modifier,
) {
    val prefs = LocalPrefs.current
    var menuOpen by remember { mutableStateOf(false) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val suppressTap = remember { booleanArrayOf(false) }
    val slop = with(LocalDensity.current) { 18.dp.toPx() }
    val currentEntry by rememberUpdatedState(entry)
    val isDragged = drag.payload?.source == source

    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { coords = it }
            .pointerInput(source) {
                detectTapGestures(
                    onPress = { suppressTap[0] = false },
                    onTap = {
                        if (!suppressTap[0]) {
                            val b = coords?.boundsInRoot()
                            vm.launch(
                                currentEntry,
                                b?.let {
                                    android.graphics.Rect(
                                        it.left.roundToInt(), it.top.roundToInt(),
                                        it.right.roundToInt(), it.bottom.roundToInt(),
                                    )
                                },
                            )
                        }
                    },
                )
            }
            .pointerInput(source) {
                var total = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { off ->
                        suppressTap[0] = true
                        total = Offset.Zero
                        menuOpen = true
                        coords?.let { drag.pointer = it.localToRoot(off) }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        total += amount
                        coords?.let { drag.pointer = it.localToRoot(change.position) }
                        if (drag.payload == null && total.getDistance() > slop) {
                            menuOpen = false
                            drag.start(DragPayload(source, currentEntry))
                        }
                    },
                    onDragEnd = {
                        if (drag.payload != null) {
                            drag.target()?.let { vm.drop(source, it) }
                            drag.end()
                        }
                    },
                    onDragCancel = { drag.end() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AppIconImage(
            entry = entry,
            size = prefs.iconSizeDp.dp,
            modifier = Modifier.graphicsLayer { alpha = if (isDragged) 0f else 1f },
        )
        AppMenu(vm, entry, source, inDrawer = false, expanded = menuOpen, onDismiss = { menuOpen = false })
    }
}

/** Menu long-press ikon aplikasi (layar utama, dock, dan laci). */
@Composable
fun AppMenu(
    vm: LauncherViewModel,
    entry: AppEntry,
    source: MoveSource?,
    inDrawer: Boolean,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shortcuts = remember(expanded, entry.key) {
        if (expanded) vm.repo.shortcuts(entry) else emptyList()
    }
    val sAppInfo = stringResource(R.string.menu_app_info)
    val sUninstall = stringResource(R.string.menu_uninstall)
    val sRemove = stringResource(R.string.menu_remove_home)
    val sMove = stringResource(R.string.menu_move)
    val sAddHome = stringResource(R.string.menu_add_home)
    val sHide = stringResource(R.string.menu_hide)

    val actions = buildList {
        shortcuts.forEach { sc ->
            val label = (sc.shortLabel ?: sc.longLabel)?.toString().orEmpty()
            if (label.isNotEmpty()) add(MenuAction(label, R.drawable.ic_ms_touch_app) { vm.repo.startShortcut(sc) })
        }
        if (inDrawer) {
            add(MenuAction(sAddHome, R.drawable.ic_ms_add) { vm.addToHome(entry.key) })
            add(MenuAction(sHide, R.drawable.ic_ms_visibility_off) { vm.setHidden(entry, true) })
        }
        add(MenuAction(sAppInfo, R.drawable.ic_ms_info) { vm.repo.openAppInfo(entry) })
        if (!entry.isSystem) {
            add(MenuAction(sUninstall, R.drawable.ic_ms_delete) { LaunchHelper.uninstall(context, entry.packageName) })
        }
        if (!inDrawer && source != null) {
            add(
                MenuAction(sRemove, R.drawable.ic_ms_close) {
                    when (source) {
                        is MoveSource.Grid -> vm.removeItem(source.itemId)
                        is MoveSource.Dock -> vm.removeFromDock(source.index)
                    }
                },
            )
            add(MenuAction(sMove, R.drawable.ic_ms_open_with) { vm.startMove(source) })
        }
    }
    InkMenu(expanded = expanded, onDismiss = onDismiss, actions = actions)
}
