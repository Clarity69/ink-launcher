package com.taro.inklauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.taro.inklauncher.R

/** Figtree (OFL) — sans bergaya Pixel. */
val Figtree = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_medium, FontWeight.Medium),
    Font(R.font.figtree_medium, FontWeight.SemiBold),
)

/** Instrument Serif (OFL) — untuk jam besar. */
val InkSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
)

private val base = Typography()

val InkTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Figtree),
    displayMedium = base.displayMedium.copy(fontFamily = Figtree),
    displaySmall = base.displaySmall.copy(fontFamily = Figtree),
    headlineLarge = base.headlineLarge.copy(fontFamily = Figtree),
    headlineMedium = base.headlineMedium.copy(fontFamily = Figtree),
    headlineSmall = base.headlineSmall.copy(fontFamily = Figtree),
    titleLarge = base.titleLarge.copy(fontFamily = Figtree),
    titleMedium = base.titleMedium.copy(fontFamily = Figtree),
    titleSmall = base.titleSmall.copy(fontFamily = Figtree),
    bodyLarge = base.bodyLarge.copy(fontFamily = Figtree),
    bodyMedium = base.bodyMedium.copy(fontFamily = Figtree),
    bodySmall = base.bodySmall.copy(fontFamily = Figtree),
    labelLarge = base.labelLarge.copy(fontFamily = Figtree),
    labelMedium = base.labelMedium.copy(fontFamily = Figtree),
    labelSmall = base.labelSmall.copy(fontFamily = Figtree),
)
