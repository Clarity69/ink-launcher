package com.taro.inklauncher.widget.custom

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class LayerType { TEXT, BOX, ICON, DOT_CLOCK, DIVIDER, DOT_PROGRESS }

@Serializable
enum class PosUnit { PERCENT, DP }

@Serializable
enum class HAlign { START, CENTER, END }

@Serializable
enum class FontKind { SANS, SERIF, MONO }

/** Hanya warna palet. INK = warna tinta (mengikuti mode terang/gelap), PAPER = warna kertas. */
@Serializable
enum class InkColor { INK, PAPER, MUTED, TRANSPARENT }

@Serializable
enum class BoxShape { RECT, CAPSULE, CIRCLE }

@Serializable
enum class TapAction { NONE, OPEN_APP, GOOGLE_SEARCH, GOOGLE_LENS, CLOCK, CALENDAR }

@Serializable
enum class ProgressSource { BATTERY, DAY, YEAR }

@Serializable
data class Layer(
    val id: String = newId(),
    val type: LayerType,
    val name: String = "",
    // Posisi & ukuran
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 100f,
    val h: Float = 30f,
    val unit: PosUnit = PosUnit.PERCENT,
    val align: HAlign = HAlign.START,
    // Teks
    val text: String = "",
    val fontSize: Float = 16f,
    val font: FontKind = FontKind.SANS,
    val bold: Boolean = false,
    // Warna
    val color: InkColor = InkColor.INK,
    // Kotak
    val shape: BoxShape = BoxShape.RECT,
    val filled: Boolean = false,
    val stroke: Float = 1.5f,
    val corner: Float = 12f,
    // Ikon
    val icon: String = "search",
    // Jam dot-matrix
    val hour24: Boolean = true,
    val showOffDots: Boolean = false,
    // Progres titik
    val source: ProgressSource = ProgressSource.BATTERY,
    val dots: Int = 10,
    // Aksi tap
    val action: TapAction = TapAction.NONE,
    val actionApp: String? = null,
)

@Serializable
data class CustomWidget(
    val id: String = newId(),
    val name: String,
    val w: Int = 4,
    val h: Int = 2,
    val layers: List<Layer> = emptyList(),
    val format: Int = 1,
)

fun newId(): String = UUID.randomUUID().toString()

val WidgetJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    coerceInputValues = true
}
