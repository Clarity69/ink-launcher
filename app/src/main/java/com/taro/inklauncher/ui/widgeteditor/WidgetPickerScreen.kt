package com.taro.inklauncher.ui.widgeteditor

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.taro.inklauncher.LauncherViewModel
import com.taro.inklauncher.MainActivity
import com.taro.inklauncher.R
import com.taro.inklauncher.ui.common.InkScaffold
import com.taro.inklauncher.ui.common.LocalPrefs
import com.taro.inklauncher.ui.common.SectionTitle
import com.taro.inklauncher.ui.theme.Figtree
import com.taro.inklauncher.ui.theme.LocalInk
import com.taro.inklauncher.widget.WidgetHostManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

private data class WidgetChoice(val info: AppWidgetProviderInfo, val label: String, val spanX: Int, val spanY: Int)
private data class WidgetGroup(val label: String, val widgets: List<WidgetChoice>)

/** Pemilih widget sistem, dikelompokkan per aplikasi, dengan preview. */
@Composable
fun WidgetPickerScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val prefs = LocalPrefs.current
    val ink = LocalInk.current
    val groups by produceState<List<WidgetGroup>?>(null) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val density = context.resources.displayMetrics.density
            val collator = Collator.getInstance()
            vm.widgetHost.providers()
                .groupBy { it.provider.packageName }
                .map { (pkg, infos) ->
                    val appLabel = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    }.getOrDefault(pkg)
                    WidgetGroup(
                        label = appLabel,
                        widgets = infos.map { info ->
                            val (sx, sy) = WidgetHostManager.spanOf(info, density, prefs.columns, prefs.rows)
                            WidgetChoice(info, runCatching { info.loadLabel(pm) }.getOrNull() ?: appLabel, sx, sy)
                        },
                    )
                }
                .sortedWith { a, b -> collator.compare(a.label, b.label) }
        }
    }

    InkScaffold(title = stringResource(R.string.widget_picker_title), onBack = vm::popScreen) {
        val list = groups
        if (list == null) {
            Text(stringResource(R.string.loading), color = ink.muted, modifier = Modifier.padding(16.dp))
        } else if (list.isEmpty()) {
            Text(stringResource(R.string.widget_none), color = ink.muted, modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                list.forEach { group ->
                    item(key = "h_" + group.label + group.widgets.first().info.provider.flattenToShortString()) {
                        SectionTitle(group.label)
                    }
                    items(group.widgets, key = { it.info.provider.flattenToShortString() + it.info.profile.hashCode() }) { w ->
                        WidgetRow(w) {
                            (context as? MainActivity)?.addWidget(w.info, w.spanX, w.spanY)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(choice: WidgetChoice, onClick: () -> Unit) {
    val ink = LocalInk.current
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(null, choice.info.provider) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val d = choice.info.loadPreviewImage(context, 0) ?: choice.info.loadIcon(context, 0)
                val iw = d.intrinsicWidth.takeIf { it > 0 } ?: 200
                val ih = d.intrinsicHeight.takeIf { it > 0 } ?: 200
                val scale = minOf(1f, 360f / maxOf(iw, ih))
                d.toBitmap((iw * scale).toInt().coerceAtLeast(1), (ih * scale).toInt().coerceAtLeast(1)).asImageBitmap()
            }.getOrNull()
        }
    }
    val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ink.muted, RoundedCornerShape(12.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            preview?.let {
                Image(it, null, contentScale = ContentScale.Fit, colorFilter = gray, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).heightIn(min = 48.dp)) {
            Text(choice.label, color = ink.fg, fontFamily = Figtree, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text("${choice.spanX} × ${choice.spanY}", color = ink.muted, fontFamily = Figtree, fontSize = 13.sp)
        }
    }
}
