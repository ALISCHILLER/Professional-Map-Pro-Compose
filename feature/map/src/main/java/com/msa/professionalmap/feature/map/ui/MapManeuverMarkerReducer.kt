package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Reduces dense step geometry to readable, non-overlapping maneuver markers. */
internal object MapManeuverMarkerReducer {
    fun reduce(route: RouteAlternative?): List<GeoPoint> {
        route ?: return emptyList()
        val origin = route.points.firstOrNull() ?: return emptyList()
        val destination = route.points.lastOrNull() ?: return emptyList()
        val result = mutableListOf<GeoPoint>()
        route.legs.asSequence()
            .flatMap { it.steps.asSequence() }
            .mapNotNull { it.location }
            .filter { point ->
                distanceMeters(point, origin) >= EndpointExclusionMeters &&
                    distanceMeters(point, destination) >= EndpointExclusionMeters
            }
            .forEach { point ->
                if (result.lastOrNull()?.let { distanceMeters(it, point) >= MinimumSpacingMeters } != false) {
                    result += point
                }
            }
        return result.take(MaximumMarkers)
    }

    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = Math.toRadians(b.latitude - a.latitude)
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(deltaLat / 2.0).pow(2.0) +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2.0)
        return 2.0 * EarthRadiusMeters * atan2(sqrt(h), sqrt(1.0 - h))
    }

    private const val EarthRadiusMeters = 6_371_000.0
    private const val EndpointExclusionMeters = 25.0
    private const val MinimumSpacingMeters = 38.0
    private const val MaximumMarkers = 24
}
