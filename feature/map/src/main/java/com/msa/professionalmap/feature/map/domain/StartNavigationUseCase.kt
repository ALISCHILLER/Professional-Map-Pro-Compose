package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteMetrics

/**
 * Validates whether the current map state can start turn-by-turn navigation.
 *
 * Route eligibility is part of the application contract: provider routes and local reference routes
 * can be navigated, while preview-only fallback routes cannot. This use case keeps that policy out
 * of presentation and returns the exact data needed to start telemetry, progress and foreground
 * service orchestration.
 */
class StartNavigationUseCase(
    private val activeRouteResolver: ActiveRouteResolver,
) {
    operator fun invoke(input: StartNavigationInput): StartNavigationDecision {
        val route = activeRouteResolver.resolve(
            selectedRoute = input.selectedRoute,
            routePoints = input.routePoints,
            metrics = input.metrics,
        ) ?: return StartNavigationDecision.MissingNavigableRoute
        val location = input.currentLocation ?: return StartNavigationDecision.MissingCurrentLocation
        return StartNavigationDecision.Ready(
            currentLocation = location,
            routeDistanceMeters = route.distanceMeters,
        )
    }
}

data class StartNavigationInput(
    val selectedRoute: RouteAlternative?,
    val routePoints: List<GeoPoint>,
    val metrics: RouteMetrics?,
    val currentLocation: DeviceLocation?,
)

sealed interface StartNavigationDecision {
    data object MissingNavigableRoute : StartNavigationDecision
    data object MissingCurrentLocation : StartNavigationDecision
    data class Ready(
        val currentLocation: DeviceLocation,
        val routeDistanceMeters: Double,
    ) : StartNavigationDecision
}
