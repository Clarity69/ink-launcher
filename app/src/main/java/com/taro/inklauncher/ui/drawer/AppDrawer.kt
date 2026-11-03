package com.taro.inklauncher.ui.drawer

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
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
import com.taro.inklauncher.ui.theme.LocalInk
import java.text.Normalizer

/**
 * Laci aplikasi. Selalu tersusun dan digeser lewat graphicsLayer sesuai [DrawerController.progress],
 * sehingga mengikuti jari tanpa recompose per frame dan langsung siap saat dibuka.
 */
@Composable
fun AppDrawer(vm: LauncherViewModel, drawer: DrawerController) {
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { drawer.heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .graphicsLayer {
                val p = drawer.progress.value
                translationY = (1f - p) * size.height
                alpha = if (p <= 0.001f) 0f else 1f
            },
    ) {
        DrawerContent(vm, drawer)
    }
}

private val diacritics = Regex("\\p{Mn}+")

private fun normalize(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(diacritics, "")

@Composable
private fun DrawerContent(vm: LauncherViewModel, drawer: DrawerController) {
    val ink = LocalInk.current
    val prefs = LocalPrefs.current
    val apps by vm.drawerApps.collectAsStateWithLifecycle()
    val open by vm.drawerOpen.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Label yang dinormalisasi dihitung sekali per daftar aplikasi.
    val normalized = remember(apps) { apps.map { it to normalize(it.label) } }
    val filtered = remember(normalized, query) {
        val q = normalize(query.trim())
        if (q.isEmpty()) {
            apps
        } else {
            normalized.filter { it.second.contains(q) }
                .sortedBy { if (it.second.startsWith(q)) 0 else 1 }
                .map { it.first }
        }
    }

    // Keyboard hanya muncul saat kolom cari diketuk. Saat laci ditutup:
    // sembunyikan keyboard, lepas fokus, dan kosongkan teks pencarian.
    LaunchedEffect(open) {
        if (!open) {
            focusManager.clearFocus()
            keyboard?.hide()
            query = ""
        }
    }

    // Grid: tarik ke bawah di posisi paling atas = laci ikut turun; lepas = menempel.
    val connection = remember(drawer) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Laci sedang setengah tertutup dan jari naik: buka lagi dulu sebelum grid menggulir.
                if (available.y < 0f && drawer.progress.value < 1f) {
                    drawer.dragBy(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f) {
                    drawer.dragBy(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (drawer.progress.value < 1f) {
                    drawer.settle(available.y)
                    return available
                }
                return Velocity.Zero
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
                .pointerInput(drawer) {
                    val tracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = { drawer.settle(tracker.calculateVelocity().y) },
                        onDragCancel = { drawer.settle(0f) },
                        onVerticalDrag = { change, amount ->
                            tracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                            drawer.dragBy(amount)
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

        val showLabels = query.isNotBlank()
        LazyVerticalGrid(
            columns = GridCells.Fixed(prefs.columns),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(connection),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 24.dp),
        ) {
            items(filtered, key = { it.key }, contentType = { "app" }) { entry ->
                DrawerIcon(vm, entry, showLabel = showLabels)
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
    val onClick = remember(entry.key) { { vm.launch(entry) } }
    val onLongClick = remember(entry.key) { { menu = true } }
    Column(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
