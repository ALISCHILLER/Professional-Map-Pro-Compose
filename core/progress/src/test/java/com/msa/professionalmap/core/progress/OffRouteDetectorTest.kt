package com.msa.professionalmap.core.progress

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.data.DefaultRouteProgressRepository
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig
import org.junit.Assert.assertTrue
import org.junit.Test

class OffRouteDetectorTest {
    @Test
    fun `detects off route location beyond threshold`() {
        val route = RouteAlternative(
            id = "r1",
            title = "Test",
            summary = "Test route",
            points = listOf(GeoPoint(35.7000, 51.4000), GeoPoint(35.7000, 51.4100)),
            distanceMeters = 900.0,
            durationSeconds = 300.0,
            provider = "test",
        )
        val repo = DefaultRouteProgressRepository(config = RouteProgressConfig(offRouteThresholdMeters = 20.0))

        val state = repo.calculateProgress(route, GeoPoint(35.7020, 51.4050), 5.0, 1_000L)

        assertTrue(state is ProgressState.OffRoute)
    }
}
