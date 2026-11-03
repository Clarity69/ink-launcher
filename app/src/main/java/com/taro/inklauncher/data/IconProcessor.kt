// Pendekatan themed/monochrome icon diadaptasi dari Lawnchair (GPL-3.0):
// lawnchair/src/app/lawnchair/icons/ThemedIconCompat.kt & LawnchairIconProvider.kt
package com.taro.inklauncher.data

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import com.taro.inklauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

enum class IconSource { THEMED, MATERIAL, GRAYSCALE, LETTER }

/**
 * @param isMask true = topeng hitam di atas transparan (diwarnai Ink/Paper saat render);
 *               false = ikon grayscale penuh (ditampilkan apa adanya / dibalik di mode gelap).
 */
@Immutable
data class IconResult(val image: ImageBitmap, val isMask: Boolean, val source: IconSource)

/**
 * Mengubah ikon aplikasi menjadi monokrom.
 * Urutan: themed icon (Android 13+) -> Material Symbols (peta package) -> grayscale lembut
 * dari ikon lengkap (background + foreground + mask) -> grayscale ikon asli -> huruf pertama.
 * Setiap hasil divalidasi; satu ikon rusak tidak memengaruhi ikon lain.
 */
class IconProcessor(private val context: Context, private val repo: AppRepository) {

    private val densityDpi = context.resources.displayMetrics.densityDpi
    private val density = context.resources.displayMetrics.density
    val sizePx: Int = (64 * density).toInt().coerceIn(96, 192)

    private val maxBytes: Int = run {
        val am = context.getSystemService(ActivityManager::class.java)
        val mb = (am?.memoryClass ?: 128) / 8
        (mb.coerceIn(4, 16) * 1024 * 1024)
    }

    private val cache = object : LruCache<String, IconResult>(maxBytes) {
        override fun sizeOf(key: String, value: IconResult): Int = value.image.width * value.image.height * 4
    }

    private val _version = MutableStateFlow(0)
    /** Naik setiap kali cache dibersihkan sehingga UI memuat ulang ikon. */
    val version: StateFlow<Int> = _version

    private val letterTypeface: Typeface by lazy {
        runCatching { ResourcesCompat.getFont(context, R.font.figtree_medium) }.getOrNull() ?: Typeface.DEFAULT_BOLD
    }

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

    fun cached(entry: AppEntry, style: FallbackStyle, forceGray: Boolean): IconResult? =
        cache.get(cacheKey(entry, style, forceGray))

