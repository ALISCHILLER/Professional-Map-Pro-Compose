package com.msa.professionalmap.core.service.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.msa.professionalmap.core.service.domain.NavigationServiceConfig
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus

internal class ServiceNotificationBuilder(
    private val context: Context,
    private val config: NavigationServiceConfig,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannel(languageTag: String = "en") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                config.notificationChannelId,
                config.notificationChannelName,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = NavigationNotificationText.channelDescription(languageTag)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun build(snapshot: NavigationServiceSnapshot): Notification {
        ensureChannel(snapshot.languageTag)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = PendingIntent.getActivity(
            context,
            RequestLaunch,
            launchIntent ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val primaryAction = if (snapshot.status == NavigationServiceStatus.Paused) {
            Notification.Action.Builder(
                android.R.drawable.ic_media_play,
                NavigationNotificationText.resumeAction(snapshot.languageTag),
                serviceIntent(ActionRequestResume, ForegroundNavigationService.Action.Resume, snapshot),
            ).build()
        } else {
            Notification.Action.Builder(
                android.R.drawable.ic_media_pause,
                NavigationNotificationText.pauseAction(snapshot.languageTag),
                serviceIntent(ActionRequestPause, ForegroundNavigationService.Action.Pause, snapshot),
            ).build()
        }
        val stopAction = Notification.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            NavigationNotificationText.stopAction(snapshot.languageTag),
            serviceIntent(ActionRequestStop, ForegroundNavigationService.Action.Stop, snapshot),
        ).build()

        return Notification.Builder(context, config.notificationChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(NavigationNotificationText.contentTitle(snapshot))
            .setContentText(NavigationNotificationText.contentText(snapshot))
            .setStyle(
                Notification.BigTextStyle().bigText(
                    snapshot.nextInstructionText ?: NavigationNotificationText.fallbackInstruction(snapshot.languageTag),
                )
            )
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setPriority(Notification.PRIORITY_HIGH)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(buildPublicSnapshotNotification(snapshot, contentIntent))
            .setOngoing(snapshot.status != NavigationServiceStatus.Stopping)
            .setOnlyAlertOnce(true)
            .addAction(primaryAction)
            .addAction(stopAction)
            .build()
    }

    private fun buildPublicSnapshotNotification(
        snapshot: NavigationServiceSnapshot,
        contentIntent: PendingIntent,
    ): Notification = Notification.Builder(context, config.notificationChannelId)
        .setSmallIcon(android.R.drawable.ic_dialog_map)
        .setContentTitle(NavigationNotificationText.publicContentTitle(snapshot.languageTag))
        .setContentText(NavigationNotificationText.publicContentText(snapshot.languageTag))
        .setStyle(
            Notification.BigTextStyle().bigText(
                NavigationNotificationText.publicContentText(snapshot.languageTag),
            )
        )
        .setContentIntent(contentIntent)
        .setCategory(Notification.CATEGORY_NAVIGATION)
        .setPriority(Notification.PRIORITY_HIGH)
        .setVisibility(Notification.VISIBILITY_PUBLIC)
        .setOnlyAlertOnce(true)
        .build()

    private fun serviceIntent(
        requestCode: Int,
        action: ForegroundNavigationService.Action,
        snapshot: NavigationServiceSnapshot,
    ): PendingIntent = PendingIntent.getService(
        context,
        requestCode,
        ForegroundNavigationService.intent(context, action, snapshot),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val RequestLaunch = 10
        const val ActionRequestPause = 11
        const val ActionRequestResume = 12
        const val ActionRequestStop = 13
    }
}
