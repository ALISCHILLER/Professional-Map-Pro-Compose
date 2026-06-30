package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.progress.domain.RouteProgress

/**
 * Splits a route into completed and remaining geometry based on the matched progress point.
 *
 * Map rendering needs separate line layers for the traveled and remaining route segments. This
 * class keeps that pure geometry operation independent from MapLibre and ViewModel state.
 */
class RouteProgressRouteSplitter {
    fun split(route: List<GeoPoint>, progress: RouteProgress?): RouteSplit {
        if (route.size < MIN_ROUTE_POINTS || progress == null) {
            return RouteSplit(completedPoints = emptyList(), remainingPoints = route)
        }
        val segmentIndex = progress.matchedLocation.segmentIndex.coerceIn(0, route.lastIndex - 1)
        val snapped = progress.matchedLocation.snappedLocation
        val completed = buildList {
            addAll(route.take(segmentIndex + 1))
            if (lastOrNull() != snapped) add(snapped)
        }
        val remaining = buildList {
            add(snapped)
            addAll(route.drop(segmentIndex + 1))
        }
        return RouteSplit(completedPoints = completed, remainingPoints = remaining)
    }

    private companion object {
        private const val MIN_ROUTE_POINTS = 2
    }
}

data class RouteSplit(
    val completedPoints: List<GeoPoint>,
    val remainingPoints: List<GeoPoint>,
)
