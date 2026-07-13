package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.service.domain.NavigationRuntimeState
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus

/** Pure mapping between service-owned navigation state and the screen state. */
internal fun NavigationRuntimeState.applyToUiState(
    current: MapUiState,
    previouslyActive: Boolean,
): MapUiState {
    val sessionRoute = session?.route
    return current.copy(
        routePoints = sessionRoute?.points ?: current.routePoints,
        simplifiedRoutePoints = sessionRoute?.points ?: current.simplifiedRoutePoints,
        routeAlternatives = sessionRoute?.let(::singleRouteList) ?: current.routeAlternatives,
        selectedRouteAlternativeId = sessionRoute?.id ?: current.selectedRouteAlternativeId,
        currentLocation = currentLocation ?: current.currentLocation,
        navigationActive = isRunning,
        navigationStatus = status,
        followUserLocation = when {
            isRunning -> true
            status == NavigationServiceStatus.Completed ||
                status == NavigationServiceStatus.Failed ||
                status == NavigationServiceStatus.Idle -> false
            else -> current.followUserLocation
        },
        progressState = progressState,
        completedRoutePoints = completedRoutePoints,
        remainingRoutePoints = remainingRoutePoints.ifEmpty {
            sessionRoute?.points ?: current.routePoints
        },
        snappedLocation = snappedLocation,
        firstOffRouteTimestampMillis = null,
        lastAction = toUiMessage(previouslyActive) ?: current.lastAction,
    )
}

internal fun NavigationRuntimeState.toUiMessage(previouslyActive: Boolean): MapUiMessage? = when (status) {
    NavigationServiceStatus.Starting,
    NavigationServiceStatus.Active -> if (!previouslyActive) MapUiMessage.NavigationStarted else null
    NavigationServiceStatus.Paused -> MapUiMessage.NavigationPaused
    NavigationServiceStatus.Rerouting -> MapUiMessage.Rerouting
    NavigationServiceStatus.Completed -> MapUiMessage.ArrivedAtDestination
    NavigationServiceStatus.Failed -> MapUiMessage.NavigationUnavailable
    NavigationServiceStatus.Stopping -> MapUiMessage.NavigationStopped
    NavigationServiceStatus.Idle -> if (previouslyActive) MapUiMessage.NavigationStopped else null
}

internal fun ProgressState.remainingDistanceMeters(): Double = when (this) {
    is ProgressState.Navigating -> progress.remainingDistanceMeters
    is ProgressState.OffRoute -> progress.remainingDistanceMeters
    is ProgressState.Rerouting -> lastKnownProgress?.remainingDistanceMeters ?: 0.0
    is ProgressState.Arrived -> progress.remainingDistanceMeters
    ProgressState.Idle -> 0.0
}

private fun singleRouteList(route: RouteAlternative): List<RouteAlternative> = listOf(route)
