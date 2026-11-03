package com.taro.inklauncher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.provider.CalendarContract
import android.text.format.DateFormat
import com.taro.inklauncher.GlanceLine
import com.taro.inklauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Sumber data baris kedua "At a Glance": acara kalender terdekat atau cuaca Open-Meteo. */
class GlanceProvider(private val context: Context) {

    private fun granted(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    suspend fun nextEvent(): GlanceLine? = withContext(Dispatchers.IO) {
        if (!granted(Manifest.permission.READ_CALENDAR)) return@withContext null
        runCatching {
            val now = System.currentTimeMillis()
            val end = now + 36L * 60 * 60 * 1000
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now)
            ContentUris.appendId(builder, end)
            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
            )
            context.contentResolver.query(
                builder.build(),
                projection,
                "${CalendarContract.Instances.END} >= ?",
                arrayOf(now.toString()),
                "${CalendarContract.Instances.ALL_DAY} ASC, ${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                var found: GlanceLine? = null
                while (found == null && c.moveToNext()) {
                    val title = c.getString(0)
                    if (title.isNullOrBlank()) continue
                    val begin = c.getLong(1)
                    val allDay = c.getInt(3) == 1
                    val text = if (allDay) {
                        title
                    } else {
                        val locale = Locale.getDefault()
                        val sameDay = isSameDay(begin, now)
                        val skeleton = if (sameDay) "HHmm" else "EEEHHmm"
                        val time = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(Date(begin))
                        "$time · $title"
                    }
                    found = GlanceLine(R.drawable.ic_ms_event, text)
                }
                found
            }
        }.getOrNull()
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    @SuppressLint("MissingPermission")
    private suspend fun location(): Location? {
        if (!granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val last = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null) return last
        if (Build.VERSION.SDK_INT < 30) return null
        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                runCatching {
                    lm.getCurrentLocation(
                        LocationManager.NETWORK_PROVIDER,
                        null,
                        context.mainExecutor,
                    ) { loc -> if (cont.isActive) cont.resume(loc) }
                }.onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
    }

    suspend fun weather(): GlanceLine? = withContext(Dispatchers.IO) {
        val loc = location() ?: return@withContext null
        runCatching {
            val url = URL(
                String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code",
                    loc.latitude,
                    loc.longitude,
                ),
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "InkLauncher")
            }
            val body = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val current = Json.parseToJsonElement(body).jsonObject["current"]!!.jsonObject
            val temp = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull ?: return@runCatching null
            val code = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 0
            GlanceLine(iconFor(code), "${temp.roundToInt()}° · ${context.getString(describe(code))}")
        }.getOrNull()
    }

    private fun iconFor(code: Int): Int = when (code) {
        0 -> R.drawable.ic_ms_sunny
        1, 2 -> R.drawable.ic_ms_partly_cloudy_day
        3 -> R.drawable.ic_ms_cloud
        45, 48 -> R.drawable.ic_ms_foggy
        in 71..77, 85, 86 -> R.drawable.ic_ms_weather_snowy
        in 95..99 -> R.drawable.ic_ms_thunderstorm
        in 51..67, in 80..82 -> R.drawable.ic_ms_rainy
        else -> R.drawable.ic_ms_cloud
    }

    private fun describe(code: Int): Int = when (code) {
        0 -> R.string.weather_clear
        1, 2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_cloudy
        45, 48 -> R.string.weather_fog
        in 51..57 -> R.string.weather_drizzle
        in 61..67 -> R.string.weather_rain
        in 71..77, 85, 86 -> R.string.weather_snow
        in 80..82 -> R.string.weather_showers
        in 95..99 -> R.string.weather_thunder
        else -> R.string.weather_cloudy
    }
}
