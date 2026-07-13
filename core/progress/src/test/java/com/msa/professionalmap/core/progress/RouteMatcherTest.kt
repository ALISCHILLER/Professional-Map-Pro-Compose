package com.msa.professionalmap.core.progress

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.progress.data.ProjectionRouteMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMatcherTest {
    private val matcher = ProjectionRouteMatcher()

    @Test
    fun `projects point onto nearest route segment`() {
        val route = listOf(
            GeoPoint(35.7000, 51.4000),
            GeoPoint(35.7000, 51.4100),
            GeoPoint(35.7100, 51.4100),
        )
        val location = GeoPoint(35.7004, 51.4050)

        val match = matcher.match(route, location)

        assertEquals(0, match.segmentIndex)
        assertTrue(match.segmentFraction in 0.45..0.55)
        assertTrue(match.distanceFromRouteMeters < 60.0)
    }

    @Test
    fun `matches to later segment when location is closer there`() {
        val route = listOf(
            GeoPoint(35.7000, 51.4000),
            GeoPoint(35.7000, 51.4100),
            GeoPoint(35.7100, 51.4100),
        )
        val location = GeoPoint(35.7060, 51.4102)

        val match = matcher.match(route, location)

        assertEquals(1, match.segmentIndex)
        assertTrue(match.distanceFromStartMeters > 800.0)
    }
}
