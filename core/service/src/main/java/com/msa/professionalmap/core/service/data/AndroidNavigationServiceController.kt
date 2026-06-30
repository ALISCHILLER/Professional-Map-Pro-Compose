package com.msa.professionalmap.core.service.data

import android.content.Context
import androidx.core.content.ContextCompat
import com.msa.professionalmap.core.service.domain.NavigationServiceController
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot

class AndroidNavigationServiceController(
    context: Context,
) : NavigationServiceController {
    private val appContext = context.applicationContext

    override fun start(snapshot: NavigationServiceSnapshot) {
        ContextCompat.startForegroundService(
            appContext,
            ForegroundNavigationService.intent(appContext, ForegroundNavigationService.Action.Start, snapshot),
        )
    }

    override fun update(snapshot: NavigationServiceSnapshot) {
        ContextCompat.startForegroundService(
            appContext,
            ForegroundNavigationService.intent(appContext, ForegroundNavigationService.Action.Update, snapshot),
        )
    }

    override fun pause() {
        ContextCompat.startForegroundService(
            appContext,
            ForegroundNavigationService.intent(appContext, ForegroundNavigationService.Action.Pause),
        )
    }

    override fun resume(snapshot: NavigationServiceSnapshot) {
        ContextCompat.startForegroundService(
            appContext,
            ForegroundNavigationService.intent(appContext, ForegroundNavigationService.Action.Resume, snapshot),
        )
    }

    override fun stop() {
        // Stopping navigation must not start a new foreground service from a
        // background or teardown path. Starting a foreground service only to
        // deliver a stop command can hit Android 12+ foreground-service start
        // restrictions when the UI is no longer visible. stopService is safe
        // whether the service is running or already stopped, and onDestroy()
        // still releases the wake lock.
        appContext.stopService(
            ForegroundNavigationService.intent(appContext, ForegroundNavigationService.Action.Stop),
        )
    }
}
