package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.GeoEngine
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteMetrics

/**
 * Resolves the route that should be used by navigation/progress calculations.
 *
 * The UI can show either a provider route alternative or a local reference route. Keeping
 * this decision outside `MapViewModel` preserves Single Responsibility: the ViewModel no longer
 * knows how to synthesize a route alternative or how to calculate route distance fallbacks.
 */
class ActiveRouteResolver(
    private val geoEngine: GeoEngine,
) {
    fun resolve(
        selectedRoute: RouteAlternative?,
        routePoints: List<GeoPoint>,
        metrics: RouteMetrics?,
    ): RouteAlternative? {
        selectedRoute?.let { route ->
            return route.takeIf { it.isNavigationEligible }
        }
        if (routePoints.size < MIN_ROUTE_POINTS) return null
        return RouteAlternative(
            id = LOCAL_ACTIVE_ROUTE_ID,
            title = LOCAL_ACTIVE_ROUTE_TITLE,
            summary = LOCAL_ACTIVE_ROUTE_SUMMARY,
            points = routePoints,
            distanceMeters = metrics?.totalDistanceMeters ?: geoEngine.routeMetrics(routePoints, routePoints).totalDistanceMeters,
            durationSeconds = 0.0,
            provider = LOCAL_ACTIVE_ROUTE_PROVIDER,
        )
    }

    private companion object {
        private const val MIN_ROUTE_POINTS = 2
        private const val LOCAL_ACTIVE_ROUTE_ID = "active-route"
        private const val LOCAL_ACTIVE_ROUTE_TITLE = "Active route"
        private const val LOCAL_ACTIVE_ROUTE_SUMMARY = "Current map route"
        private const val LOCAL_ACTIVE_ROUTE_PROVIDER = "local"
    }
}
