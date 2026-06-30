package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteNavigationPolicy
import com.msa.professionalmap.core.model.RouteOverview
import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import com.msa.professionalmap.core.routing.RoutingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateRouteUseCaseTest {
    private val origin = GeoPoint(latitude = 35.0, longitude = 51.0)
    private val destination = GeoPoint(latitude = 35.1, longitude = 51.1)

    @Test
    fun `returns provider route when repository succeeds`() = runBlocking {
        val useCase = CalculateRouteUseCase(
            routingRepository = SuccessfulRoutingRepository,
            geoEngine = KotlinGeoEngine,
        )

        val outcome = useCase(origin, destination)

        assertTrue(outcome is RouteCalculationOutcome.ProviderRoute)
        assertEquals("test-provider", (outcome as RouteCalculationOutcome.ProviderRoute).result.provider)
        assertTrue(outcome.result.primaryRoute.isNavigationEligible)
    }

    @Test
    fun `returns deterministic preview fallback when repository fails`() = runBlocking {
        val useCase = CalculateRouteUseCase(
            routingRepository = FailingRoutingRepository,
            geoEngine = KotlinGeoEngine,
        )

        val outcome = useCase(origin, destination)

        assertTrue(outcome is RouteCalculationOutcome.FallbackRoute)
        val fallback = (outcome as RouteCalculationOutcome.FallbackRoute).route
        assertEquals("local-fallback", fallback.provider)
        assertEquals(RouteNavigationPolicy.PreviewOnly, fallback.navigationPolicy)
        assertFalse(fallback.isNavigationEligible)
        assertEquals(listOf(origin, destination), fallback.points)
        assertTrue(fallback.distanceMeters > 0.0)
    }


    @Test
    fun `forwards routing request options to repository`() = runBlocking {
        val repository = CapturingRoutingRepository()
        val useCase = CalculateRouteUseCase(
            routingRepository = repository,
            geoEngine = KotlinGeoEngine,
        )

        useCase(
            origin = origin,
            destination = destination,
            requestAlternatives = false,
            includeSteps = false,
            overview = RouteOverview.Simplified,
        )

        val request = requireNotNull(repository.lastRequest)
        assertEquals(origin, request.origin)
        assertEquals(destination, request.destination)
        assertFalse(request.requestAlternatives)
        assertFalse(request.includeSteps)
        assertEquals(RouteOverview.Simplified, request.overview)
    }

    @Test(expected = CancellationException::class)
    fun `propagates cancellation instead of converting it to fallback`() = runBlocking {
        val useCase = CalculateRouteUseCase(
            routingRepository = CancellingRoutingRepository,
            geoEngine = KotlinGeoEngine,
        )

        useCase(origin, destination)
    }



    private class CapturingRoutingRepository : RoutingRepository {
        var lastRequest: RoutingRequest? = null
            private set

        override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
            lastRequest = request
            return RoutingResult(
                routes = listOf(
                    RouteAlternative(
                        id = "captured",
                        title = "Captured",
                        summary = "Captured request",
                        points = listOf(request.origin, request.destination),
                        distanceMeters = 1000.0,
                        durationSeconds = 120.0,
                        provider = "capture-provider",
                    )
                ),
                provider = "capture-provider",
            )
        }
    }

    private object SuccessfulRoutingRepository : RoutingRepository {
        override suspend fun calculateRoute(request: RoutingRequest): RoutingResult = RoutingResult(
            routes = listOf(
                RouteAlternative(
                    id = "primary",
                    title = "Primary",
                    summary = "Test route",
                    points = listOf(request.origin, request.destination),
                    distanceMeters = 1000.0,
                    durationSeconds = 120.0,
                    provider = "test-provider",
                )
            ),
            provider = "test-provider",
        )
    }

    private object FailingRoutingRepository : RoutingRepository {
        override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
            error("network failed")
        }
    }

    private object CancellingRoutingRepository : RoutingRepository {
        override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
            throw CancellationException("route request replaced")
        }
    }
}
