package com.msa.professionalmap.feature.map

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteNavigationPolicy
import com.msa.professionalmap.feature.map.presentation.MapUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapUiStateTest {
    @Test
    fun canStartNavigation_isFalseForPreviewOnlySelectedRoute() {
        val route = routeAlternative(RouteNavigationPolicy.PreviewOnly)
        val state = MapUiState(
            routePoints = route.points,
            currentLocation = deviceLocation(),
            routeAlternatives = listOf(route),
            selectedRouteAlternativeId = route.id,
        )

        assertFalse(state.canStartNavigation)
    }

    @Test
    fun canStartNavigation_isTrueForNavigableSelectedRouteWithLocation() {
        val route = routeAlternative(RouteNavigationPolicy.Navigable)
        val state = MapUiState(
            routePoints = route.points,
            currentLocation = deviceLocation(),
            routeAlternatives = listOf(route),
            selectedRouteAlternativeId = route.id,
        )

        assertTrue(state.canStartNavigation)
    }

    @Test
    fun canStartNavigation_allowsLocalReferenceRouteWhenNoAlternativeIsSelected() {
        val state = MapUiState(
            routePoints = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1)),
            currentLocation = deviceLocation(),
        )

        assertTrue(state.canStartNavigation)
    }

    @Test
    fun canStartNavigation_requiresCurrentLocation() {
        val route = routeAlternative(RouteNavigationPolicy.Navigable)
        val state = MapUiState(
            routePoints = route.points,
            routeAlternatives = listOf(route),
            selectedRouteAlternativeId = route.id,
        )

        assertFalse(state.canStartNavigation)
    }

    private fun routeAlternative(policy: RouteNavigationPolicy): RouteAlternative = RouteAlternative(
        id = "test-route",
        title = "Test route",
        summary = "Test summary",
        points = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1)),
        distanceMeters = 1000.0,
        durationSeconds = 600.0,
        provider = "test",
        navigationPolicy = policy,
    )

    private fun deviceLocation(): DeviceLocation = DeviceLocation(
        position = GeoPoint(35.0, 51.0),
        accuracyMeters = 5.0f,
        altitudeMeters = null,
        bearingDegrees = null,
        speedMetersPerSecond = null,
        timestampMillis = 1L,
        provider = "test",
        isMock = false,
    )
}
