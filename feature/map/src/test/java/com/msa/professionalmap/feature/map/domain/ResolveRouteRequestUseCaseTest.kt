package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveRouteRequestUseCaseTest {
    private val useCase = ResolveRouteRequestUseCase()
    private val destination = GeoPoint(35.2, 51.2)

    @Test
    fun `returns missing destination when selected point is absent`() {
        val decision = useCase(
            RouteRequestInput(
                selectedDestination = null,
                currentLocation = null,
                referenceRoutePoints = listOf(GeoPoint(35.0, 51.0)),
                currentRoutePoints = emptyList(),
                preferCurrentLocation = true,
            )
        )

        assertEquals(RouteRequestDecision.MissingDestination, decision)
    }

    @Test
    fun `returns missing origin when no origin source exists`() {
        val decision = useCase(
            RouteRequestInput(
                selectedDestination = destination,
                currentLocation = null,
                referenceRoutePoints = emptyList(),
                currentRoutePoints = emptyList(),
                preferCurrentLocation = true,
            )
        )

        assertEquals(RouteRequestDecision.MissingOrigin, decision)
    }

    @Test
    fun `returns current location origin when preferred and available`() {
        val currentLocation = DeviceLocation(position = GeoPoint(35.1, 51.1), accuracyMeters = 5.0f)

        val decision = useCase(
            RouteRequestInput(
                selectedDestination = destination,
                currentLocation = currentLocation,
                referenceRoutePoints = listOf(GeoPoint(35.0, 51.0)),
                currentRoutePoints = emptyList(),
                preferCurrentLocation = true,
            )
        )

        assertTrue(decision is RouteRequestDecision.Ready)
        decision as RouteRequestDecision.Ready
        assertEquals(currentLocation.position, decision.origin)
        assertEquals(destination, decision.destination)
    }
}
