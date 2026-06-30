package com.msa.professionalmap.core.geo

import com.msa.professionalmap.core.model.GeoPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object KotlinGeoEngine : GeoEngine {
    private const val EarthRadiusMeters = 6_371_008.8

    override fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = from.latitude.toRadians()
        val lat2 = to.latitude.toRadians()
        val dLat = (to.latitude - from.latitude).toRadians()
        val dLon = (to.longitude - from.longitude).toRadians()
        val rawA = sin(dLat / 2).square() + cos(lat1) * cos(lat2) * sin(dLon / 2).square()
        val a = rawA.coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EarthRadiusMeters * c
    }

    override fun initialBearingDegrees(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = from.latitude.toRadians()
        val lat2 = to.latitude.toRadians()
        val dLon = (to.longitude - from.longitude).toRadians()
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (atan2(y, x).toDegrees() + 360.0) % 360.0
    }

    override fun destinationPoint(from: GeoPoint, bearingDegrees: Double, distanceMeters: Double): GeoPoint {
        require(bearingDegrees.isFinite()) { "Bearing must be finite." }
        require(distanceMeters.isFinite() && distanceMeters >= 0.0) { "Distance must be finite and non-negative." }
        val angularDistance = distanceMeters / EarthRadiusMeters
        val bearing = bearingDegrees.toRadians()
        val lat1 = from.latitude.toRadians()
        val lon1 = from.longitude.toRadians()
        val lat2 = asin(sin(lat1) * cos(angularDistance) + cos(lat1) * sin(angularDistance) * cos(bearing))
        val lon2 = lon1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2),
        )
        return GeoPoint(lat2.toDegrees(), normalizeLongitude(lon2.toDegrees()))
    }

    override fun routeLengthMeters(points: List<GeoPoint>): Double = points
        .zipWithNext()
        .sumOf { (a, b) -> distanceMeters(a, b) }

    override fun simplifyRoute(points: List<GeoPoint>, toleranceMeters: Double): List<GeoPoint> {
        require(toleranceMeters.isFinite() && toleranceMeters >= 0.0) { "Tolerance must be finite and non-negative." }
        if (points.size <= 2 || toleranceMeters <= 0.0) return points
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.lastIndex] = true
        simplifyRecursive(points, 0, points.lastIndex, toleranceMeters, keep)
        return points.filterIndexed { index, _ -> keep[index] }
    }

    private fun simplifyRecursive(points: List<GeoPoint>, start: Int, end: Int, tolerance: Double, keep: BooleanArray) {
        if (end <= start + 1) return
        var maxDistance = -1.0
        var maxIndex = start
        for (i in start + 1 until end) {
            val distance = crossTrackDistanceMeters(points[i], points[start], points[end])
            if (distance > maxDistance) {
                maxDistance = distance
                maxIndex = i
            }
        }
        if (maxDistance > tolerance) {
            keep[maxIndex] = true
            simplifyRecursive(points, start, maxIndex, tolerance, keep)
            simplifyRecursive(points, maxIndex, end, tolerance, keep)
        }
    }

    private fun crossTrackDistanceMeters(point: GeoPoint, start: GeoPoint, end: GeoPoint): Double {
        val d13 = distanceMeters(start, point) / EarthRadiusMeters
        val theta13 = initialBearingDegrees(start, point).toRadians()
        val theta12 = initialBearingDegrees(start, end).toRadians()
        return abs(asin(sin(d13) * sin(theta13 - theta12)) * EarthRadiusMeters)
    }

    private fun Double.square(): Double = this * this
    private fun Double.toRadians(): Double = this * PI / 180.0
    private fun Double.toDegrees(): Double = this * 180.0 / PI
    private fun normalizeLongitude(lon: Double): Double = ((lon + 540.0) % 360.0) - 180.0
}
