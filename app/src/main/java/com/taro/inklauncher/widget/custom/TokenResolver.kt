package com.taro.inklauncher.widget.custom

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TokenContext(
    val now: Long,
    val batteryLevel: Int,
    val charging: Boolean,
    val nextAlarm: String,
    val locale: Locale,
    val chargingLabel: String,
)

/**
 * Mengganti token {time}, {time12}, {hour}, {minute}, {date}, {day}, {month}, {year},
 * {battery}, {charging}, {next_alarm}. Format tanggal mengikuti locale.
 */
object TokenResolver {

    val TOKENS = listOf(
        "{time}", "{time12}", "{hour}", "{minute}", "{date}", "{day}",
        "{month}", "{year}", "{battery}", "{charging}", "{next_alarm}",
    )

    private val regex = Regex("\\{([a-z0-9_]+)\\}")

    fun resolve(text: String, ctx: TokenContext): String {
        if ('{' !in text) return text
        val date = Date(ctx.now)
        val locale = ctx.locale
        fun pattern(p: String) = SimpleDateFormat(p, locale).format(date)
        fun skeleton(s: String) = pattern(DateFormat.getBestDateTimePattern(locale, s))
        return regex.replace(text) { m ->
            when (m.groupValues[1]) {
                "time" -> pattern("HH:mm")
                "time12" -> pattern("h:mm")
                "hour" -> pattern("HH")
                "minute" -> pattern("mm")
                "date" -> skeleton("dMMMM")
                "day" -> skeleton("EEEE")
                "month" -> skeleton("MMMM")
                "year" -> pattern("yyyy")
                "battery" -> if (ctx.batteryLevel >= 0) ctx.batteryLevel.toString() else "--"
                "charging" -> if (ctx.charging) ctx.chargingLabel else ""
                "next_alarm" -> ctx.nextAlarm
                else -> m.value
            }
        }
    }

    fun clockText(ctx: TokenContext, hour24: Boolean): String =
        SimpleDateFormat(if (hour24) "HH:mm" else "h:mm", ctx.locale).format(Date(ctx.now))
}
