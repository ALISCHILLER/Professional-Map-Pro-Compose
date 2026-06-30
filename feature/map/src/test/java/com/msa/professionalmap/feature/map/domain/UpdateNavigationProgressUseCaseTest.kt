package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteNavigationPolicy
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress
import com.msa.professionalmap.core.progress.domain.RouteProgressRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNavigationProgressUseCaseTest {
    private val route = RouteAlternative(
        id = "route-1",
        title = "Route",
        summary = "Test route",
        points = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1), GeoPoint(35.2, 51.2)),
        distanceMeters = 2_000.0,
        durationSeconds = 120.0,
        provider = "test",
    )

    @Test
    fun `execute returns split navigation progress for accepted location`() {
        val useCase = createUseCase(progressState = ProgressState.Navigating(progressAt(segmentIndex = 1)))

        val update = useCase.execute(
            input = input(navigationActive = true),
            location = location(timestampMillis = 1_000L),
        )

        assertNotNull(update)
        requireNotNull(update)
        assertTrue(update.progressState is ProgressState.Navigating)
        assertEquals(3, update.split.completedPoints.size)
        assertEquals(2, update.split.remainingPoints.size)
        assertFalse(update.isFirstOffRouteEvent)
    }

    @Test
    fun `execute ignores updates while navigation is inactive`() {
        val useCase = createUseCase(progressState = ProgressState.Navigating(progressAt(segmentIndex = 0)))

        val update = useCase.execute(
            input = input(navigationActive = false),
            location = location(timestampMillis = 1_000L),
        )

        assertEquals(null, update)
    }

    @Test
    fun `execute marks first off route event once`() {
        val useCase = createUseCase(
            progressState = ProgressState.OffRoute(progressAt(segmentIndex = 0), distanceFromRouteMeters = 50.0),
        )

        val update = useCase.execute(
            input = input(navigationActive = true, firstOffRouteTimestampMillis = null),
            location = location(timestampMillis = 2_000L),
        )

        assertNotNull(update)
        requireNotNull(update)
        assertTrue(update.isFirstOffRouteEvent)
        assertEquals(2_000L, update.firstOffRouteTimestampMillis)
    }


    @Test
    fun `execute rejects preview-only route before calculating progress`() {
        val repository = TrackingProgressRepository(ProgressState.Navigating(progressAt(segmentIndex = 0)))
        val useCase = createUseCase(progressRepository = repository)

        val update = useCase.execute(
            input = input(
                navigationActive = true,
                selectedRoute = route.copy(navigationPolicy = RouteNavigationPolicy.PreviewOnly),
            ),
            location = location(timestampMillis = 1_000L),
        )

        assertEquals(null, update)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `execute throttles rapid location updates before calculating progress`() {
        val repository = TrackingProgressRepository(ProgressState.Navigating(progressAt(segmentIndex = 0)))
        val useCase = createUseCase(
            progressRepository = repository,
            config = MapFeatureConfig(progressThrottleMillis = 500L),
        )

        val first = useCase.execute(
            input = input(navigationActive = true),
            location = location(timestampMillis = 1_000L),
        )
        val second = useCase.execute(
            input = input(navigationActive = true),
            location = location(timestampMillis = 1_100L),
        )
        val third = useCase.execute(
            input = input(navigationActive = true),
            location = location(timestampMillis = 1_500L),
        )

        assertNotNull(first)
        assertEquals(null, second)
        assertNotNull(third)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `reset clears throttling state for the next location update`() {
        val repository = TrackingProgressRepository(ProgressState.Navigating(progressAt(segmentIndex = 0)))
        val useCase = createUseCase(
            progressRepository = repository,
            config = MapFeatureConfig(progressThrottleMillis = 500L),
        )

        assertNotNull(useCase.execute(input(navigationActive = true), location(timestampMillis = 1_000L)))
        assertEquals(null, useCase.execute(input(navigationActive = true), location(timestampMillis = 1_100L)))
        useCase.reset()
        assertNotNull(useCase.execute(input(navigationActive = true), location(timestampMillis = 1_100L)))

        assertEquals(2, repository.callCount)
    }

    @Test
    fun `execute does not repeat first off route event when timestamp already exists`() {
        val useCase = createUseCase(
            progressState = ProgressState.OffRoute(progressAt(segmentIndex = 0), distanceFromRouteMeters = 50.0),
        )

        val update = useCase.execute(
            input = input(navigationActive = true, firstOffRouteTimestampMillis = 1_500L),
            location = location(timestampMillis = 2_000L),
        )

        assertNotNull(update)
        requireNotNull(update)
        assertFalse(update.isFirstOffRouteEvent)
        assertEquals(null, update.firstOffRouteTimestampMillis)
    }

    private fun createUseCase(progressState: ProgressState): UpdateNavigationProgressUseCase =
        createUseCase(progressRepository = TrackingProgressRepository(progressState))

    private fun createUseCase(
        progressRepository: RouteProgressRepository,
        config: MapFeatureConfig = MapFeatureConfig(progressThrottleMillis = 0L),
    ): UpdateNavigationProgressUseCase = UpdateNavigationProgressUseCase(
        progressRepository = progressRepository,
        activeRouteResolver = ActiveRouteResolver(KotlinGeoEngine),
        routeSplitter = RouteProgressRouteSplitter(),
        arrivalGuard = ArrivalConfirmationGuard(config),
        progressUpdateThrottle = ProgressUpdateThrottle(config),
    )

    private fun input(
        navigationActive: Boolean,
        firstOffRouteTimestampMillis: Long? = null,
        selectedRoute: RouteAlternative? = route,
    ): NavigationProgressInput = NavigationProgressInput(
        navigationActive = navigationActive,
        selectedRoute = selectedRoute,
        routePoints = route.points,
        metrics = null,
        firstOffRouteTimestampMillis = firstOffRouteTimestampMillis,
    )

    private fun location(timestampMillis: Long): DeviceLocation = DeviceLocation(
        position = GeoPoint(35.15, 51.15),
        accuracyMeters = 5.0f,
        altitudeMeters = null,
        bearingDegrees = null,
        speedMetersPerSecond = 1.5f,
        timestampMillis = timestampMillis,
        provider = "test",
        isMock = false,
    )

    private fun progressAt(segmentIndex: Int): RouteProgress = RouteProgress(
        routeId = route.id,
        matchedLocation = MatchedRouteLocation(
            originalLocation = GeoPoint(35.15, 51.15),
            snappedLocation = GeoPoint(35.15, 51.15),
            segmentIndex = segmentIndex,
            segmentFraction = 0.5,
            distanceFromRouteMeters = 0.0,
            distanceFromStartMeters = 1_000.0,
        ),
        totalDistanceMeters = 2_000.0,
        completedDistanceMeters = 1_000.0,
        remainingDistanceMeters = 1_000.0,
        remainingDurationSeconds = 60.0,
        progressFraction = 0.5,
        etaEpochMillis = null,
        nextInstruction = null,
        timestampMillis = 1_000L,
    )

    private class TrackingProgressRepository(
        private val state: ProgressState,
    ) : RouteProgressRepository {
        var callCount: Int = 0
            private set

        override fun calculateProgress(
            route: RouteAlternative,
            location: GeoPoint,
            speedMetersPerSecond: Double?,
            timestampMillis: Long,
        ): ProgressState {
            callCount += 1
            return state
        }
    }
}
