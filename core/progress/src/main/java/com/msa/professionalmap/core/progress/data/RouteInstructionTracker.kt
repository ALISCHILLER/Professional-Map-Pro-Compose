package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteInstruction
import com.msa.professionalmap.core.progress.domain.InstructionTracker
import com.msa.professionalmap.core.progress.domain.NextInstruction
import com.msa.professionalmap.core.progress.domain.RouteMatcher

class RouteInstructionTracker(
    private val matcher: RouteMatcher = ProjectionRouteMatcher(),
    private val instructionLookAheadMeters: Double = 8.0,
) : InstructionTracker {
    override fun nextInstruction(route: RouteAlternative, matchedDistanceFromStartMeters: Double): NextInstruction? {
        val instructions = route.legs.flatMap { it.steps }
        if (instructions.isEmpty()) {
            val totalDistance = if (route.distanceMeters > 0.0) route.distanceMeters else GeoProgressMath.routeLengthMeters(route.points)
            val remaining = (totalDistance - matchedDistanceFromStartMeters).coerceAtLeast(0.0)
            return NextInstruction(
                instruction = if (remaining <= 25.0) "Arrive at destination" else "Continue to destination",
                distanceMeters = remaining,
                roadName = null,
                maneuverType = if (remaining <= 25.0) "arrive" else "continue",
                maneuverModifier = null,
                sourceInstruction = null,
            )
        }

        val indexed = instructions.mapNotNull { instruction ->
            val anchor = instruction.location ?: instruction.geometry.firstOrNull() ?: return@mapNotNull null
            val anchorDistance = runCatching { matcher.match(route.points, anchor).distanceFromStartMeters }.getOrNull()
                ?: return@mapNotNull null
            instruction to anchorDistance
        }.sortedBy { it.second }

        val next = indexed.firstOrNull { (_, distanceOnRoute) ->
            distanceOnRoute + instructionLookAheadMeters >= matchedDistanceFromStartMeters
        } ?: indexed.lastOrNull()

        return next?.let { (instruction, distanceOnRoute) ->
            instruction.toNextInstruction(
                distanceMeters = (distanceOnRoute - matchedDistanceFromStartMeters).coerceAtLeast(0.0),
            )
        }
    }

    private fun RouteInstruction.toNextInstruction(distanceMeters: Double): NextInstruction {
        val readableInstruction = instruction.ifBlank {
            buildString {
                append(maneuverType ?: "Continue")
                maneuverModifier?.let { append(" ").append(it) }
                roadName?.takeIf { it.isNotBlank() }?.let { append(" onto ").append(it) }
            }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        return NextInstruction(
            instruction = readableInstruction,
            distanceMeters = distanceMeters,
            roadName = roadName,
            maneuverType = maneuverType,
            maneuverModifier = maneuverModifier,
            sourceInstruction = this,
        )
    }
}
