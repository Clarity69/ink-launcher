// Diadaptasi dari Lawnchair (GPL-3.0): lawnchair/src/app/lawnchair/gestures/handlers/OpenNotificationsHandler.kt
package com.taro.inklauncher.system

import android.annotation.SuppressLint
import android.content.Context

object StatusBarHelper {
    /** Membuka panel notifikasi lewat refleksi; gagal = diam saja. */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        try {
            val service = context.getSystemService("statusbar") ?: return
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .apply { isAccessible = true }
                .invoke(service)
        } catch (e: Throwable) {
            // diam
        }
    }
}
