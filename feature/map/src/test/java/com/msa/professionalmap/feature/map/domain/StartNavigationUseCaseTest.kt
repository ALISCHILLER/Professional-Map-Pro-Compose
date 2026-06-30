package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteNavigationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartNavigationUseCaseTest {
    private val useCase = StartNavigationUseCase(ActiveRouteResolver(KotlinGeoEngine))
    private val route = RouteAlternative(
        id = "route-1",
        title = "Route",
        summary = "Provider route",
        points = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1)),
        distanceMeters = 1200.0,
        durationSeconds = 300.0,
        provider = "provider",
    )
    private val location = DeviceLocation(position = GeoPoint(35.0, 51.0), accuracyMeters = 5.0f)

    @Test
    fun `rejects preview-only fallback routes`() {
        val decision = useCase(
            StartNavigationInput(
                selectedRoute = route.copy(navigationPolicy = RouteNavigationPolicy.PreviewOnly),
                routePoints = emptyList(),
                metrics = null,
                currentLocation = location,
            )
        )

        assertEquals(StartNavigationDecision.MissingNavigableRoute, decision)
    }

    @Test
    fun `requires current location before navigation can start`() {
        val decision = useCase(
            StartNavigationInput(
                selectedRoute = route,
                routePoints = route.points,
                metrics = null,
                currentLocation = null,
            )
        )

        assertEquals(StartNavigationDecision.MissingCurrentLocation, decision)
    }

    @Test
    fun `returns route distance and current location when navigation can start`() {
        val decision = useCase(
            StartNavigationInput(
                selectedRoute = route,
                routePoints = route.points,
                metrics = null,
                currentLocation = location,
            )
        )

        assertTrue(decision is StartNavigationDecision.Ready)
        decision as StartNavigationDecision.Ready
        assertEquals(location, decision.currentLocation)
        assertEquals(route.distanceMeters, decision.routeDistanceMeters, 0.0)
    }
}
