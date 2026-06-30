package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.NextInstruction
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildNavigationSnapshotUseCaseTest {
    @Test
    fun invoke_usesNextInstructionInstructionText() {
        val snapshot = BuildNavigationSnapshotUseCase().invoke(
            destination = null,
            routePoints = routePoints,
            metrics = null,
            progressState = ProgressState.Navigating(progress("Turn right")),
            selectedRoute = route,
            lastMessage = "Fallback message",
            nowMillis = 1_000L,
        )

        assertEquals("Turn right", snapshot.nextInstructionText)
        assertEquals("en", snapshot.languageTag)
        assertEquals(NavigationServiceStatus.Active, snapshot.status)
        assertEquals(1_000L, snapshot.lastUpdatedAtMillis)
    }

    @Test
    fun invoke_prefersInstructionOverrideForForegroundService() {
        val snapshot = BuildNavigationSnapshotUseCase().invoke(
            destination = null,
            routePoints = routePoints,
            metrics = null,
            progressState = ProgressState.Navigating(progress("Turn right")),
            selectedRoute = route,
            lastMessage = "Fallback message",
            instructionOverride = "Recalculating route",
            language = GuidanceLanguage.Persian,
        )

        assertEquals("Recalculating route", snapshot.nextInstructionText)
        assertEquals("fa", snapshot.languageTag)
    }


    @Test
    fun invoke_allowsExplicitIdleStatusForStoppedServiceSnapshots() {
        val snapshot = BuildNavigationSnapshotUseCase().invoke(
            destination = null,
            routePoints = routePoints,
            metrics = null,
            progressState = ProgressState.Idle,
            selectedRoute = route,
            lastMessage = null,
            status = NavigationServiceStatus.Idle,
        )

        assertEquals(NavigationServiceStatus.Idle, snapshot.status)
    }

    private fun progress(instruction: String): RouteProgress = RouteProgress(
        routeId = route.id,
        matchedLocation = MatchedRouteLocation(
            originalLocation = routePoints.first(),
            snappedLocation = routePoints.first(),
            segmentIndex = 0,
            segmentFraction = 0.0,
            distanceFromRouteMeters = 0.0,
            distanceFromStartMeters = 0.0,
        ),
        totalDistanceMeters = route.distanceMeters,
        completedDistanceMeters = 0.0,
        remainingDistanceMeters = route.distanceMeters,
        remainingDurationSeconds = route.durationSeconds,
        progressFraction = 0.0,
        etaEpochMillis = null,
        nextInstruction = NextInstruction(
            instruction = instruction,
            distanceMeters = 50.0,
            roadName = null,
            maneuverType = "turn",
            maneuverModifier = "right",
            sourceInstruction = null,
        ),
        timestampMillis = 1_000L,
    )

    private companion object {
        val routePoints = listOf(
            GeoPoint(latitude = 35.7000, longitude = 51.4000),
            GeoPoint(latitude = 35.7100, longitude = 51.4100),
        )
        val route = RouteAlternative(
            id = "route-1",
            title = "Primary",
            summary = "Test route",
            points = routePoints,
            distanceMeters = 1_000.0,
            durationSeconds = 300.0,
            provider = "test",
        )
    }
}
