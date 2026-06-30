package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteNavigationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveRouteResolverTest {
    private val resolver = ActiveRouteResolver(KotlinGeoEngine)

    @Test
    fun `returns selected provider route when available`() {
        val selected = routeAlternative()

        assertEquals(selected, resolver.resolve(selected, emptyList(), metrics = null))
    }

    @Test
    fun `rejects selected preview route for navigation progress`() {
        val selected = routeAlternative(navigationPolicy = RouteNavigationPolicy.PreviewOnly)

        assertNull(resolver.resolve(selected, emptyList(), metrics = null))
    }

    @Test
    fun `synthesizes local active route when provider route is absent`() {
        val route = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1))

        val resolved = resolver.resolve(selectedRoute = null, routePoints = route, metrics = null)

        assertEquals("active-route", resolved?.id)
        assertEquals("local", resolved?.provider)
        assertEquals(route, resolved?.points)
    }

    @Test
    fun `returns null when no route geometry exists`() {
        assertNull(resolver.resolve(selectedRoute = null, routePoints = listOf(GeoPoint(35.0, 51.0)), metrics = null))
    }

    private fun routeAlternative(
        navigationPolicy: RouteNavigationPolicy = RouteNavigationPolicy.Navigable,
    ): RouteAlternative = RouteAlternative(
        id = "provider-1",
        title = "Provider route",
        summary = "Fastest",
        points = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1)),
        distanceMeters = 1000.0,
        durationSeconds = 120.0,
        provider = "test",
        navigationPolicy = navigationPolicy,
    )
}
