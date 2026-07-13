package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteMetrics
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress
import com.msa.professionalmap.core.progress.domain.RouteProgressRepository

/**
 * Calculates and normalizes navigation progress for one accepted location update.
 *
 * The ViewModel should not know how progress is throttled, how a route is resolved,
 * how arrival candidates are confirmed, or how geometry is split for rendering. This
 * use case owns that workflow and returns an immutable update that presentation can
 * apply to UI/service state.
 */
class UpdateNavigationProgressUseCase(
    private val progressRepository: RouteProgressRepository,
    private val activeRouteResolver: ActiveRouteResolver,
    private val routeSplitter: RouteProgressRouteSplitter,
    private val arrivalGuard: ArrivalConfirmationGuard,
    private val progressUpdateThrottle: ProgressUpdateThrottle,
) {
    fun execute(
        input: NavigationProgressInput,
        location: DeviceLocation,
    ): NavigationProgressUpdate? {
        if (!input.navigationActive) return null
        if (!progressUpdateThrottle.shouldAccept(location.timestampMillis)) return null

        val route = activeRouteResolver.resolve(
            selectedRoute = input.selectedRoute,
            routePoints = input.routePoints,
            metrics = input.metrics,
        ) ?: return null

        val rawProgressState = progressRepository.calculateProgress(
            route = route,
            location = location.position,
            speedMetersPerSecond = location.speedMetersPerSecond?.toDouble(),
            timestampMillis = location.timestampMillis,
        )
        val progressState = arrivalGuard.confirm(rawProgressState, location)
        val progress = progressState.routeProgressOrNull()
        val split = routeSplitter.split(route.points, progress)
        val firstOffRouteAt = when (progressState) {
            is ProgressState.OffRoute -> input.firstOffRouteTimestampMillis ?: location.timestampMillis
            else -> null
        }

        return NavigationProgressUpdate(
            progressState = progressState,
            progress = progress,
            split = split,
            firstOffRouteTimestampMillis = firstOffRouteAt,
            isFirstOffRouteEvent = progressState is ProgressState.OffRoute && input.firstOffRouteTimestampMillis == null,
        )
    }

    fun reset() {
        arrivalGuard.reset()
        progressUpdateThrottle.reset()
    }

    private fun ProgressState.routeProgressOrNull(): RouteProgress? = when (this) {
        is ProgressState.Arrived -> progress
        is ProgressState.Navigating -> progress
        is ProgressState.OffRoute -> progress
        is ProgressState.Rerouting -> lastKnownProgress
        ProgressState.Idle -> null
    }
}

data class NavigationProgressInput(
    val navigationActive: Boolean,
    val selectedRoute: RouteAlternative?,
    val routePoints: List<GeoPoint>,
    val metrics: RouteMetrics?,
    val firstOffRouteTimestampMillis: Long?,
)

data class NavigationProgressUpdate(
    val progressState: ProgressState,
    val progress: RouteProgress?,
    val split: RouteSplit,
    val firstOffRouteTimestampMillis: Long?,
    val isFirstOffRouteEvent: Boolean,
)
