package com.taro.inklauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val Paper = Color(0xFFF4F1EA)
val Ink = Color(0xFF111111)
val Muted = Color(0xFF5A5A5A)

@Immutable
data class InkColors(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val dark: Boolean,
)

val LightInk = InkColors(bg = Paper, fg = Ink, muted = Muted, dark = false)
val DarkInk = InkColors(bg = Ink, fg = Paper, muted = Muted, dark = true)

val LocalInk = staticCompositionLocalOf { LightInk }

/** Durasi animasi singkat ala e-ink. */
object InkMotion {
    const val SHORT = 160
    const val MEDIUM = 200
}

private fun scheme(c: InkColors): ColorScheme {
    val bg = c.bg
    val fg = c.fg
    val muted = c.muted
    return if (c.dark) {
        darkColorScheme(
            primary = fg, onPrimary = bg, primaryContainer = bg, onPrimaryContainer = fg,
            inversePrimary = bg, secondary = fg, onSecondary = bg, secondaryContainer = bg,
            onSecondaryContainer = fg, tertiary = fg, onTertiary = bg, tertiaryContainer = bg,
            onTertiaryContainer = fg, background = bg, onBackground = fg, surface = bg,
            onSurface = fg, surfaceVariant = bg, onSurfaceVariant = muted, surfaceTint = bg,
            inverseSurface = fg, inverseOnSurface = bg, error = fg, onError = bg,
            errorContainer = bg, onErrorContainer = fg, outline = muted, outlineVariant = muted,
            scrim = Color.Transparent, surfaceBright = bg, surfaceContainer = bg,
            surfaceContainerHigh = bg, surfaceContainerHighest = bg, surfaceContainerLow = bg,
            surfaceContainerLowest = bg, surfaceDim = bg,
        )
    } else {
        lightColorScheme(
            primary = fg, onPrimary = bg, primaryContainer = bg, onPrimaryContainer = fg,
            inversePrimary = bg, secondary = fg, onSecondary = bg, secondaryContainer = bg,
            onSecondaryContainer = fg, tertiary = fg, onTertiary = bg, tertiaryContainer = bg,
            onTertiaryContainer = fg, background = bg, onBackground = fg, surface = bg,
            onSurface = fg, surfaceVariant = bg, onSurfaceVariant = muted, surfaceTint = bg,
            inverseSurface = fg, inverseOnSurface = bg, error = fg, onError = bg,
            errorContainer = bg, onErrorContainer = fg, outline = muted, outlineVariant = muted,
            scrim = Color.Transparent, surfaceBright = bg, surfaceContainer = bg,
            surfaceContainerHigh = bg, surfaceContainerHighest = bg, surfaceContainerLow = bg,
            surfaceContainerLowest = bg, surfaceDim = bg,
        )
    }
}

private val InkShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun InkTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkInk else LightInk
    CompositionLocalProvider(LocalInk provides colors) {
        MaterialTheme(
            colorScheme = scheme(colors),
            typography = InkTypography,
            shapes = InkShapes,
            content = content,
        )
    }
}
