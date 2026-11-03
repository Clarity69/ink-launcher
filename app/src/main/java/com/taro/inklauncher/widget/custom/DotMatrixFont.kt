package com.taro.inklauncher.widget.custom

/** Font titik 5×7 sederhana (gaya papan titik) untuk jam dot-matrix. */
object DotMatrixFont {
    const val ROWS = 7

    private val glyphs: Map<Char, List<String>> = mapOf(
        '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
        '1' to listOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
        '2' to listOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
        '3' to listOf("11111", "00010", "00100", "00010", "00001", "10001", "01110"),
        '4' to listOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
        '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
        '6' to listOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
        '7' to listOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
        '8' to listOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
        '9' to listOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
        ':' to listOf("0", "0", "1", "0", "1", "0", "0"),
        ' ' to listOf("00", "00", "00", "00", "00", "00", "00"),
        'A' to listOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
        'P' to listOf("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
        'M' to listOf("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
    )

    fun glyph(c: Char): List<String> = glyphs[c] ?: glyphs.getValue(' ')

    /** Total kolom untuk teks, termasuk jarak 1 kolom antar karakter. */
    fun columns(text: String): Int =
        text.sumOf { glyph(it)[0].length } + (text.length - 1).coerceAtLeast(0)
}
