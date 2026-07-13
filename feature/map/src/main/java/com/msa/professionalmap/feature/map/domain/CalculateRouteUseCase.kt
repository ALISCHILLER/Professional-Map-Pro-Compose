package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.GeoEngine
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteNavigationPolicy
import com.msa.professionalmap.core.model.RouteOverview
import com.msa.professionalmap.core.model.RouteSnapOptions
import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import com.msa.professionalmap.core.routing.RoutingRepository
import kotlinx.coroutines.CancellationException

/**
 * Calculates a route through the configured provider and returns a deterministic local preview when
 * provider routing fails.
 *
 * Provider failures are intentionally converted into a preview-only fallback so the map can still
 * show origin/destination context. Coroutine cancellation is never converted into a fallback;
 * cancellation belongs to structured concurrency and must propagate to the caller.
 */
class CalculateRouteUseCase(
    private val routingRepository: RoutingRepository,
    private val geoEngine: GeoEngine,
) {
    suspend operator fun invoke(
        origin: GeoPoint,
        destination: GeoPoint,
        requestAlternatives: Boolean = true,
        includeSteps: Boolean = true,
        overview: RouteOverview = RouteOverview.Full,
        originLocation: DeviceLocation? = null,
    ): RouteCalculationOutcome {
        return try {
            RouteCalculationOutcome.ProviderRoute(
                routingRepository.calculateRoute(
                    RoutingRequest(
                        origin = origin,
                        destination = destination,
                        requestAlternatives = requestAlternatives,
                        includeSteps = includeSteps,
                        overview = overview,
                        snapOptions = originLocation.toSnapOptions(),
                    )
                )
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            RouteCalculationOutcome.FallbackRoute(
                route = fallbackRoute(origin, destination),
                cause = throwable,
            )
        }
    }

    private fun DeviceLocation?.toSnapOptions(): RouteSnapOptions {
        this ?: return RouteSnapOptions()
        val radius = ((accuracyMeters?.toDouble() ?: DefaultAccuracyMeters) * AccuracyRadiusMultiplier)
            .coerceIn(MinimumSnapRadiusMeters, MaximumSnapRadiusMeters)
        return RouteSnapOptions(
            originBearingDegrees = bearingDegrees?.toDouble(),
            originBearingToleranceDegrees = if ((speedMetersPerSecond ?: 0f) >= ReliableBearingSpeed) 35 else 65,
            originRadiusMeters = radius,
            destinationRadiusMeters = DestinationRadiusMeters,
            continueStraight = true,
        )
    }

    private fun fallbackRoute(origin: GeoPoint, destination: GeoPoint): RouteAlternative {
        val points = listOf(origin, destination)
        val distance = geoEngine.distanceMeters(origin, destination)
        val duration = if (distance > 0.0) distance / CITY_FALLBACK_SPEED_METERS_PER_SECOND else 0.0
        return RouteAlternative(
            id = "fallback-${origin.hashCode()}-${destination.hashCode()}",
            title = "Straight-line fallback preview",
            summary = "Provider route unavailable",
            points = points,
            distanceMeters = distance,
            durationSeconds = duration,
            provider = "local-fallback",
            navigationPolicy = RouteNavigationPolicy.PreviewOnly,
        )
    }

    private companion object {
        private const val CITY_FALLBACK_SPEED_METERS_PER_SECOND = 13.8
        private const val DefaultAccuracyMeters = 18.0
        private const val AccuracyRadiusMultiplier = 1.8
        private const val MinimumSnapRadiusMeters = 20.0
        private const val MaximumSnapRadiusMeters = 90.0
        private const val DestinationRadiusMeters = 45.0
        private const val ReliableBearingSpeed = 2.0f
    }
}

sealed interface RouteCalculationOutcome {
    data class ProviderRoute(val result: RoutingResult) : RouteCalculationOutcome
    data class FallbackRoute(val route: RouteAlternative, val cause: Throwable) : RouteCalculationOutcome
}
