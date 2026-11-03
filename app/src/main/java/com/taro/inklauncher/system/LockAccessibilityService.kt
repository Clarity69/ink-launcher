package com.taro.inklauncher.system

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Layanan aksesibilitas opsional, hanya untuk mengunci layar (double-tap).
 * Tidak membaca konten layar apa pun.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        var instance: LockAccessibilityService? = null
            private set

        val isRunning: Boolean get() = instance != null

        /** @return true jika layar berhasil dikunci. */
        fun lockScreen(): Boolean {
            val svc = instance ?: return false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
            return svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
