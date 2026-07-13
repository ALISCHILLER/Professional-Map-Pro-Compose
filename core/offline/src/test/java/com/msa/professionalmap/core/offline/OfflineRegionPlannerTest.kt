package com.msa.professionalmap.core.offline

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.offline.domain.OfflineRegionPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineRegionPlannerTest {
    @Test
    fun createsBufferedRouteRequest() {
        val planner = OfflineRegionPlanner()
        val request = planner.createRoutePackRequest(
            routePoints = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.2)),
            styleId = "Liberty Style",
            styleTitle = "Liberty",
            styleUrl = "https://example.com/style.json",
        )

        assertTrue(request.clientId.matches(Regex("route-liberty-style-[0-9a-f]{16}")))
        assertTrue("Client id must not expose route coordinates", !request.clientId.contains("350000"))
        assertTrue("Client id must not expose route coordinates", !request.clientId.contains("510000"))
        assertEquals("Liberty route pack", request.title)
        assertTrue(request.bounds.southWest.latitude < 35.0)
        assertTrue(request.bounds.northEast.longitude > 51.2)
        assertTrue(request.minZoom <= request.maxZoom)
    }
}
