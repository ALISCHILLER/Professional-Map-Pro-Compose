package com.msa.professionalmap.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteAlternativeTest {
    private val origin = GeoPoint(latitude = 35.0, longitude = 51.0)
    private val destination = GeoPoint(latitude = 35.1, longitude = 51.1)

    @Test
    fun `provider route is navigation eligible by default`() {
        val route = routeAlternative()

        assertTrue(route.isNavigationEligible)
    }

    @Test
    fun `preview only route is not navigation eligible`() {
        val route = routeAlternative(navigationPolicy = RouteNavigationPolicy.PreviewOnly)

        assertFalse(route.isNavigationEligible)
    }

    private fun routeAlternative(
        navigationPolicy: RouteNavigationPolicy = RouteNavigationPolicy.Navigable,
    ): RouteAlternative = RouteAlternative(
        id = "route-1",
        title = "Route",
        summary = "Summary",
        points = listOf(origin, destination),
        distanceMeters = 1000.0,
        durationSeconds = 120.0,
        provider = "test-provider",
        navigationPolicy = navigationPolicy,
    )
}
