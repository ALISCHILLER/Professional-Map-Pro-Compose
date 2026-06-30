package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.RouteMatcher
/**
 * Matches a GPS point to the nearest projection on a route segment.
 * This is the practical baseline before heavier HMM/Viterbi map matching.
 */
class ProjectionRouteMatcher : RouteMatcher {
    override fun match(routePoints: List<GeoPoint>, location: GeoPoint): MatchedRouteLocation {
        require(routePoints.size >= 2) { "Route matching requires at least two route points." }

        var cumulativeMeters = 0.0
        var best: MatchedRouteLocation? = null

        routePoints.zipWithNext().forEachIndexed { index, (start, end) ->
            val segmentLength = GeoProgressMath.distanceMeters(start, end)
            if (segmentLength <= 0.0) return@forEachIndexed

            val startLocal = GeoProgressMath.toLocalMeters(start, start)
            val endLocal = GeoProgressMath.toLocalMeters(start, end)
            val locationLocal = GeoProgressMath.toLocalMeters(start, location)

            val vx = endLocal.x - startLocal.x
            val vy = endLocal.y - startLocal.y
            val wx = locationLocal.x - startLocal.x
            val wy = locationLocal.y - startLocal.y
            val lengthSquared = vx * vx + vy * vy
            val fraction = if (lengthSquared == 0.0) 0.0 else ((wx * vx + wy * vy) / lengthSquared).coerceIn(0.0, 1.0)
            val snapped = GeoProgressMath.interpolate(start, end, fraction)
            val distanceFromRoute = GeoProgressMath.distanceMeters(location, snapped)
            val distanceFromStart = cumulativeMeters + segmentLength * fraction

            val candidate = MatchedRouteLocation(
                originalLocation = location,
                snappedLocation = snapped,
                segmentIndex = index,
                segmentFraction = fraction,
                distanceFromRouteMeters = distanceFromRoute,
                distanceFromStartMeters = distanceFromStart,
            )

            val currentBest = best
            if (currentBest == null || candidate.distanceFromRouteMeters < currentBest.distanceFromRouteMeters) {
                best = candidate
            }

            cumulativeMeters += segmentLength
        }

        return best ?: MatchedRouteLocation(
            originalLocation = location,
            snappedLocation = routePoints.first(),
            segmentIndex = 0,
            segmentFraction = 0.0,
            distanceFromRouteMeters = GeoProgressMath.distanceMeters(location, routePoints.first()),
            distanceFromStartMeters = 0.0,
        )
    }
}
