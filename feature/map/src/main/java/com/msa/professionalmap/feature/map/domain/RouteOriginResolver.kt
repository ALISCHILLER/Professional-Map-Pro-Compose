package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint

/**
 * Resolves the origin used for route requests.
 *
 * The policy is intentionally kept out of MapViewModel so routing behavior can be
 * changed or tested without touching presentation orchestration.
 */
class RouteOriginResolver {
    fun resolve(
        currentLocation: DeviceLocation?,
        referenceRoutePoints: List<GeoPoint>,
        currentRoutePoints: List<GeoPoint>,
        preferCurrentLocation: Boolean,
    ): GeoPoint? {
        if (preferCurrentLocation) {
            currentLocation?.position?.let { return it }
        }
        return referenceRoutePoints.firstOrNull()
            ?: currentRoutePoints.firstOrNull()
    }
}
