package com.taro.inklauncher.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.DropTarget
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.MoveSource
import com.taro.inklauncher.R
import com.taro.inklauncher.Screen
import com.taro.inklauncher.data.AppEntry
import com.taro.inklauncher.data.HomeLayout
import com.taro.inklauncher.data.ItemType
import com.taro.inklauncher.system.LaunchHelper
import com.taro.inklauncher.system.StatusBarHelper
import com.taro.inklauncher.ui.common.AppIconImage
import com.taro.inklauncher.ui.common.InkMenu
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.common.MenuAction
import com.taro.inklauncher.ui.common.MsIcon
import com.taro.inklauncher.ui.glance.AtAGlance
import com.taro.inklauncher.ui.search.PixelSearchBar
import com.taro.inklauncher.ui.theme.InkMotion
import com.taro.inklauncher.ui.theme.LocalInk
import kotlin.math.roundToInt

/** Area aman: status bar, navigation bar (tombol), dan cutout. */
@Composable
fun safeInsets(): WindowInsets = WindowInsets.systemBars
    .union(WindowInsets.displayCutout)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val prefs = LocalPrefs.current
    val ink = LocalInk.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val layout by vm.layout.collectAsStateWithLifecycle()
    val appMap by vm.appMap.collectAsStateWithLifecycle()
    val moving by vm.moving.collectAsStateWithLifecycle()
    val homeMenu by vm.homeMenu.collectAsStateWithLifecycle()
    val resizing by vm.resizing.collectAsStateWithLifecycle()
    val pageCount = prefs.pageCount
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val drag = remember { DragController() }
    val pageOrigin = remember { HashMap<Int, Offset>() }
    drag.cols = prefs.columns
    drag.rows = prefs.rows
    drag.currentPage = pagerState.currentPage

    LaunchedEffect(Unit) {
        vm.homeReset.collect {
            if (pagerState.currentPage != 0) {
                pagerState.animateScrollToPage(0, animationSpec = tween(InkMotion.MEDIUM))
            }
        }
    }

    val swipeThreshold = with(density) { 56.dp.toPx() }
    val onSwipeUp by rememberUpdatedState { if (prefs.gestureSwipeUp) vm.openDrawer() }
    val onSwipeDown by rememberUpdatedState { if (prefs.gestureSwipeDown) StatusBarHelper.expandNotifications(context) }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { drag.rootOrigin = it.positionInRoot() },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(safeInsets())
                // Gestur launcher hanya di area tengah (sudah dikurangi inset gestur sistem).
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total < -swipeThreshold) onSwipeUp() else if (total > swipeThreshold) onSwipeDown()
                            total = 0f
                        },
                        onDragCancel = { total = 0f },
                        onVerticalDrag = { change, amount ->
                            total += amount
                            change.consume()
                        },
                    )
                },
        ) {
            Column(Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    userScrollEnabled = pageCount > 1 && drag.payload == null && moving == null && resizing == null,
                    key = { it },
                ) { page ->
                    Box(Modifier.fillMaxSize()) {
                        // Lapisan latar: tahan area kosong = menu.
                        Box(
                            Modifier
                                .matchParentSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { off ->
                                            vm.homeMenu.value = off + (pageOrigin[page] ?: Offset.Zero)
                                        },
                                    )
                                }
                                .onGloballyPositioned { pageOrigin[page] = it.positionInRoot() },
                        )
                        Column(Modifier.fillMaxSize()) {
                            if (page == 0 && prefs.showGlance) {
                                AtAGlance(vm, Modifier.fillMaxWidth())
                            }
                            HomeGrid(
                                vm = vm,
                                page = page,
                                layout = layout,
                                appMap = appMap,
                                drag = drag,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                }
                if (pageCount > 1) {
                    PageDots(current = pagerState.currentPage, count = pageCount)
                }
                Dock(vm = vm, layout = layout, appMap = appMap, drag = drag)
                if (prefs.showSearchBar) {
                    PixelSearchBar(Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp))
                }
            }
        }

        // Ikon yang sedang di-drag.
        drag.payload?.let { p ->
            val sizePx = with(density) { prefs.iconSizeDp.dp.toPx() }
            Box(
                Modifier.offset {
                    IntOffset(
                        (drag.pointer.x - drag.rootOrigin.x - sizePx / 2).roundToInt(),
                        (drag.pointer.y - drag.rootOrigin.y - sizePx / 2).roundToInt(),
                    )
                },
            ) {
                AppIconImage(
                    entry = p.entry,
                    size = prefs.iconSizeDp.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                    },
                )
            }
        }

        if (moving != null || resizing != null) {
            MoveHint(
                text = stringResource(if (moving != null) R.string.move_hint else R.string.resize_hint),
                button = stringResource(if (moving != null) R.string.action_cancel else R.string.action_done),
                onClick = {
                    vm.moving.value = null
                    vm.resizing.value = null
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(top = 8.dp),
            )
        }

        homeMenu?.let { pos ->
            Box(
                Modifier.offset {
                    IntOffset(
                        (pos.x - drag.rootOrigin.x).roundToInt(),
                        (pos.y - drag.rootOrigin.y).roundToInt(),
                    )
                },
            ) {
                InkMenu(
                    expanded = true,
                    onDismiss = { vm.homeMenu.value = null },
                    actions = listOf(
                        MenuAction(stringResource(R.string.menu_wallpaper), R.drawable.ic_ms_wallpaper) {
                            LaunchHelper.wallpaper(context)
                        },
                        MenuAction(stringResource(R.string.menu_widgets), R.drawable.ic_ms_widgets) {
                            vm.openScreen(Screen.WidgetPicker)
                        },
                        MenuAction(stringResource(R.string.menu_custom_widgets), R.drawable.ic_ms_dashboard_customize) {
                            vm.openScreen(Screen.CustomWidgets)
                        },
                        MenuAction(stringResource(R.string.menu_launcher_settings), R.drawable.ic_ms_settings) {
                            vm.openScreen(Screen.Settings())
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun MoveHint(text: String, button: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    Row(
        modifier
            .background(ink.bg, RoundedCornerShape(50))
            .border(1.dp, ink.fg, RoundedCornerShape(50))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = ink.fg)
        TextButton(onClick = onClick) { Text(button, color = ink.fg) }
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    val ink = LocalInk.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(6.dp)
                    .then(
                        if (i == current) {
                            Modifier.background(ink.fg, CircleShape)
                        } else {
                            Modifier.border(1.dp, ink.fg, CircleShape)
                        },
                    ),
            )
        }
    }
}

@Composable
fun HomeGrid(
    vm: LauncherViewModel,
    page: Int,
    layout: HomeLayout,
    appMap: Map<String, AppEntry>,
    drag: DragController,
    modifier: Modifier = Modifier,
) {
    val prefs = LocalPrefs.current
    val ink = LocalInk.current
    val moving by vm.moving.collectAsStateWithLifecycle()
    val resizing by vm.resizing.collectAsStateWithLifecycle()
    val cols = prefs.columns
    val rows = prefs.rows
    BoxWithConstraints(
        modifier.onGloballyPositioned { drag.gridBounds[page] = it.boundsInRoot() },
    ) {
        val cellW = maxWidth / cols
        val cellH = maxHeight / rows
        val pageItems = layout.items.filter { it.page == page }
        for (item in pageItems) {
            androidx.compose.runtime.key(item.id) {
                Box(
                    Modifier
                        .offset(x = cellW * item.x, y = cellH * item.y)
                        .size(width = cellW * item.w, height = cellH * item.h),
                ) {
                    when (item.type) {
                        ItemType.APP -> {
                            val entry = item.appKey?.let { appMap[it] }
                            if (entry != null) {
                                AppCell(vm, entry, MoveSource.Grid(item.id), drag)
                            }
                        }
                        ItemType.WIDGET, ItemType.CUSTOM -> WidgetSlot(vm, item, cellW, cellH)
                    }
                }
            }
        }

        // Mode pindah: setiap sel kosong bisa diketuk sebagai tujuan.
        if (moving != null) {
            for (y in 0 until rows) for (x in 0 until cols) {
                Box(
                    Modifier
                        .offset(x = cellW * x, y = cellH * y)
                        .size(cellW, cellH)
                        .padding(4.dp)
                        .border(1.dp, ink.muted, RoundedCornerShape(12.dp))
                        .clickable { vm.dropMove(DropTarget.Cell(page, x, y)) },
                )
            }
        }

        resizing?.let { id ->
            pageItems.firstOrNull { it.id == id }?.let { item ->
                ResizeFrame(vm, item, cellW, cellH, cols, rows)
            }
        }
    }
}

@Composable
fun Dock(
    vm: LauncherViewModel,
    layout: HomeLayout,
    appMap: Map<String, AppEntry>,
    drag: DragController,
) {
    val prefs = LocalPrefs.current
    val ink = LocalInk.current
    val moving by vm.moving.collectAsStateWithLifecycle()
    val count = prefs.dockCount
    val dock = layout.dockSafe()
    drag.dockCount = count
    val drawerLabel = stringResource(R.string.cd_app_drawer)
    Row(
        Modifier
            .fillMaxWidth()
            .height((prefs.iconSizeDp + 28).dp),
    ) {
        Row(
            Modifier
                .weight(count.toFloat())
                .fillMaxHeight()
                .onGloballyPositioned { drag.dockBounds = it.boundsInRoot() },
        ) {
            for (i in 0 until count) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    val entry = dock.getOrNull(i)?.let { appMap[it] }
                    if (entry != null) {
                        AppCell(vm, entry, MoveSource.Dock(i), drag)
                    }
                    if (moving != null) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .padding(4.dp)
                                .border(1.dp, ink.muted, RoundedCornerShape(12.dp))
                                .clickable { vm.dropMove(DropTarget.Dock(i)) },
                        )
                    }
                }
            }
        }
        // Tombol laci aplikasi (alternatif dari swipe ke atas).
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(prefs.iconSizeDp.dp)
                    .clip(CircleShape)
                    .clickable { vm.openDrawer() },
                contentAlignment = Alignment.Center,
            ) {
                MsIcon(R.drawable.ic_ms_apps, drawerLabel, size = (prefs.iconSizeDp * 0.6f).dp)
            }
        }
    }
}
