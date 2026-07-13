package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint

/**
 * Resolves a user route-request intent into provider-ready coordinates.
 *
 * UI state contains several possible origin sources: current GPS, the immutable reference route,
 * and the currently rendered route. Keeping that fallback policy in a use case makes route-request
 * decisions deterministic and keeps presentation controllers from duplicating origin rules.
 */
class ResolveRouteRequestUseCase(
    private val originResolver: RouteOriginResolver = RouteOriginResolver(),
) {
    operator fun invoke(input: RouteRequestInput): RouteRequestDecision {
        val destination = input.selectedDestination ?: return RouteRequestDecision.MissingDestination
        val origin = originResolver.resolve(
            currentLocation = input.currentLocation,
            referenceRoutePoints = input.referenceRoutePoints,
            currentRoutePoints = input.currentRoutePoints,
            preferCurrentLocation = input.preferCurrentLocation,
        ) ?: return RouteRequestDecision.MissingOrigin
        return RouteRequestDecision.Ready(
            origin = origin,
            destination = destination,
            originLocation = input.currentLocation?.takeIf {
                input.preferCurrentLocation && it.position == origin
            },
        )
    }
}

data class RouteRequestInput(
    val selectedDestination: GeoPoint?,
    val currentLocation: DeviceLocation?,
    val referenceRoutePoints: List<GeoPoint>,
    val currentRoutePoints: List<GeoPoint>,
    val preferCurrentLocation: Boolean,
)

sealed interface RouteRequestDecision {
    data object MissingDestination : RouteRequestDecision
    data object MissingOrigin : RouteRequestDecision
    data class Ready(
        val origin: GeoPoint,
        val destination: GeoPoint,
        val originLocation: DeviceLocation? = null,
    ) : RouteRequestDecision
}
