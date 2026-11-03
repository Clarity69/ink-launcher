// Alur AppWidgetHost/bind/konfigurasi diadaptasi dari pendekatan Lawnchair/Launcher3 (GPL-3.0 / Apache-2.0),
// ditulis ulang secara ringan.
package com.taro.inklauncher.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.UserManager
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import kotlin.math.abs
import kotlin.math.ceil

/** AppWidgetHostView yang bisa mendeteksi long-press tanpa mengganggu klik widget. */
class InkWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPress: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressed = false

    private val longPressRunnable = Runnable {
        longPressed = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressed = false
                downX = ev.x
                downY = ev.y
                handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(ev.x - downX) > touchSlop || abs(ev.y - downY) > touchSlop) {
                    handler.removeCallbacks(longPressRunnable)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(longPressRunnable)
        }
        return longPressed
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (longPressed) {
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
                longPressed = false
            }
            return true
        }
        return super.onTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(longPressRunnable)
        super.onDetachedFromWindow()
    }
}

class WidgetHostManager(private val context: Context) {

    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)

    val host: AppWidgetHost = object : AppWidgetHost(context, HOST_ID) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = InkWidgetHostView(context)
    }

    private val views = HashMap<Int, InkWidgetHostView>()

    fun startListening() {
        runCatching { host.startListening() }
    }

    fun stopListening() {
        runCatching { host.stopListening() }
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun deleteId(id: Int) {
        views.remove(id)
        runCatching { host.deleteAppWidgetId(id) }
    }

    fun info(id: Int): AppWidgetProviderInfo? = runCatching { manager.getAppWidgetInfo(id) }.getOrNull()

    fun bindIfAllowed(id: Int, info: AppWidgetProviderInfo): Boolean =
        runCatching { manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null) }.getOrDefault(false)

    /** View di-cache supaya tidak dibuat ulang saat halaman pager dibuang/dirakit lagi. */
    fun getView(activityContext: Context, id: Int): InkWidgetHostView? {
        views[id]?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
            return v
        }
        val info = info(id) ?: return null
        val v = runCatching { host.createView(activityContext, id, info) as? InkWidgetHostView }.getOrNull() ?: return null
        views[id] = v
        return v
    }

    fun clearViews() {
        views.clear()
    }

    fun updateSize(id: Int, widthDp: Int, heightDp: Int) {
        runCatching {
            val opts = manager.getAppWidgetOptions(id)
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            manager.updateAppWidgetOptions(id, opts)
        }
    }

    fun providers(): List<AppWidgetProviderInfo> {
        val um = context.getSystemService(UserManager::class.java)
        val profiles = runCatching { um.userProfiles }.getOrDefault(emptyList())
        if (profiles.isEmpty()) return runCatching { manager.installedProviders }.getOrDefault(emptyList())
        return profiles.flatMap { user ->
            runCatching { manager.getInstalledProvidersForProfile(user) }.getOrDefault(emptyList())
        }
    }

    companion object {
        const val HOST_ID = 1024

        /** Perkiraan jumlah sel dari ukuran minimum widget. */
        fun spanOf(info: AppWidgetProviderInfo, density: Float, cols: Int, rows: Int): Pair<Int, Int> {
            var sx = 0
            var sy = 0
            if (Build.VERSION.SDK_INT >= 31) {
                sx = info.targetCellWidth
                sy = info.targetCellHeight
            }
            if (sx <= 0) sx = ceil((info.minWidth / density + 30f) / 70f).toInt()
            if (sy <= 0) sy = ceil((info.minHeight / density + 30f) / 70f).toInt()
            return sx.coerceIn(1, cols) to sy.coerceIn(1, rows)
        }
    }
}
