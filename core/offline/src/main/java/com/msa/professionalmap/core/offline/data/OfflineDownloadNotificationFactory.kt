package com.msa.professionalmap.core.offline.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo

internal class OfflineDownloadNotificationFactory(
    context: Context,
    private val messages: OfflineDownloadMessages,
) {
    private val appContext = context.applicationContext

    fun create(title: String, percent: Int): ForegroundInfo {
        ensureChannelExists()
        val safePercent = percent.coerceIn(0, 100)
        val notification = Notification.Builder(appContext, ChannelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(messages.notificationTitle())
            .setContentText(messages.notificationBody(title, safePercent))
            .setProgress(100, safePercent, safePercent == 0)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NotificationId, notification)
        }
    }

    private fun ensureChannelExists() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(ChannelId, ChannelName, NotificationManager.IMPORTANCE_LOW)
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val NotificationId = 5051
        const val ChannelId = "offline_downloads"
        const val ChannelName = "Offline downloads"
    }
}
