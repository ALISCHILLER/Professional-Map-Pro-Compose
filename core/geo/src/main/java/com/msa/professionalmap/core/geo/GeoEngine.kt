package com.msa.professionalmap.core.geo

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteMetrics

/**
 * Platform-neutral geo math contract.
 *
 * Implementations may use native C++/JNI for performance or a pure Kotlin fallback for tests,
 * previews and platforms where the native library is unavailable.
 */
interface GeoEngine {
    fun distanceMeters(from: GeoPoint, to: GeoPoint): Double
    fun initialBearingDegrees(from: GeoPoint, to: GeoPoint): Double
    fun destinationPoint(from: GeoPoint, bearingDegrees: Double, distanceMeters: Double): GeoPoint
    fun routeLengthMeters(points: List<GeoPoint>): Double
    fun simplifyRoute(points: List<GeoPoint>, toleranceMeters: Double): List<GeoPoint>

    fun routeMetrics(points: List<GeoPoint>, simplified: List<GeoPoint>): RouteMetrics {
        require(points.size >= 2) { "A route needs at least two points." }
        return RouteMetrics(
            totalDistanceMeters = routeLengthMeters(points),
            directDistanceMeters = distanceMeters(points.first(), points.last()),
            initialBearingDegrees = initialBearingDegrees(points.first(), points.last()),
            simplifiedPointCount = simplified.size,
            originalPointCount = points.size,
        )
    }
}
