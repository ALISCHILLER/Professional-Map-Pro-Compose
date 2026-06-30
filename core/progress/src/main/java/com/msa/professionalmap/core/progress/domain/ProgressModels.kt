package com.msa.professionalmap.core.progress.domain

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteInstruction

/**
 * Configuration knobs for navigation-grade progress tracking.
 * Keep values injectable so apps can tune them for city driving, walking, cycling or truck modes.
 */
data class RouteProgressConfig(
    val offRouteThresholdMeters: Double = 20.0,
    val arrivalThresholdMeters: Double = 25.0,
    val minReliableSpeedMetersPerSecond: Double = 1.2,
    val rerouteDebounceMillis: Long = 5_000L,
) {
    init {
        require(offRouteThresholdMeters > 0.0) { "offRouteThresholdMeters must be positive." }
        require(arrivalThresholdMeters > 0.0) { "arrivalThresholdMeters must be positive." }
        require(minReliableSpeedMetersPerSecond >= 0.0) { "minReliableSpeedMetersPerSecond must be non-negative." }
        require(rerouteDebounceMillis >= 0L) { "rerouteDebounceMillis must be non-negative." }
    }
}

data class MatchedRouteLocation(
    val originalLocation: GeoPoint,
    val snappedLocation: GeoPoint,
    val segmentIndex: Int,
    val segmentFraction: Double,
    val distanceFromRouteMeters: Double,
    val distanceFromStartMeters: Double,
) {
    init {
        require(segmentIndex >= 0) { "segmentIndex must be non-negative." }
        require(segmentFraction in 0.0..1.0) { "segmentFraction must be in [0, 1]." }
        require(distanceFromRouteMeters >= 0.0) { "distanceFromRouteMeters must be non-negative." }
        require(distanceFromStartMeters >= 0.0) { "distanceFromStartMeters must be non-negative." }
    }
}

data class NextInstruction(
    val instruction: String,
    val distanceMeters: Double,
    val roadName: String?,
    val maneuverType: String?,
    val maneuverModifier: String?,
    val sourceInstruction: RouteInstruction?,
) {
    init {
        require(distanceMeters >= 0.0) { "distanceMeters must be non-negative." }
    }
}

data class RouteProgress(
    val routeId: String,
    val matchedLocation: MatchedRouteLocation,
    val totalDistanceMeters: Double,
    val completedDistanceMeters: Double,
    val remainingDistanceMeters: Double,
    val remainingDurationSeconds: Double,
    val progressFraction: Double,
    val etaEpochMillis: Long?,
    val nextInstruction: NextInstruction?,
    val timestampMillis: Long,
) {
    init {
        require(totalDistanceMeters >= 0.0) { "totalDistanceMeters must be non-negative." }
        require(completedDistanceMeters >= 0.0) { "completedDistanceMeters must be non-negative." }
        require(remainingDistanceMeters >= 0.0) { "remainingDistanceMeters must be non-negative." }
        require(remainingDurationSeconds >= 0.0) { "remainingDurationSeconds must be non-negative." }
        require(progressFraction in 0.0..1.0) { "progressFraction must be in [0, 1]." }
    }

    val completedDistanceKm: Double get() = completedDistanceMeters / 1000.0
    val remainingDistanceKm: Double get() = remainingDistanceMeters / 1000.0
    val remainingMinutes: Double get() = remainingDurationSeconds / 60.0
}

sealed interface ProgressState {
    data object Idle : ProgressState
    data class Navigating(val progress: RouteProgress) : ProgressState
    data class OffRoute(val progress: RouteProgress, val distanceFromRouteMeters: Double) : ProgressState
    data class Rerouting(val lastKnownProgress: RouteProgress?) : ProgressState
    data class Arrived(val progress: RouteProgress) : ProgressState
}

interface RouteProgressRepository {
    fun calculateProgress(
        route: RouteAlternative,
        location: GeoPoint,
        speedMetersPerSecond: Double?,
        timestampMillis: Long,
    ): ProgressState
}

interface OffRouteDetector {
    fun isOffRoute(match: MatchedRouteLocation, config: RouteProgressConfig): Boolean
}

interface RouteMatcher {
    fun match(routePoints: List<GeoPoint>, location: GeoPoint): MatchedRouteLocation
}

interface InstructionTracker {
    fun nextInstruction(route: RouteAlternative, matchedDistanceFromStartMeters: Double): NextInstruction?
}

interface ProgressCalculator {
    fun calculate(
        route: RouteAlternative,
        match: MatchedRouteLocation,
        speedMetersPerSecond: Double?,
        timestampMillis: Long,
    ): RouteProgress
}

class ReroutingStrategy(
    private val debounceMillis: Long = RouteProgressConfig().rerouteDebounceMillis,
) {
    fun shouldReroute(
        firstOffRouteTimestampMillis: Long?,
        nowMillis: Long,
        isCurrentlyOffRoute: Boolean,
    ): Boolean {
        if (!isCurrentlyOffRoute || firstOffRouteTimestampMillis == null) return false
        return nowMillis - firstOffRouteTimestampMillis >= debounceMillis
    }
}
