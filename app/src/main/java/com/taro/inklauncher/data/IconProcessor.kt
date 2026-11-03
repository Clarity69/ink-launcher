// Pendekatan themed/monochrome icon diadaptasi dari Lawnchair (GPL-3.0):
// lawnchair/src/app/lawnchair/icons/ThemedIconCompat.kt & LawnchairIconProvider.kt
package com.taro.inklauncher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

enum class IconSource { THEMED, MATERIAL, GRAYSCALE }

/**
 * Mengubah ikon aplikasi menjadi topeng monokrom (hitam di atas transparan).
 * Warna akhir (Ink/Paper) diberikan saat render lewat ColorFilter.tint, jadi cache
 * tidak perlu dibuang ketika tema berganti.
 *
 * Urutan sumber: AdaptiveIconDrawable.getMonochrome() (Android 13+) ->
 * peta Material Symbols -> grayscale + kontras + threshold menjadi ikon garis.
 */
class IconProcessor(private val context: Context, private val repo: AppRepository) {

    private val density = context.resources.displayMetrics.density
    val sizePx: Int = (64 * density).toInt().coerceIn(96, 200)

    private val maxBytes: Int = (Runtime.getRuntime().maxMemory() / 16)
        .coerceAtMost(12L * 1024 * 1024).toInt()

    private val cache = object : LruCache<String, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    private val _version = MutableStateFlow(0)
    /** Naik setiap kali cache dibersihkan sehingga UI memuat ulang ikon. */
    val version: StateFlow<Int> = _version

    fun clear() {
        cache.evictAll()
        _version.value++
    }

    fun invalidatePackage(pkg: String) {
        cache.snapshot().keys.filter { it.contains(pkg) }.forEach { cache.remove(it) }
        _version.value++
    }

    private fun cacheKey(entry: AppEntry, style: FallbackStyle, forceGray: Boolean) =
        "${entry.key}|$style|$forceGray"

    fun cached(entry: AppEntry, style: FallbackStyle, forceGray: Boolean): ImageBitmap? =
        cache.get(cacheKey(entry, style, forceGray))

