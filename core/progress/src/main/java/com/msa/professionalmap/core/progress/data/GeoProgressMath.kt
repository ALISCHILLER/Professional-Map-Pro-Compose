package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.model.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal object GeoProgressMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = Math.toRadians(b.latitude - a.latitude)
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(deltaLat / 2.0).pow(2.0) + cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2.0)
        return 2.0 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1.0 - h))
    }

    fun routeLengthMeters(points: List<GeoPoint>): Double = points.zipWithNext().sumOf { (a, b) -> distanceMeters(a, b) }

    fun bearingDegrees(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun angularDifferenceDegrees(first: Double, second: Double): Double {
        val difference = kotlin.math.abs((first - second) % 360.0)
        return if (difference > 180.0) 360.0 - difference else difference
    }

    fun interpolate(a: GeoPoint, b: GeoPoint, fraction: Double): GeoPoint {
        val t = fraction.coerceIn(0.0, 1.0)
        return GeoPoint(
            latitude = a.latitude + (b.latitude - a.latitude) * t,
            longitude = a.longitude + (b.longitude - a.longitude) * t,
        )
    }

    fun toLocalMeters(origin: GeoPoint, point: GeoPoint): LocalPoint {
        val latRadians = Math.toRadians(origin.latitude)
        val x = Math.toRadians(point.longitude - origin.longitude) * EARTH_RADIUS_METERS * cos(latRadians)
        val y = Math.toRadians(point.latitude - origin.latitude) * EARTH_RADIUS_METERS
        return LocalPoint(x = x, y = y)
    }

    data class LocalPoint(val x: Double, val y: Double)
}
