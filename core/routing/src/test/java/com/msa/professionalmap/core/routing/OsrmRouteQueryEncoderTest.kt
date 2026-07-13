package com.msa.professionalmap.core.routing

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteSnapOptions
import com.msa.professionalmap.core.model.RoutingRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OsrmRouteQueryEncoderTest {
    @Test
    fun encodesOriginBearingAndRadiusForEveryWaypointSlot() {
        val request = RoutingRequest(
            origin = GeoPoint(35.70, 51.40),
            intermediateWaypoints = listOf(GeoPoint(35.71, 51.41)),
            destination = GeoPoint(35.72, 51.42),
            snapOptions = RouteSnapOptions(
                originBearingDegrees = 371.4,
                originBearingToleranceDegrees = 40,
                originRadiusMeters = 32.0,
                destinationRadiusMeters = 48.0,
            ),
        )

        assertEquals("11,40;;", OsrmRouteQueryEncoder.bearings(request))
        assertEquals("32;;48", OsrmRouteQueryEncoder.radiuses(request))
    }

    @Test
    fun omitsUnusedSnapHints() {
        val request = RoutingRequest(
            origin = GeoPoint(35.70, 51.40),
            destination = GeoPoint(35.72, 51.42),
        )

        assertNull(OsrmRouteQueryEncoder.bearings(request))
        assertNull(OsrmRouteQueryEncoder.radiuses(request))
    }
}
