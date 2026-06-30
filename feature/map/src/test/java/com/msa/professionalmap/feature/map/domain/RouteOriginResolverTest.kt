package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteOriginResolverTest {
    private val resolver = RouteOriginResolver()

    @Test
    fun `prefers current location when requested`() {
        val current = DeviceLocation(position = GeoPoint(10.0, 10.0), accuracyMeters = 5f)
        val referenceRoute = listOf(GeoPoint(1.0, 1.0))

        val origin = resolver.resolve(
            currentLocation = current,
            referenceRoutePoints = referenceRoute,
            currentRoutePoints = emptyList(),
            preferCurrentLocation = true,
        )

        assertEquals(current.position, origin)
    }

    @Test
    fun `falls back to reference route start when GPS is unavailable`() {
        val referenceStart = GeoPoint(35.0, 51.0)

        val origin = resolver.resolve(
            currentLocation = null,
            referenceRoutePoints = listOf(referenceStart),
            currentRoutePoints = emptyList(),
            preferCurrentLocation = true,
        )

        assertEquals(referenceStart, origin)
    }

    @Test
    fun `falls back to current route start when reference route is unavailable`() {
        val currentRouteStart = GeoPoint(36.0, 52.0)

        val origin = resolver.resolve(
            currentLocation = null,
            referenceRoutePoints = emptyList(),
            currentRoutePoints = listOf(currentRouteStart),
            preferCurrentLocation = false,
        )

        assertEquals(currentRouteStart, origin)
    }

    @Test
    fun `returns null when no origin source exists`() {
        val origin = resolver.resolve(
            currentLocation = null,
            referenceRoutePoints = emptyList(),
            currentRoutePoints = emptyList(),
            preferCurrentLocation = true,
        )

        assertNull(origin)
    }
}
