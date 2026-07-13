package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.RouteMatcher
/**
 * Matches a GPS point to the nearest projection on a route segment.
 * This is the practical baseline before heavier HMM/Viterbi map matching.
 */
class ProjectionRouteMatcher : RouteMatcher {
    override fun match(routePoints: List<GeoPoint>, location: GeoPoint): MatchedRouteLocation =
        candidates(routePoints, location).first()

    internal fun candidates(
        routePoints: List<GeoPoint>,
        location: GeoPoint,
    ): List<MatchedRouteLocation> {
        require(routePoints.size >= 2) { "Route matching requires at least two route points." }
        var cumulativeMeters = 0.0
        val candidates = buildList {
            routePoints.zipWithNext().forEachIndexed { index, (start, end) ->
                val segmentLength = GeoProgressMath.distanceMeters(start, end)
                if (segmentLength <= 0.0) return@forEachIndexed
                val endLocal = GeoProgressMath.toLocalMeters(start, end)
                val locationLocal = GeoProgressMath.toLocalMeters(start, location)
                val lengthSquared = endLocal.x * endLocal.x + endLocal.y * endLocal.y
                val fraction = if (lengthSquared == 0.0) {
                    0.0
                } else {
                    ((locationLocal.x * endLocal.x + locationLocal.y * endLocal.y) / lengthSquared)
                        .coerceIn(0.0, 1.0)
                }
                val snapped = GeoProgressMath.interpolate(start, end, fraction)
                add(
                    MatchedRouteLocation(
                        originalLocation = location,
                        snappedLocation = snapped,
                        segmentIndex = index,
                        segmentFraction = fraction,
                        distanceFromRouteMeters = GeoProgressMath.distanceMeters(location, snapped),
                        distanceFromStartMeters = cumulativeMeters + segmentLength * fraction,
                    )
                )
                cumulativeMeters += segmentLength
            }
        }
        if (candidates.isNotEmpty()) return candidates.sortedBy { it.distanceFromRouteMeters }.take(MaxCandidates)
        return listOf(
            MatchedRouteLocation(
                originalLocation = location,
                snappedLocation = routePoints.first(),
                segmentIndex = 0,
                segmentFraction = 0.0,
                distanceFromRouteMeters = GeoProgressMath.distanceMeters(location, routePoints.first()),
                distanceFromStartMeters = 0.0,
            )
        )
    }

    private companion object {
        const val MaxCandidates = 12
    }
}

