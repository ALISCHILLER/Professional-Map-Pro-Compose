package com.msa.professionalmap.core.progress

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.data.DefaultRouteProgressRepository
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteLocationSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedRouteProgressTest {
    @Test
    fun headingPrefersTheCorrectSideOfAParallelReturnRoute() {
        val route = route(
            listOf(
                GeoPoint(35.7000, 51.4000),
                GeoPoint(35.7000, 51.4100),
                GeoPoint(35.7002, 51.4100),
                GeoPoint(35.7002, 51.4000),
            )
        )
        val repository = DefaultRouteProgressRepository()
        val state = repository.calculateProgress(
            route = route,
            sample = RouteLocationSample(
                position = GeoPoint(35.7001, 51.4050),
                accuracyMeters = 6.0,
                bearingDegrees = 90.0,
                speedMetersPerSecond = 7.0,
            ),
            timestampMillis = 1_000L,
        )

        val match = (state as ProgressState.Navigating).progress.matchedLocation
        assertEquals(0, match.segmentIndex)
    }

    @Test
    fun poorAccuracyExpandsOffRouteThresholdWithinSafetyCap() {
        val route = route(listOf(GeoPoint(35.7000, 51.4000), GeoPoint(35.7000, 51.4200)))
        val repository = DefaultRouteProgressRepository()
        val state = repository.calculateProgress(
            route = route,
            sample = RouteLocationSample(
                position = GeoPoint(35.70035, 51.4100),
                accuracyMeters = 35.0,
                bearingDegrees = 90.0,
            ),
            timestampMillis = 2_000L,
        )

        assertTrue(state is ProgressState.Navigating)
    }

    private fun route(points: List<GeoPoint>) = RouteAlternative(
        id = "advanced-route",
        title = "Recommended",
        summary = "",
        points = points,
        distanceMeters = 2_000.0,
        durationSeconds = 300.0,
        provider = "test",
    )
}
