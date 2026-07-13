package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRoutePresentationUseCaseTest {
    private val useCase = BuildRoutePresentationUseCase(KotlinGeoEngine)

    @Test
    fun `returns null for invalid route geometry`() {
        assertNull(useCase(points = listOf(GeoPoint(35.0, 51.0)), simplificationToleranceMeters = 10.0))
    }

    @Test
    fun `builds simplified geometry and metrics for route points`() {
        val points = listOf(
            GeoPoint(35.0, 51.0),
            GeoPoint(35.001, 51.001),
            GeoPoint(35.01, 51.01),
        )

        val presentation = useCase(points = points, simplificationToleranceMeters = 5.0)

        assertNotNull(presentation)
        requireNotNull(presentation)
        assertEquals(points, presentation.routePoints)
        assertTrue(presentation.simplifiedRoutePoints.size in 2..points.size)
        assertEquals(points.size, presentation.metrics.originalPointCount)
        assertTrue(presentation.metrics.totalDistanceMeters > 0.0)
    }

    @Test
    fun `builds route presentation from alternative`() {
        val alternative = RouteAlternative(
            id = "route-1",
            title = "Primary",
            summary = "Two point route",
            points = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.01, 51.01)),
            distanceMeters = 1000.0,
            durationSeconds = 600.0,
            provider = "test",
        )

        val presentation = useCase.fromAlternative(alternative, simplificationToleranceMeters = 0.0)

        assertEquals(alternative.points, presentation.routePoints)
        assertEquals(2, presentation.metrics.originalPointCount)
    }

    @Test
    fun `measures selected point distance from route start`() {
        val route = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.01, 51.01))

        val distanceKm = useCase.distanceFromRouteStartKm(route, GeoPoint(35.005, 51.005))

        assertNotNull(distanceKm)
        requireNotNull(distanceKm)
        assertTrue(distanceKm > 0.0)
    }
}
