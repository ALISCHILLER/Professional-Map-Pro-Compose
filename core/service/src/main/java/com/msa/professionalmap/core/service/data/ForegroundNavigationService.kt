package com.msa.professionalmap.core.service.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.service.domain.NavigationServiceConfig
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus

class ForegroundNavigationService : Service() {
    private val config = NavigationServiceConfig()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val notificationBuilder: ServiceNotificationBuilder by lazy { ServiceNotificationBuilder(this, config) }
    private val wakeLockManager: WakeLockManager by lazy { WakeLockManager(this) }
    private var snapshot = NavigationServiceSnapshot(status = NavigationServiceStatus.Idle)
    private var foregroundStarted = false

    private val inactiveStopRunnable = Runnable {
        if (snapshot.status == NavigationServiceStatus.Paused) stop()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (Action.from(intent?.action)) {
            Action.Start -> start(intent.toSnapshot(NavigationServiceStatus.Active))
            Action.Update -> updateOrStart(
                intent.toSnapshot(
                    snapshot.status.takeUnless { it == NavigationServiceStatus.Idle }
                        ?: NavigationServiceStatus.Active,
                )
            )
            Action.Pause -> pause()
            Action.Resume -> start(intent.toSnapshot(NavigationServiceStatus.Active))
            Action.Stop -> stop()
            null -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cancelInactiveStop()
        wakeLockManager.release()
        super.onDestroy()
    }

    private fun start(next: NavigationServiceSnapshot) {
        cancelInactiveStop()
        snapshot = next.copy(status = NavigationServiceStatus.Active)
        if (config.useWakeLock) wakeLockManager.acquire()
        startForegroundCompat(config.notificationId, notificationBuilder.build(snapshot))
        foregroundStarted = true
    }

    private fun updateOrStart(next: NavigationServiceSnapshot) {
        if (!foregroundStarted) {
            start(next.copy(status = NavigationServiceStatus.Active))
        } else {
            update(next)
        }
    }

    private fun update(next: NavigationServiceSnapshot) {
        cancelInactiveStop()
        snapshot = next
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(config.notificationId, notificationBuilder.build(snapshot))
    }

    private fun pause() {
        snapshot = snapshot.copy(
            status = NavigationServiceStatus.Paused,
            nextInstructionText = NavigationNotificationText.pausedInstruction(snapshot.languageTag),
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
        wakeLockManager.release()
        if (!foregroundStarted) {
            startForegroundCompat(config.notificationId, notificationBuilder.build(snapshot))
            foregroundStarted = true
        } else {
            updatePausedNotification(snapshot)
        }
        scheduleInactiveStop()
    }

    private fun stop() {
        cancelInactiveStop()
        snapshot = snapshot.copy(
            status = NavigationServiceStatus.Stopping,
            nextInstructionText = NavigationNotificationText.stoppedInstruction(snapshot.languageTag),
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
        wakeLockManager.release()
        if (!foregroundStarted) {
            startForegroundCompat(config.notificationId, notificationBuilder.build(snapshot))
        }
        foregroundStarted = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updatePausedNotification(next: NavigationServiceSnapshot) {
        snapshot = next
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(config.notificationId, notificationBuilder.build(snapshot))
    }

    private fun scheduleInactiveStop() {
        cancelInactiveStop()
        mainHandler.postDelayed(inactiveStopRunnable, config.autoStopAfterInactiveMillis)
    }

    private fun cancelInactiveStop() {
        mainHandler.removeCallbacks(inactiveStopRunnable)
    }

    private fun startForegroundCompat(id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(id, notification)
        }
    }

    private fun Intent?.toSnapshot(status: NavigationServiceStatus): NavigationServiceSnapshot {
        val lat = this?.getDoubleExtra(ExtraDestinationLat, Double.NaN) ?: Double.NaN
        val lon = this?.getDoubleExtra(ExtraDestinationLon, Double.NaN) ?: Double.NaN
        return NavigationServiceSnapshot(
            status = status,
            destinationTitle = this?.getStringExtra(ExtraDestinationTitle) ?: snapshot.destinationTitle,
            remainingDistanceText = this?.getStringExtra(ExtraRemainingDistance) ?: snapshot.remainingDistanceText,
            remainingDurationText = this?.getStringExtra(ExtraRemainingDuration) ?: snapshot.remainingDurationText,
            nextInstructionText = this?.getStringExtra(ExtraInstruction) ?: snapshot.nextInstructionText,
            destination = if (lat.isFinite() && lon.isFinite()) GeoPoint(lat, lon) else snapshot.destination,
            languageTag = this?.getStringExtra(ExtraLanguageTag) ?: snapshot.languageTag,
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
    }

    enum class Action(val actionName: String) {
        Start("com.msa.professionalmap.navigation.START"),
        Update("com.msa.professionalmap.navigation.UPDATE"),
        Pause("com.msa.professionalmap.navigation.PAUSE"),
        Resume("com.msa.professionalmap.navigation.RESUME"),
        Stop("com.msa.professionalmap.navigation.STOP");

        companion object {
            fun from(action: String?): Action? = entries.firstOrNull { it.actionName == action }
        }
    }

    companion object {
        private const val ExtraDestinationTitle = "extra_destination_title"
        private const val ExtraRemainingDistance = "extra_remaining_distance"
        private const val ExtraRemainingDuration = "extra_remaining_duration"
        private const val ExtraInstruction = "extra_instruction"
        private const val ExtraDestinationLat = "extra_destination_lat"
        private const val ExtraDestinationLon = "extra_destination_lon"
        private const val ExtraLanguageTag = "extra_language_tag"

        fun intent(context: Context, action: Action, snapshot: NavigationServiceSnapshot? = null): Intent {
            return Intent(context, ForegroundNavigationService::class.java).apply {
                this.action = action.actionName
                snapshot?.let {
                    putExtra(ExtraDestinationTitle, it.destinationTitle)
                    putExtra(ExtraRemainingDistance, it.remainingDistanceText)
                    putExtra(ExtraRemainingDuration, it.remainingDurationText)
                    putExtra(ExtraInstruction, it.nextInstructionText)
                    putExtra(ExtraDestinationLat, it.destination?.latitude ?: Double.NaN)
                    putExtra(ExtraDestinationLon, it.destination?.longitude ?: Double.NaN)
                    putExtra(ExtraLanguageTag, it.languageTag)
                }
            }
        }
    }
}