    suspend fun icon(entry: AppEntry, style: FallbackStyle, forceGray: Boolean): ImageBitmap {
        val key = cacheKey(entry, style, forceGray)
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.Default) {
            val bmp = runCatching { render(entry, style, forceGray) }.getOrNull()
                ?: Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val img = bmp.asImageBitmap()
            cache.put(key, img)
            img
        }
    }

    fun sourceOf(entry: AppEntry, style: FallbackStyle, forceGray: Boolean): IconSource {
        if (forceGray) return IconSource.GRAYSCALE
        val drawable = loadDrawable(entry)
        if (Build.VERSION.SDK_INT >= 33 && drawable is AdaptiveIconDrawable && drawable.monochrome != null) {
            return IconSource.THEMED
        }
        if (style == FallbackStyle.MATERIAL && MaterialIconMap.forPackage(entry.packageName) != null) {
            return IconSource.MATERIAL
        }
        return IconSource.GRAYSCALE
    }

    private fun loadDrawable(entry: AppEntry): Drawable? {
        val info = repo.activityInfo(entry) ?: return null
        return runCatching { info.getIcon(context.resources.displayMetrics.densityDpi) }.getOrNull()
    }

    private fun render(entry: AppEntry, style: FallbackStyle, forceGray: Boolean): Bitmap {
        val drawable = loadDrawable(entry)
        if (!forceGray && Build.VERSION.SDK_INT >= 33 && drawable is AdaptiveIconDrawable) {
            val mono = drawable.monochrome
            if (mono != null) return drawAdaptiveLayer(mono)
        }
        if (!forceGray && style == FallbackStyle.MATERIAL) {
            MaterialIconMap.forPackage(entry.packageName)?.let { res ->
                context.getDrawable(res)?.let { return drawGlyph(it, 0.2f) }
            }
        }
        if (drawable == null) {
            return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        }
        return lineArt(drawable)
    }

    /** Layer adaptive (108dp) digambar 1.5x agar area aman 72dp memenuhi bitmap. */
    private fun drawAdaptiveLayer(layer: Drawable): Bitmap {
        val s = sizePx
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val d = layer.mutate()
        d.setBounds(-s / 4, -s / 4, s + s / 4, s + s / 4)
        d.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        d.draw(c)
        return bmp
    }

    private fun drawGlyph(d: Drawable, insetFraction: Float): Bitmap {
        val s = sizePx
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val inset = (s * insetFraction).toInt()
        val dd = d.mutate()
        dd.setBounds(inset, inset, s - inset, s - inset)
        dd.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        dd.draw(c)
        return bmp
    }

    /**
     * Fallback: grayscale (saturasi 0) + kontras dinaikkan + threshold, lalu diambil
     * tepinya sehingga hasilnya terlihat seperti ikon garis monokrom.
     */
    private fun lineArt(drawable: Drawable): Bitmap {
        val s = sizePx
        val src = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(src)
        if (drawable is AdaptiveIconDrawable && drawable.foreground != null) {
            val fg = drawable.foreground.mutate()
            fg.setBounds(-s / 4, -s / 4, s + s / 4, s + s / 4)
            fg.draw(c)
        } else {
            val inset = (s * 0.16f).toInt()
            val d = drawable.mutate()
            d.setBounds(inset, inset, s - inset, s - inset)
            d.draw(c)
        }

        val px = IntArray(s * s)
        src.getPixels(px, 0, s, 0, 0, s, s)
        src.recycle()

        val n = s * s
        val opaque = BooleanArray(n)
        val lum = FloatArray(n)
        var count = 0
        var sum = 0f
        for (i in 0 until n) {
            val p = px[i]
            val a = (p ushr 24) and 0xFF
            if (a > 128) {
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                // Grayscale (saturasi 0) lalu kontras 1.6x.
                val l = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val boosted = ((l - 0.5f) * 1.6f + 0.5f).coerceIn(0f, 1f)
                lum[i] = boosted
                opaque[i] = true
                count++
                sum += boosted
            }
        }
        if (count == 0) return Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val mean = sum / count
        val coverage = count.toFloat() / n
        val fullBleed = coverage > 0.5f

        // Piksel "detail" = yang kontras terhadap warna dominan.
        val fill = BooleanArray(n)
        var fillCount = 0
        for (i in 0 until n) {
            if (!opaque[i]) continue
            val detail = abs(lum[i] - mean) > 0.3f
            val on = if (fullBleed) detail else !detail
            if (on) { fill[i] = true; fillCount++ }
        }
        if (fillCount < n * 0.02f) {
            // Terlalu sedikit detail: pakai siluet saja.
            for (i in 0 until n) fill[i] = opaque[i]
        }

        val r = max(2, s / 36)
        val eroded = erode(fill, s, r)
        val out = IntArray(n)
        for (i in 0 until n) {
            if (fill[i] && !eroded[i]) out[i] = Color.BLACK
        }
        val result = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, s, 0, 0, s, s)
        return result
    }

    /** Erosi kotak (2r+1)^2 memakai integral image. */
    private fun erode(mask: BooleanArray, s: Int, r: Int): BooleanArray {
        val w = s + 1
        val integral = IntArray(w * w)
        for (y in 0 until s) {
            var rowSum = 0
            for (x in 0 until s) {
                if (mask[y * s + x]) rowSum++
                integral[(y + 1) * w + (x + 1)] = integral[y * w + (x + 1)] + rowSum
            }
        }
        val out = BooleanArray(s * s)
        val full = (2 * r + 1) * (2 * r + 1)
        for (y in 0 until s) {
            for (x in 0 until s) {
                if (!mask[y * s + x]) continue
                val x0 = x - r
                val y0 = y - r
                val x1 = x + r + 1
                val y1 = y + r + 1
                if (x0 < 0 || y0 < 0 || x1 > s || y1 > s) continue
                val total = integral[y1 * w + x1] - integral[y0 * w + x1] - integral[y1 * w + x0] + integral[y0 * w + x0]
                out[y * s + x] = total == full
            }
        }
        return out
    }
}
