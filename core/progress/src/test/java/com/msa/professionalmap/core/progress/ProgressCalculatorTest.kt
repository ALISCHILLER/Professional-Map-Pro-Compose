package com.msa.professionalmap.core.progress

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.data.DefaultRouteProgressRepository
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCalculatorTest {
    @Test
    fun `calculates remaining distance and progress fraction`() {
        val route = RouteAlternative(
            id = "r1",
            title = "Test",
            summary = "Test route",
            points = listOf(
                GeoPoint(35.7000, 51.4000),
                GeoPoint(35.7000, 51.4100),
            ),
            distanceMeters = 900.0,
            durationSeconds = 300.0,
            provider = "test",
        )
        val repo = DefaultRouteProgressRepository()

        val state = repo.calculateProgress(
            route = route,
            location = GeoPoint(35.7000, 51.4050),
            speedMetersPerSecond = 8.0,
            timestampMillis = 1_000L,
        )

        val progress = (state as ProgressState.Navigating).progress
        assertTrue(progress.progressFraction in 0.45..0.55)
        assertTrue(progress.remainingDistanceMeters in 350.0..550.0)
        assertTrue(progress.etaEpochMillis != null)
    }

    @Test
    fun `marks route as arrived near destination`() {
        val route = RouteAlternative(
            id = "r1",
            title = "Test",
            summary = "Test route",
            points = listOf(GeoPoint(35.7000, 51.4000), GeoPoint(35.7000, 51.4100)),
            distanceMeters = 900.0,
            durationSeconds = 300.0,
            provider = "test",
        )
        val repo = DefaultRouteProgressRepository(config = RouteProgressConfig(arrivalThresholdMeters = 35.0))

        val state = repo.calculateProgress(route, GeoPoint(35.7000, 51.4100), 4.0, 1_000L)

        assertTrue(state is ProgressState.Arrived)
    }
}
