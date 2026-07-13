package com.msa.professionalmap.core.service.data

import android.content.Context
import android.os.PowerManager

internal class WakeLockManager(context: Context) {
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire(tag: String = "ProfessionalMapPro:Navigation") {
        if (wakeLock?.isHeld == true) return
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag).apply {
            setReferenceCounted(false)
            acquire(WakeLockTimeoutMillis)
        }
    }

    /** Refreshes the bounded lock while active location updates are being delivered. */
    fun renew() {
        val lock = wakeLock ?: return acquire()
        if (lock.isHeld) lock.release()
        lock.acquire(WakeLockTimeoutMillis)
    }

    fun release() {
        val lock = wakeLock
        if (lock?.isHeld == true) lock.release()
        wakeLock = null
    }

    private companion object {
        const val WakeLockTimeoutMillis = 10L * 60L * 1000L
    }
}
