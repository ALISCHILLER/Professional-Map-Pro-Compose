package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.OffRouteDetector
import com.msa.professionalmap.core.progress.domain.ProgressCalculator
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteLocationSample
import com.msa.professionalmap.core.progress.domain.RouteMatcher
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig
import com.msa.professionalmap.core.progress.domain.RouteProgressRepository

class DefaultRouteProgressRepository(
    private val config: RouteProgressConfig = RouteProgressConfig(),
    private val matcher: RouteMatcher = ProjectionRouteMatcher(),
    private val offRouteDetector: OffRouteDetector = DistanceOffRouteDetector(),
    private val progressCalculator: ProgressCalculator = DefaultProgressCalculator(),
) : RouteProgressRepository {
    private val selector = RouteMatchSelector(config)
    private var previousRouteId: String? = null
    private var previousMatch: MatchedRouteLocation? = null

    override fun calculateProgress(
        route: RouteAlternative,
        location: GeoPoint,
        speedMetersPerSecond: Double?,
        timestampMillis: Long,
    ): ProgressState = calculateProgress(
        route = route,
        sample = RouteLocationSample(
            position = location,
            speedMetersPerSecond = speedMetersPerSecond,
        ),
        timestampMillis = timestampMillis,
    )

    fun calculateProgress(
        route: RouteAlternative,
        sample: RouteLocationSample,
        timestampMillis: Long,
    ): ProgressState {
        if (previousRouteId != route.id) {
            previousRouteId = route.id
            previousMatch = null
        }
        val match = selectMatch(route, sample)
        previousMatch = match
        val progress = progressCalculator.calculate(
            route = route,
            match = match,
            speedMetersPerSecond = sample.speedMetersPerSecond,
            timestampMillis = timestampMillis,
        )
        val adaptiveConfig = config.copy(
            offRouteThresholdMeters = adaptiveOffRouteThreshold(sample.accuracyMeters),
        )
        return when {
            progress.remainingDistanceMeters <= config.arrivalThresholdMeters -> ProgressState.Arrived(progress)
            offRouteDetector.isOffRoute(match, adaptiveConfig) -> ProgressState.OffRoute(
                progress = progress,
                distanceFromRouteMeters = match.distanceFromRouteMeters,
            )
            else -> ProgressState.Navigating(progress)
        }
    }

    fun reset() {
        previousRouteId = null
        previousMatch = null
    }

    private fun selectMatch(
        route: RouteAlternative,
        sample: RouteLocationSample,
    ): MatchedRouteLocation {
        val projectionMatcher = matcher as? ProjectionRouteMatcher
            ?: return matcher.match(route.points, sample.position)
        return selector.select(
            routePoints = route.points,
            candidates = projectionMatcher.candidates(route.points, sample.position),
            previous = previousMatch,
            bearingDegrees = sample.bearingDegrees,
        )
    }

    private fun adaptiveOffRouteThreshold(accuracyMeters: Double?): Double {
        val accuracyBased = accuracyMeters
            ?.times(config.accuracyThresholdMultiplier)
            ?: config.offRouteThresholdMeters
        return accuracyBased.coerceIn(
            config.offRouteThresholdMeters,
            config.maximumOffRouteThresholdMeters,
        )
    }
}
