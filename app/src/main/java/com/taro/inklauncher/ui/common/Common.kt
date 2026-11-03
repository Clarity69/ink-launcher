package com.taro.inklauncher.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.taro.inklauncher.data.AppEntry
import com.taro.inklauncher.data.IconProcessor
import com.taro.inklauncher.data.IconShape
import com.taro.inklauncher.data.LauncherPrefs
import com.taro.inklauncher.ui.theme.LocalInk

val LocalPrefs = staticCompositionLocalOf { LauncherPrefs() }
val LocalIconProcessor = staticCompositionLocalOf<IconProcessor?> { null }

/** Ikon Material Symbols dari res/drawable. */
@Composable
fun MsIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalInk.current.fg,
) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

/** Ikon aplikasi monokrom tanpa label. */
@Composable
fun AppIconImage(entry: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val ink = LocalInk.current
    val prefs = LocalPrefs.current
    val processor = LocalIconProcessor.current ?: return
    val version by processor.version.collectAsState()
    val forceGray = entry.packageName in prefs.forceGrayscale
    val bitmap: ImageBitmap? by produceState(
        initialValue = processor.cached(entry, prefs.fallbackStyle, forceGray),
        entry.key, prefs.fallbackStyle, forceGray, version,
    ) {
        value = processor.icon(entry, prefs.fallbackStyle, forceGray)
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (prefs.iconShape) {
            IconShape.CIRCLE -> Box(Modifier.size(size).border(1.5.dp, ink.fg, CircleShape))
            IconShape.SQUIRCLE -> Box(Modifier.size(size).border(1.5.dp, ink.fg, RoundedCornerShape(30)))
            IconShape.NONE -> Unit
        }
        val glyph = if (prefs.iconShape == IconShape.NONE) size * 1.3f else size * 0.95f
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = entry.label,
                modifier = Modifier.requiredSize(glyph),
                colorFilter = ColorFilter.tint(ink.fg),
            )
        }
    }
}

data class MenuAction(
    val label: String,
    @DrawableRes val icon: Int? = null,
    val onClick: () -> Unit,
)

/** Menu tarik-turun bergaya e-ink: garis tepi, tanpa bayangan. */
@Composable
fun InkMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: List<MenuAction>,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
) {
    val ink = LocalInk.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset,
        containerColor = ink.bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, ink.fg),
        shape = RoundedCornerShape(14.dp),
    ) {
        actions.forEach { a ->
            val res = a.icon
            DropdownMenuItem(
                text = { Text(a.label, color = ink.fg) },
                leadingIcon = if (res != null) {
                    { MsIcon(res, null, size = 20.dp) }
                } else {
                    null
                },
                onClick = {
                    onDismiss()
                    a.onClick()
                },
            )
        }
    }
}
