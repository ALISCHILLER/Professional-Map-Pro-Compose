package com.msa.professionalmap.core.service.data

import android.content.Context
import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import androidx.core.content.ContextCompat
import com.msa.professionalmap.core.service.domain.NavigationRuntimeErrorCode
import com.msa.professionalmap.core.service.domain.NavigationRuntimeState
import com.msa.professionalmap.core.service.domain.NavigationServiceController
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import com.msa.professionalmap.core.service.domain.NavigationSession
import kotlinx.coroutines.flow.StateFlow

/** Android edge for controlling the independent foreground navigation runtime. */
class AndroidNavigationServiceController(
    context: Context,
) : NavigationServiceController {
    private val appContext = context.applicationContext
    private val sessionStore = EncryptedNavigationSessionStore(appContext)

    override val runtimeState: StateFlow<NavigationRuntimeState> = NavigationRuntimeRegistry.state

    override fun start(session: NavigationSession) {
        val persisted = runCatching { sessionStore.save(session) }.isSuccess
        if (!persisted) {
            fail(session, NavigationRuntimeErrorCode.SessionPersistenceFailed)
            return
        }

        NavigationRuntimeRegistry.set(
            NavigationRuntimeState(
                status = NavigationServiceStatus.Starting,
                session = session,
                snapshot = initialSnapshot(session, NavigationServiceStatus.Starting),
            )
        )
        startForegroundCommand(ForegroundNavigationService.Action.Start, session)
    }

    override fun pause() {
        if (!runtimeState.value.isRunning) return
        startRegularCommand(ForegroundNavigationService.Action.Pause)
    }

    override fun resume() {
        val running = runtimeState.value
        if (running.status == NavigationServiceStatus.Paused) {
            startRegularCommand(ForegroundNavigationService.Action.Resume)
            return
        }
        restore()
    }


    override fun updateGuidance(config: GuidanceConfig) {
        val current = runtimeState.value.session ?: sessionStore.read() ?: return
        val updated = current.copy(
            languageTag = config.language.bcp47Tag,
            guidanceConfig = config,
        )
        if (runCatching { sessionStore.save(updated) }.isFailure) {
            fail(current, NavigationRuntimeErrorCode.SessionPersistenceFailed)
            return
        }
        NavigationRuntimeRegistry.update { state ->
            state.copy(
                session = updated,
                snapshot = state.snapshot.copy(
                    languageTag = updated.languageTag,
                    lastUpdatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
        if (runtimeState.value.isRunning) {
            startRegularCommand(ForegroundNavigationService.Action.UpdateGuidance)
        }
    }

    override fun stop() {
        sessionStore.clear()
        NavigationRuntimeRegistry.update { current ->
            current.copy(
                status = NavigationServiceStatus.Stopping,
                snapshot = current.snapshot.copy(status = NavigationServiceStatus.Stopping),
                errorCode = null,
            )
        }
        appContext.stopService(ForegroundNavigationService.intent(appContext, ForegroundNavigationService.Action.Stop))
        NavigationRuntimeRegistry.reset()
    }

    override fun restore() {
        if (runtimeState.value.isRunning) return
        val session = sessionStore.read()
        if (session == null) {
            // A fresh app launch without an active encrypted session is a normal idle state.
            // Do not surface it as a navigation failure in the UI.
            NavigationRuntimeRegistry.reset()
            return
        }
        NavigationRuntimeRegistry.set(
            NavigationRuntimeState(
                status = NavigationServiceStatus.Starting,
                session = session,
                snapshot = initialSnapshot(session, NavigationServiceStatus.Starting),
            )
        )
        startForegroundCommand(ForegroundNavigationService.Action.Restore, session)
    }

    private fun startForegroundCommand(
        action: ForegroundNavigationService.Action,
        session: NavigationSession,
    ) {
        try {
            ContextCompat.startForegroundService(appContext, ForegroundNavigationService.intent(appContext, action))
        } catch (_: SecurityException) {
            fail(session, NavigationRuntimeErrorCode.ForegroundStartBlocked)
        } catch (_: IllegalStateException) {
            fail(session, NavigationRuntimeErrorCode.ForegroundStartBlocked)
        }
    }

    private fun startRegularCommand(action: ForegroundNavigationService.Action) {
        val command = ForegroundNavigationService.intent(appContext, action)
        try {
            // Commands for an already-running foreground service do not need another
            // startForegroundService call. This avoids Android 12+ background-start restrictions.
            appContext.startService(command)
        } catch (_: SecurityException) {
            NavigationRuntimeRegistry.update {
                it.copy(
                    status = NavigationServiceStatus.Failed,
                    errorCode = NavigationRuntimeErrorCode.ForegroundStartBlocked,
                )
            }
        } catch (_: IllegalStateException) {
            NavigationRuntimeRegistry.update {
                it.copy(
                    status = NavigationServiceStatus.Failed,
                    errorCode = NavigationRuntimeErrorCode.ForegroundStartBlocked,
                )
            }
        }
    }

    private fun fail(session: NavigationSession, code: NavigationRuntimeErrorCode) {
        NavigationRuntimeRegistry.set(
            NavigationRuntimeState(
                status = NavigationServiceStatus.Failed,
                session = session,
                snapshot = initialSnapshot(session, NavigationServiceStatus.Failed),
                errorCode = code,
            )
        )
    }

    private fun initialSnapshot(
        session: NavigationSession,
        status: NavigationServiceStatus,
    ): NavigationServiceSnapshot = NavigationServiceSnapshot(
        status = status,
        destinationTitle = session.destinationTitle,
        destination = session.destination,
        languageTag = session.languageTag,
        lastUpdatedAtMillis = System.currentTimeMillis(),
    )
}
