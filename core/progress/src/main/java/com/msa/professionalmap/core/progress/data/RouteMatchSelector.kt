package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig

/** Selects a stable road projection using distance, heading and continuity. */
internal class RouteMatchSelector(
    private val config: RouteProgressConfig,
) {
    fun select(
        routePoints: List<GeoPoint>,
        candidates: List<MatchedRouteLocation>,
        previous: MatchedRouteLocation?,
        bearingDegrees: Double?,
    ): MatchedRouteLocation = candidates.minByOrNull { candidate ->
        candidate.distanceFromRouteMeters +
            headingPenalty(routePoints, candidate, bearingDegrees) +
            continuityPenalty(candidate, previous)
    } ?: error("Route matching requires at least one candidate.")

    private fun headingPenalty(
        routePoints: List<GeoPoint>,
        candidate: MatchedRouteLocation,
        bearingDegrees: Double?,
    ): Double {
        val bearing = bearingDegrees ?: return 0.0
        val start = routePoints.getOrNull(candidate.segmentIndex) ?: return 0.0
        val end = routePoints.getOrNull(candidate.segmentIndex + 1) ?: return 0.0
        val segmentBearing = GeoProgressMath.bearingDegrees(start, end)
        val mismatch = GeoProgressMath.angularDifferenceDegrees(bearing, segmentBearing) / 180.0
        return mismatch * config.headingPenaltyMeters
    }

    private fun continuityPenalty(
        candidate: MatchedRouteLocation,
        previous: MatchedRouteLocation?,
    ): Double {
        previous ?: return 0.0
        val backwardMeters = previous.distanceFromStartMeters - candidate.distanceFromStartMeters
        if (backwardMeters <= config.maximumBackwardJumpMeters) return 0.0
        return config.headingPenaltyMeters + backwardMeters
    }
}
