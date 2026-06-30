package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.RouteProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteProgressRouteSplitterTest {
    private val splitter = RouteProgressRouteSplitter()

    @Test
    fun `splits route at snapped progress point`() {
        val route = listOf(
            GeoPoint(35.0, 51.0),
            GeoPoint(35.1, 51.1),
            GeoPoint(35.2, 51.2),
        )
        val snapped = GeoPoint(35.15, 51.15)
        val progress = RouteProgress(
            routeId = "route-1",
            matchedLocation = MatchedRouteLocation(
                originalLocation = snapped,
                snappedLocation = snapped,
                segmentIndex = 1,
                segmentFraction = 0.5,
                distanceFromRouteMeters = 0.0,
                distanceFromStartMeters = 1000.0,
            ),
            totalDistanceMeters = 2000.0,
            completedDistanceMeters = 1000.0,
            remainingDistanceMeters = 1000.0,
            remainingDurationSeconds = 60.0,
            progressFraction = 0.5,
            etaEpochMillis = null,
            nextInstruction = null,
            timestampMillis = 1_000L,
        )

        val split = splitter.split(route, progress)

        assertEquals(listOf(route[0], route[1], snapped), split.completedPoints)
        assertEquals(listOf(snapped, route[2]), split.remainingPoints)
    }

    @Test
    fun `returns original route as remaining when progress is absent`() {
        val route = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1))

        val split = splitter.split(route, progress = null)

        assertEquals(emptyList<GeoPoint>(), split.completedPoints)
        assertEquals(route, split.remainingPoints)
    }
}
