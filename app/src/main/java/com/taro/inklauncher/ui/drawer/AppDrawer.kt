package com.taro.inklauncher.ui.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.R
import com.taro.inklauncher.data.AppEntry
import com.taro.inklauncher.ui.common.AppIconImage
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.common.MsIcon
import com.taro.inklauncher.ui.home.AppMenu
import com.taro.inklauncher.ui.home.safeInsets
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.InkMotion
import com.taro.inklauncher.ui.theme.LocalInk
import kotlinx.coroutines.delay
import java.text.Normalizer

@Composable
fun AppDrawer(vm: LauncherViewModel, visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(InkMotion.MEDIUM)) { it / 5 } + fadeIn(tween(InkMotion.SHORT)),
        exit = slideOutVertically(tween(InkMotion.SHORT)) { it / 5 } + fadeOut(tween(InkMotion.SHORT)),
    ) {
        DrawerContent(vm)
    }
}

private fun normalize(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

@Composable
private fun DrawerContent(vm: LauncherViewModel) {
    val ink = LocalInk.current
    val prefs = LocalPrefs.current
    val apps by vm.drawerApps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val closeThreshold = with(density) { 96.dp.toPx() }

    val filtered = remember(apps, query) {
        val q = normalize(query.trim())
        if (q.isEmpty()) {
            apps
        } else {
            apps.filter { normalize(it.label).contains(q) }
                .sortedBy { if (normalize(it.label).startsWith(q)) 0 else 1 }
        }
    }

    LaunchedEffect(Unit) {
        if (prefs.drawerAutoKeyboard) {
            delay(InkMotion.MEDIUM.toLong())
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
    }

    val close: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
        vm.closeDrawer()
    }

    // Tarik ke bawah saat daftar di posisi paling atas = tutup laci.
    val closeConnection = remember {
        object : NestedScrollConnection {
            var pull = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    pull += available.y
                    if (pull > closeThreshold) {
                        pull = 0f
                        close()
                    }
                } else if (consumed.y != 0f) {
                    pull = 0f
                }
                return Offset.Zero
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ink.bg)
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(safeInsets())
            .imePadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > closeThreshold / 2) close() },
                        onVerticalDrag = { change, amount ->
                            total += amount
                            change.consume()
                        },
                    )
                }
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.5.dp, ink.fg, CircleShape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MsIcon(R.drawable.ic_ms_search, null, size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.drawer_search_hint),
                            color = ink.muted,
                            fontFamily = Figtree,
                            fontSize = 17.sp,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = ink.fg, fontFamily = Figtree, fontSize = 17.sp),
                        cursorBrush = SolidColor(ink.fg),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { filtered.firstOrNull()?.let { vm.launch(it) } }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                }
                if (query.isNotEmpty()) {
                    Box(
                        Modifier
                            .padding(start = 8.dp)
                            .pointerInput(Unit) { detectTapGestures { query = "" } },
                    ) {
                        MsIcon(R.drawable.ic_ms_close, stringResource(R.string.action_clear), size = 20.dp)
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(prefs.columns),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(closeConnection),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 24.dp),
        ) {
            items(filtered, key = { it.key }) { entry ->
                DrawerIcon(vm, entry, showLabel = query.isNotBlank())
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerIcon(vm: LauncherViewModel, entry: AppEntry, showLabel: Boolean) {
    val ink = LocalInk.current
    val prefs = LocalPrefs.current
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { vm.launch(entry) }, onLongClick = { menu = true })
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIconImage(entry, prefs.iconSizeDp.dp)
            AppMenu(vm, entry, source = null, inDrawer = true, expanded = menu, onDismiss = { menu = false })
        }
        if (showLabel) {
            Text(
                text = entry.label,
                color = ink.fg,
                fontFamily = Figtree,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}
