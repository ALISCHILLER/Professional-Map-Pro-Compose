package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.domain.InstructionTracker
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.ProgressCalculator
import com.msa.professionalmap.core.progress.domain.RouteProgress
import kotlin.math.max

class DefaultProgressCalculator(
    private val instructionTracker: InstructionTracker = RouteInstructionTracker(),
) : ProgressCalculator {
    override fun calculate(
        route: RouteAlternative,
        match: MatchedRouteLocation,
        speedMetersPerSecond: Double?,
        timestampMillis: Long,
    ): RouteProgress {
        val geometryDistance = GeoProgressMath.routeLengthMeters(route.points)
        val totalDistance = when {
            route.distanceMeters > 0.0 -> route.distanceMeters
            geometryDistance > 0.0 -> geometryDistance
            else -> 0.0
        }
        val completed = match.distanceFromStartMeters.coerceIn(0.0, totalDistance)
        val remaining = (totalDistance - completed).coerceAtLeast(0.0)
        val progressFraction = if (totalDistance <= 0.0) 1.0 else (completed / totalDistance).coerceIn(0.0, 1.0)

        val providerDuration = if (route.durationSeconds > 0.0 && totalDistance > 0.0) {
            route.durationSeconds * (remaining / totalDistance)
        } else {
            0.0
        }
        val speedBasedDuration = speedMetersPerSecond
            ?.takeIf { it.isFinite() && it > 1.2 }
            ?.let { remaining / max(it, 0.1) }

        val remainingDuration = when {
            providerDuration > 0.0 && speedBasedDuration != null -> providerDuration * 0.65 + speedBasedDuration * 0.35
            providerDuration > 0.0 -> providerDuration
            speedBasedDuration != null -> speedBasedDuration
            else -> 0.0
        }

        return RouteProgress(
            routeId = route.id,
            matchedLocation = match,
            totalDistanceMeters = totalDistance,
            completedDistanceMeters = completed,
            remainingDistanceMeters = remaining,
            remainingDurationSeconds = remainingDuration,
            progressFraction = progressFraction,
            etaEpochMillis = if (remainingDuration > 0.0) timestampMillis + (remainingDuration * 1000.0).toLong() else null,
            nextInstruction = instructionTracker.nextInstruction(route, completed),
            timestampMillis = timestampMillis,
        )
    }
}
