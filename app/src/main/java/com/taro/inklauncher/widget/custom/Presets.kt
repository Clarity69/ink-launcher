package com.taro.inklauncher.widget.custom

/** Tiga preset bawaan. */
object Presets {

    fun dotClockDate(): CustomWidget = CustomWidget(
        name = "Jam titik + tanggal",
        w = 4,
        h = 2,
        layers = listOf(
            Layer(
                type = LayerType.DOT_CLOCK, name = "Jam", x = 5f, y = 6f, w = 90f, h = 58f,
                align = HAlign.CENTER, hour24 = true, action = TapAction.CLOCK,
            ),
            Layer(
                type = LayerType.TEXT, name = "Tanggal", text = "{day}, {date}", x = 0f, y = 72f, w = 100f, h = 22f,
                align = HAlign.CENTER, fontSize = 16f, action = TapAction.CALENDAR,
            ),
        ),
    )

    fun searchLens(): CustomWidget = CustomWidget(
        name = "Pencarian G + Lens",
        w = 4,
        h = 1,
        layers = listOf(
            Layer(
                type = LayerType.BOX, name = "Kapsul", x = 2f, y = 18f, w = 96f, h = 64f,
                shape = BoxShape.CAPSULE, stroke = 1.5f, action = TapAction.GOOGLE_SEARCH,
            ),
            Layer(
                type = LayerType.TEXT, name = "G", text = "G", x = 8f, y = 18f, w = 12f, h = 64f,
                align = HAlign.START, fontSize = 22f, bold = true, action = TapAction.GOOGLE_SEARCH,
            ),
            Layer(
                type = LayerType.ICON, name = "Lens", icon = "center_focus_weak", x = 82f, y = 30f, w = 10f, h = 40f,
                align = HAlign.CENTER, action = TapAction.GOOGLE_LENS,
            ),
        ),
    )

    fun serifClockBattery(): CustomWidget = CustomWidget(
        name = "Jam serif + baterai",
        w = 4,
        h = 2,
        layers = listOf(
            Layer(
                type = LayerType.TEXT, name = "Jam", text = "{time}", x = 0f, y = 0f, w = 100f, h = 66f,
                align = HAlign.CENTER, font = FontKind.SERIF, fontSize = 72f, action = TapAction.CLOCK,
            ),
            Layer(
                type = LayerType.DOT_PROGRESS, name = "Baterai", source = ProgressSource.BATTERY, dots = 10,
                x = 22f, y = 74f, w = 40f, h = 12f, align = HAlign.START,
            ),
            Layer(
                type = LayerType.TEXT, name = "Persen", text = "{battery}% {charging}", x = 64f, y = 70f, w = 34f, h = 20f,
                align = HAlign.START, fontSize = 14f,
            ),
        ),
    )

    fun all(): List<CustomWidget> = listOf(dotClockDate(), searchLens(), serifClockBattery())
}
