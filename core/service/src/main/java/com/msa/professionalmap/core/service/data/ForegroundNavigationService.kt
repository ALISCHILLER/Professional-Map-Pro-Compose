package com.msa.professionalmap.core.service.data

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.service.domain.NavigationRuntimeErrorCode
import com.msa.professionalmap.core.service.domain.NavigationRuntimeState
import com.msa.professionalmap.core.service.domain.NavigationServiceConfig
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import com.msa.professionalmap.core.service.domain.NavigationSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** Android foreground shell around the service-owned navigation runtime. */
class ForegroundNavigationService : Service() {
    private val config = NavigationServiceConfig()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val notificationBuilder by lazy { ServiceNotificationBuilder(this, config) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val sessionStore by lazy { EncryptedNavigationSessionStore(this) }
    private val wakeLockManager by lazy { WakeLockManager(this) }
    private val guidanceSpeakerDelegate = lazy { ServiceVoiceGuidanceSpeaker(this, scope) }
    private val guidanceSpeaker by guidanceSpeakerDelegate
    private val engineDelegate = lazy {
        NavigationRuntimeEngine(
            context = this,
            scope = scope,
            sessionStore = sessionStore,
            wakeLockManager = wakeLockManager,
            detectPermissionLevel = ::detectPermissionLevel,
            onStateChanged = ::showState,
            onProgressChanged = { progress, location ->
                guidanceSpeaker.onProgress(progress, location.speedMetersPerSecond?.toDouble(), location.timestampMillis)
            },
            onCompleted = ::scheduleCompletedStop,
        )
    }
    private val engine by engineDelegate
    private var foregroundStarted = false

    private val inactiveStopRunnable = Runnable {
        if (NavigationRuntimeRegistry.state.value.status == NavigationServiceStatus.Paused) {
            stopRuntime(clearPersistedSession = true)
        }
    }

    private val completedStopRunnable = Runnable {
        stopRuntime(clearPersistedSession = true, preserveState = true)
    }

    private val failedStopRunnable = Runnable {
        // Preserve the encrypted session so a permission/provider failure can be retried
        // when the user returns to the app. The failed state remains visible to the UI.
        stopRuntime(clearPersistedSession = false, preserveState = true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (Action.from(intent?.action)) {
            Action.Start,
            Action.Restore,
            null -> {
                if (!hasLocationPermission()) return START_NOT_STICKY
                // Promote immediately. A persisted route can be several megabytes, so decrypting
                // and parsing it before startForeground() risks the Android FGS start deadline.
                if (!startForegroundImmediately(NavigationRuntimeRegistry.state.value.session)) {
                    return START_NOT_STICKY
                }
                engine.startOrRestore()?.let { session ->
                    guidanceSpeaker.updateConfig(session.guidanceConfig)
                }
            }
            Action.Pause -> {
                if (!hasLocationPermission()) return START_NOT_STICKY
                if (!foregroundStarted && !startForegroundImmediately(NavigationRuntimeRegistry.state.value.session)) {
                    return START_NOT_STICKY
                }
                if (guidanceSpeakerDelegate.isInitialized()) guidanceSpeaker.stopSpeaking()
                if (engine.pause()) scheduleInactiveStop()
            }
            Action.Resume -> {
                cancelInactiveStop()
                if (!hasLocationPermission()) return START_NOT_STICKY
                if (!foregroundStarted && !startForegroundImmediately(NavigationRuntimeRegistry.state.value.session)) {
                    return START_NOT_STICKY
                }
                sessionStore.read()?.guidanceConfig?.let(guidanceSpeaker::updateConfig)
                engine.resume()
            }
            Action.UpdateGuidance -> {
                if (foregroundStarted) {
                    sessionStore.read()?.guidanceConfig?.let(guidanceSpeaker::updateConfig)
                } else {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            Action.Stop -> stopRuntime(clearPersistedSession = true)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cancelInactiveStop()
        mainHandler.removeCallbacks(completedStopRunnable)
        mainHandler.removeCallbacks(failedStopRunnable)
        if (engineDelegate.isInitialized()) engine.close()
        if (guidanceSpeakerDelegate.isInitialized()) guidanceSpeaker.close()
        scope.cancel()
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        notificationManager.cancel(config.notificationId)
        foregroundStarted = false
        super.onDestroy()
    }

    private fun hasLocationPermission(): Boolean {
        if (detectPermissionLevel() != LocationPermissionLevel.None) return true
        failBeforeForeground(
            session = NavigationRuntimeRegistry.state.value.session,
            errorCode = NavigationRuntimeErrorCode.MissingLocationPermission,
        )
        return false
    }

    private fun startForegroundImmediately(
        session: NavigationSession?,
    ): Boolean {
        if (foregroundStarted) return true
        val snapshot = NavigationProgressPresentation.baseSnapshot(
            session = session,
            status = NavigationServiceStatus.Starting,
        )
        return try {
            startForegroundCompat(snapshot)
            true
        } catch (_: SecurityException) {
            failBeforeForeground(session, NavigationRuntimeErrorCode.ForegroundStartBlocked)
            false
        } catch (_: IllegalStateException) {
            failBeforeForeground(session, NavigationRuntimeErrorCode.ForegroundStartBlocked)
            false
        }
    }

    private fun showState(state: NavigationRuntimeState) {
        if (foregroundStarted) {
            notificationManager.notify(config.notificationId, notificationBuilder.build(state.snapshot))
        }
        if (state.status == NavigationServiceStatus.Failed) {
            if (guidanceSpeakerDelegate.isInitialized()) guidanceSpeaker.stopSpeaking()
            mainHandler.removeCallbacks(failedStopRunnable)
            mainHandler.postDelayed(failedStopRunnable, 2_000L)
        }
    }

    private fun startForegroundCompat(snapshot: NavigationServiceSnapshot) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            config.notificationId,
            notificationBuilder.build(snapshot),
            type,
        )
        foregroundStarted = true
    }

    private fun stopRuntime(clearPersistedSession: Boolean, preserveState: Boolean = false) {
        cancelInactiveStop()
        mainHandler.removeCallbacks(completedStopRunnable)
        mainHandler.removeCallbacks(failedStopRunnable)
        if (engineDelegate.isInitialized()) {
            engine.stop(clearPersistedSession)
        } else if (clearPersistedSession) {
            sessionStore.clear()
        }
        if (guidanceSpeakerDelegate.isInitialized()) guidanceSpeaker.stopSpeaking()
        if (!preserveState) NavigationRuntimeRegistry.reset()
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    private fun failBeforeForeground(
        session: NavigationSession?,
        errorCode: NavigationRuntimeErrorCode,
    ) {
        NavigationRuntimeRegistry.set(
            NavigationRuntimeState(
                status = NavigationServiceStatus.Failed,
                session = session,
                snapshot = NavigationProgressPresentation.baseSnapshot(
                    session = session,
                    status = NavigationServiceStatus.Failed,
                ),
                errorCode = errorCode,
            )
        )
        if (guidanceSpeakerDelegate.isInitialized()) guidanceSpeaker.stopSpeaking()
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    private fun scheduleInactiveStop() {
        cancelInactiveStop()
        mainHandler.postDelayed(inactiveStopRunnable, config.autoStopAfterInactiveMillis)
    }

    private fun cancelInactiveStop() {
        mainHandler.removeCallbacks(inactiveStopRunnable)
    }

    private fun scheduleCompletedStop() {
        mainHandler.removeCallbacks(completedStopRunnable)
        mainHandler.postDelayed(completedStopRunnable, 8_000L)
    }

    private fun detectPermissionLevel(): LocationPermissionLevel = when {
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
            LocationPermissionLevel.Precise
        }
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
            LocationPermissionLevel.Approximate
        }
        else -> LocationPermissionLevel.None
    }

    enum class Action(val actionName: String) {
        Start("com.msa.professionalmap.navigation.START"),
        Restore("com.msa.professionalmap.navigation.RESTORE"),
        Pause("com.msa.professionalmap.navigation.PAUSE"),
        Resume("com.msa.professionalmap.navigation.RESUME"),
        UpdateGuidance("com.msa.professionalmap.navigation.UPDATE_GUIDANCE"),
        Stop("com.msa.professionalmap.navigation.STOP");

        companion object {
            fun from(action: String?): Action? = entries.firstOrNull { it.actionName == action }
        }
    }

    companion object {
        fun intent(context: Context, action: Action): Intent =
            Intent(context, ForegroundNavigationService::class.java).setAction(action.actionName)
    }
}
