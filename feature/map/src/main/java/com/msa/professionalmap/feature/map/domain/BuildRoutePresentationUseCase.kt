package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.GeoEngine
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteMetrics

/**
 * Builds render-ready route geometry for the map presentation layer.
 *
 * Route simplification, metric derivation and point-to-route-start measurement are geometry
 * policies. Keeping them here prevents MapViewModel from depending on low-level geometry
 * operations and makes route presentation deterministic in unit tests.
 */
class BuildRoutePresentationUseCase(
    private val geoEngine: GeoEngine,
) {
    operator fun invoke(
        points: List<GeoPoint>,
        simplificationToleranceMeters: Double,
    ): RoutePresentation? {
        if (points.size < MinimumRoutePoints) return null
        val simplified = geoEngine.simplifyRoute(points, simplificationToleranceMeters)
        return RoutePresentation(
            routePoints = points,
            simplifiedRoutePoints = simplified,
            metrics = geoEngine.routeMetrics(points, simplified),
        )
    }

    fun fromAlternative(
        alternative: RouteAlternative,
        simplificationToleranceMeters: Double,
    ): RoutePresentation = requireNotNull(invoke(alternative.points, simplificationToleranceMeters)) {
        "Route alternative must contain at least $MinimumRoutePoints points."
    }

    fun distanceFromRouteStartKm(routePoints: List<GeoPoint>, point: GeoPoint): Double? {
        val start = routePoints.firstOrNull() ?: return null
        return geoEngine.distanceMeters(start, point) / MetersPerKilometer
    }

    private companion object {
        const val MinimumRoutePoints = 2
        const val MetersPerKilometer = 1_000.0
    }
}

data class RoutePresentation(
    val routePoints: List<GeoPoint>,
    val simplifiedRoutePoints: List<GeoPoint>,
    val metrics: RouteMetrics,
)
