package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteNavigationPolicy
import com.msa.professionalmap.core.model.RoutingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplyRouteCalculationOutcomeUseCaseTest {
    private val useCase = ApplyRouteCalculationOutcomeUseCase(BuildRoutePresentationUseCase(KotlinGeoEngine))
    private val route = RouteAlternative(
        id = "route-1",
        title = "Primary",
        summary = "Provider route",
        points = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1)),
        distanceMeters = 1000.0,
        durationSeconds = 300.0,
        provider = "provider",
    )
    private val fallback = route.copy(
        id = "fallback",
        title = "Fallback",
        provider = "local-fallback",
        navigationPolicy = RouteNavigationPolicy.PreviewOnly,
    )
    private val failure = IllegalStateException("provider unavailable")

    @Test
    fun `applies provider route with presentation data`() {
        val result = useCase(
            RouteCalculationApplicationInput(
                outcome = RouteCalculationOutcome.ProviderRoute(RoutingResult(listOf(route), provider = "provider")),
                simplificationToleranceMeters = 0.0,
                allowPreviewFallback = true,
            )
        )

        assertTrue(result is RouteCalculationApplicationResult.ProviderRouteApplied)
        result as RouteCalculationApplicationResult.ProviderRouteApplied
        assertEquals(route.id, result.selectedRoute.id)
        assertEquals(route.points, result.presentation.routePoints)
    }

    @Test
    fun `allows preview fallback for planning requests`() {
        val result = useCase(
            RouteCalculationApplicationInput(
                outcome = RouteCalculationOutcome.FallbackRoute(fallback, failure),
                simplificationToleranceMeters = 0.0,
                allowPreviewFallback = true,
            )
        )

        assertTrue(result is RouteCalculationApplicationResult.FallbackPreviewApplied)
        result as RouteCalculationApplicationResult.FallbackPreviewApplied
        assertEquals(fallback.id, result.route.id)
        assertEquals(RouteNavigationPolicy.PreviewOnly, result.route.navigationPolicy)
    }

    @Test
    fun `rejects preview fallback while active navigation is rerouting`() {
        val result = useCase(
            RouteCalculationApplicationInput(
                outcome = RouteCalculationOutcome.FallbackRoute(fallback, failure),
                simplificationToleranceMeters = 0.0,
                allowPreviewFallback = false,
            )
        )

        assertTrue(result is RouteCalculationApplicationResult.ProviderFailedDuringActiveNavigation)
    }
}
