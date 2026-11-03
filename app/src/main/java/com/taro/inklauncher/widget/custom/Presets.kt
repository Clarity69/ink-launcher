package com.taro.inklauncher.widget.custom

import android.content.Context
import com.taro.inklauncher.R

/** Tiga preset bawaan. */
object Presets {

    private lateinit var ctx: Context
    private fun t(id: Int): String = ctx.getString(id)

    private fun dotClockDate(): CustomWidget = CustomWidget(
        name = t(R.string.preset_dot_clock),
        w = 4,
        h = 2,
        layers = listOf(
            Layer(
                type = LayerType.DOT_CLOCK, name = t(R.string.layer_name_clock), x = 5f, y = 6f, w = 90f, h = 58f,
                align = HAlign.CENTER, hour24 = true, action = TapAction.CLOCK,
            ),
            Layer(
                type = LayerType.TEXT, name = t(R.string.layer_name_date), text = "{day}, {date}", x = 0f, y = 72f, w = 100f, h = 22f,
                align = HAlign.CENTER, fontSize = 16f, action = TapAction.CALENDAR,
            ),
        ),
    )

    private fun searchLens(): CustomWidget = CustomWidget(
        name = t(R.string.preset_search_lens),
        w = 4,
        h = 1,
        layers = listOf(
            Layer(
                type = LayerType.BOX, name = t(R.string.layer_name_capsule), x = 2f, y = 18f, w = 96f, h = 64f,
                shape = BoxShape.CAPSULE, stroke = 1.5f, action = TapAction.GOOGLE_SEARCH,
            ),
            Layer(
                type = LayerType.TEXT, name = "G", text = "G", x = 8f, y = 18f, w = 12f, h = 64f,
                align = HAlign.START, fontSize = 22f, bold = true, action = TapAction.GOOGLE_SEARCH,
            ),
            Layer(
                type = LayerType.ICON, name = t(R.string.layer_name_lens), icon = "center_focus_weak", x = 82f, y = 30f, w = 10f, h = 40f,
                align = HAlign.CENTER, action = TapAction.GOOGLE_LENS,
            ),
        ),
    )

    private fun serifClockBattery(): CustomWidget = CustomWidget(
        name = t(R.string.preset_serif_clock),
        w = 4,
        h = 2,
        layers = listOf(
            Layer(
                type = LayerType.TEXT, name = t(R.string.layer_name_clock), text = "{time}", x = 0f, y = 0f, w = 100f, h = 66f,
                align = HAlign.CENTER, font = FontKind.SERIF, fontSize = 72f, action = TapAction.CLOCK,
            ),
            Layer(
                type = LayerType.DOT_PROGRESS, name = t(R.string.layer_name_battery), source = ProgressSource.BATTERY, dots = 10,
                x = 22f, y = 74f, w = 40f, h = 12f, align = HAlign.START,
            ),
            Layer(
                type = LayerType.TEXT, name = t(R.string.layer_name_percent), text = "{battery}% {charging}", x = 64f, y = 70f, w = 34f, h = 20f,
                align = HAlign.START, fontSize = 14f,
            ),
        ),
    )

    fun all(context: Context): List<CustomWidget> {
        ctx = context.applicationContext
        return listOf(dotClockDate(), searchLens(), serifClockBattery())
    }
}
