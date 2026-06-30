package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.domain.OffRouteDetector
import com.msa.professionalmap.core.progress.domain.ProgressCalculator
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteMatcher
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig
import com.msa.professionalmap.core.progress.domain.RouteProgressRepository

class DefaultRouteProgressRepository(
    private val config: RouteProgressConfig = RouteProgressConfig(),
    private val matcher: RouteMatcher = ProjectionRouteMatcher(),
    private val offRouteDetector: OffRouteDetector = DistanceOffRouteDetector(),
    private val progressCalculator: ProgressCalculator = DefaultProgressCalculator(),
) : RouteProgressRepository {
    override fun calculateProgress(
        route: RouteAlternative,
        location: GeoPoint,
        speedMetersPerSecond: Double?,
        timestampMillis: Long,
    ): ProgressState {
        val match = matcher.match(route.points, location)
        val progress = progressCalculator.calculate(
            route = route,
            match = match,
            speedMetersPerSecond = speedMetersPerSecond,
            timestampMillis = timestampMillis,
        )

        return when {
            progress.remainingDistanceMeters <= config.arrivalThresholdMeters -> ProgressState.Arrived(progress)
            offRouteDetector.isOffRoute(match, config) -> ProgressState.OffRoute(
                progress = progress,
                distanceFromRouteMeters = match.distanceFromRouteMeters,
            )
            else -> ProgressState.Navigating(progress)
        }
    }
}