    suspend fun icon(entry: AppEntry, style: FallbackStyle, forceGray: Boolean): IconResult {
        val key = cacheKey(entry, style, forceGray)
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.Default) {
            val result = try {
                render(entry, style, forceGray)
            } catch (t: Throwable) {
                Log.w(TAG, "Gagal memproses ikon ${entry.key}", t)
                letterIcon(entry.label)
            }
            cache.put(key, result)
            result
        }
    }

    suspend fun sourceOf(entry: AppEntry, style: FallbackStyle, forceGray: Boolean): IconSource =
        icon(entry, style, forceGray).source

    private fun loadDrawable(entry: AppEntry): Drawable? {
        val info = repo.activityInfo(entry) ?: return null
        return try {
            info.getBadgedIcon(densityDpi)
        } catch (t: Throwable) {
            Log.w(TAG, "getBadgedIcon gagal ${entry.key}", t)
            runCatching { info.getIcon(densityDpi) }.getOrNull()
        }
    }

    private fun render(entry: AppEntry, style: FallbackStyle, forceGray: Boolean): IconResult {
        val drawable = loadDrawable(entry)

        // 1) Themed icon resmi.
        if (!forceGray && Build.VERSION.SDK_INT >= 33 && drawable is AdaptiveIconDrawable) {
            val mono = drawable.monochrome
            if (mono != null) {
                attempt(entry, "themed") { drawAdaptiveLayer(mono) }
                    ?.takeIf { isValidMask(it) }
                    ?.let { return IconResult(it.asImageBitmap(), true, IconSource.THEMED) }
            }
        }

        // 2) Material Symbols dari peta package.
        if (!forceGray && style == FallbackStyle.MATERIAL) {
            materialIcon(entry)?.let { return it }
        }

        if (drawable != null) {
            // 3) Ikon lengkap (bg + fg + mask) -> grayscale dengan kontras lembut.
            attempt(entry, "grayscale") { grayscale(renderFull(drawable), contrast = 1.25f) }
                ?.takeIf { isValidGray(it) }
                ?.let { return IconResult(it.asImageBitmap(), false, IconSource.GRAYSCALE) }
            // 3a) Grayscale ikon asli tanpa penyesuaian kontras.
            attempt(entry, "grayscale-plain") { grayscale(renderFull(drawable), contrast = 1f) }
                ?.takeIf { isValidGray(it, strict = false) }
                ?.let { return IconResult(it.asImageBitmap(), false, IconSource.GRAYSCALE) }
        }

        // 3b) Material Symbols (walau gaya fallback = grayscale).
        materialIcon(entry)?.let { return it }

        // 3c) Huruf pertama dalam lingkaran.
        return letterIcon(entry.label)
    }

    private inline fun attempt(entry: AppEntry, step: String, block: () -> Bitmap): Bitmap? = try {
        block()
    } catch (t: Throwable) {
        Log.w(TAG, "Langkah $step gagal untuk ${entry.key}", t)
        null
    }

    private fun materialIcon(entry: AppEntry): IconResult? {
        val res = MaterialIconMap.forPackage(entry.packageName) ?: return null
        val d = runCatching { context.getDrawable(res) }.getOrNull() ?: return null
        val bmp = attempt(entry, "material") { drawGlyph(d, 0.2f) } ?: return null
        return IconResult(bmp.asImageBitmap(), true, IconSource.MATERIAL)
    }

    private fun newBitmap(): Bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)

    /** Layer adaptive (108dp) digambar 1.5x agar area aman 72dp memenuhi bitmap. */
    private fun drawAdaptiveLayer(layer: Drawable): Bitmap {
        val s = sizePx
        val bmp = newBitmap()
        val d = layer.mutate()
        d.setBounds(-s / 4, -s / 4, s + s / 4, s + s / 4)
        d.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        d.draw(Canvas(bmp))
        return bmp
    }

    private fun drawGlyph(d: Drawable, insetFraction: Float): Bitmap {
        val s = sizePx
        val bmp = newBitmap()
        val inset = (s * insetFraction).toInt()
        val dd = d.mutate()
        dd.setBounds(inset, inset, s - inset, s - inset)
        dd.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        dd.draw(Canvas(bmp))
        return bmp
    }

    /**
     * Render ikon lengkap ke ukuran pasti. AdaptiveIconDrawable.draw() sudah menggabungkan
     * background + foreground lalu memotongnya dengan mask ikon sistem.
     * Drawable lain (Bitmap/Vector/dll.) digambar memenuhi bounds yang sama.
     */
    private fun renderFull(drawable: Drawable): Bitmap {
        val s = sizePx
        val bmp = newBitmap()
        val d = drawable.mutate()
        val pad = (s * 0.04f).toInt()
        d.setBounds(pad, pad, s - pad, s - pad)
        d.draw(Canvas(bmp))
        return bmp
    }

    private fun grayscale(src: Bitmap, contrast: Float): Bitmap {
        val out = newBitmap()
        val cm = ColorMatrix().apply { setSaturation(0f) }
        if (contrast != 1f) {
            val t = 128f * (1f - contrast)
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, t,
                        0f, contrast, 0f, 0f, t,
                        0f, 0f, contrast, 0f, t,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        src.recycle()
        return out
    }

    private class Stats(val opaqueFraction: Float, val lumStdDev: Float)

    private fun stats(bmp: Bitmap): Stats {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        var opaque = 0
        var sum = 0.0
        var sumSq = 0.0
        for (p in px) {
            val a = (p ushr 24) and 0xFF
            if (a > 32) {
                opaque++
                val l = (0.299 * ((p shr 16) and 0xFF) + 0.587 * ((p shr 8) and 0xFF) + 0.114 * (p and 0xFF)) / 255.0
                sum += l
                sumSq += l * l
            }
        }
        if (opaque == 0) return Stats(0f, 0f)
        val mean = sum / opaque
        val variance = (sumSq / opaque - mean * mean).coerceAtLeast(0.0)
        return Stats(opaque.toFloat() / px.size, sqrt(variance).toFloat())
    }

    /** Topeng (themed/material) valid bila tidak hampir kosong dan tidak hampir penuh. */
    private fun isValidMask(bmp: Bitmap): Boolean {
        val s = stats(bmp)
        return s.opaqueFraction in 0.01f..0.92f
    }

    /** Grayscale valid bila tidak kosong dan tidak berupa satu warna rata. */
    private fun isValidGray(bmp: Bitmap, strict: Boolean = true): Boolean {
        val s = stats(bmp)
        if (s.opaqueFraction < 0.03f) return false
        val minStd = if (strict) 0.035f else 0.015f
        return s.lumStdDev >= minStd
    }

    /** Fallback terakhir: huruf pertama nama aplikasi dalam lingkaran outline (topeng). */
    private fun letterIcon(label: String): IconResult {
        val s = sizePx
        val bmp = newBitmap()
        val c = Canvas(bmp)
        val stroke = s / 24f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = Color.BLACK
        }
        c.drawCircle(s / 2f, s / 2f, s * 0.36f, ring)
        val letter = label.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = s * 0.34f
            textAlign = Paint.Align.CENTER
            typeface = letterTypeface
        }
        val y = s / 2f - (text.descent() + text.ascent()) / 2f
        c.drawText(letter, s / 2f, y, text)
        return IconResult(bmp.asImageBitmap(), true, IconSource.LETTER)
    }

    companion object {
        const val TAG = "InkIcons"
    }
}
