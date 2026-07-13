package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Navigation-aware camera policy with look-ahead framing and smoothed bearing. */
internal class MapCameraController {
    private var fittedRouteIdentity: String? = null
    private var lastFollowedTimestamp: Long? = null
    private var lastBearing: Double? = null

    fun reset() {
        fittedRouteIdentity = null
        lastFollowedTimestamp = null
        lastBearing = null
    }

    fun update(map: MapLibreMap, scene: MapScene) {
        if (scene.followUserLocation && scene.currentLocation != null) {
            follow(map, scene)
            return
        }
        if (scene.activeRoutePoints.size >= 2 && fittedRouteIdentity != scene.routeIdentity) {
            fittedRouteIdentity = scene.routeIdentity
            fitRoute(map, scene.selectedRoute?.points ?: scene.activeRoutePoints)
        }
    }

    private fun follow(map: MapLibreMap, scene: MapScene) {
        val location = checkNotNull(scene.currentLocation)
        val previousTimestamp = lastFollowedTimestamp
        if (previousTimestamp == location.timestampMillis) return
        if (previousTimestamp != null && location.timestampMillis - previousTimestamp < MinimumCameraIntervalMillis) return
        lastFollowedTimestamp = location.timestampMillis
        val routeBearing = scene.remainingRoutePoints.takeIf { it.size >= 2 }
            ?.let { bearingDegrees(it[0], it[1]) }
        val bearing = smoothBearing(
            candidate = location.bearingDegrees?.toDouble() ?: routeBearing,
            fallback = map.cameraPosition.bearing,
        )
        val speed = location.speedMetersPerSecond?.toDouble() ?: 0.0
        val target = if (scene.navigationActive) {
            lookAheadPoint(scene.remainingRoutePoints, lookAheadMeters(speed))
                ?: scene.snappedLocation
                ?: location.position
        } else {
            location.position
        }
        val position = CameraPosition.Builder()
            .target(LatLng(target.latitude, target.longitude))
            .zoom(if (scene.navigationActive) navigationZoom(speed) else map.cameraPosition.zoom.coerceAtLeast(15.5))
            .bearing(if (scene.navigationActive) bearing else map.cameraPosition.bearing)
            .tilt(if (scene.navigationActive) 50.0 else 0.0)
            .build()
        map.easeCamera(
            CameraUpdateFactory.newCameraPosition(position),
            if (scene.navigationActive) NavigationAnimationMillis else FollowAnimationMillis,
        )
    }

    private fun lookAheadPoint(points: List<GeoPoint>, targetMeters: Double): GeoPoint? {
        if (points.isEmpty()) return null
        var traveled = 0.0
        for (index in 0 until points.lastIndex) {
            val start = points[index]
            val end = points[index + 1]
            val segment = distanceMeters(start, end)
            if (segment > 0.0 && traveled + segment >= targetMeters) {
                val fraction = ((targetMeters - traveled) / segment).coerceIn(0.0, 1.0)
                return GeoPoint(
                    latitude = start.latitude + (end.latitude - start.latitude) * fraction,
                    longitude = start.longitude + (end.longitude - start.longitude) * fraction,
                )
            }
            traveled += segment
        }
        return points.last()
    }

    private fun smoothBearing(candidate: Double?, fallback: Double): Double {
        val normalized = candidate?.takeIf(Double::isFinite)?.normalizeBearing() ?: return fallback
        val previous = lastBearing
        if (previous == null) {
            lastBearing = normalized
            return normalized
        }
        var delta = normalized - previous
        if (delta > 180.0) delta -= 360.0
        if (delta < -180.0) delta += 360.0
        val result = if (abs(delta) < 2.5) previous else (previous + delta * BearingSmoothing).normalizeBearing()
        lastBearing = result
        return result
    }

    private fun fitRoute(map: MapLibreMap, points: List<GeoPoint>) {
        if (points.size < 2) return
        val bounds = LatLngBounds.Builder().apply {
            points.forEach { include(LatLng(it.latitude, it.longitude)) }
        }.build()
        map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, RoutePaddingPixels), RouteFitAnimationMillis)
    }

    private fun navigationZoom(speedMetersPerSecond: Double): Double = when {
        speedMetersPerSecond >= 22.0 -> 16.4
        speedMetersPerSecond >= 12.0 -> 16.8
        speedMetersPerSecond >= 4.0 -> 17.2
        else -> 17.5
    }

    private fun lookAheadMeters(speedMetersPerSecond: Double): Double =
        (55.0 + speedMetersPerSecond * 2.6).coerceIn(55.0, 120.0)

    private fun bearingDegrees(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val deltaLat = Math.toRadians(b.latitude - a.latitude)
        val deltaLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(deltaLat / 2.0).pow(2.0) + cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2.0)
        return 2.0 * EarthRadiusMeters * atan2(sqrt(h), sqrt(1.0 - h))
    }

    private fun Double.normalizeBearing(): Double = ((this % 360.0) + 360.0) % 360.0

    private companion object {
        const val BearingSmoothing = 0.28
        const val MinimumCameraIntervalMillis = 350L
        const val EarthRadiusMeters = 6_371_000.0
        const val RoutePaddingPixels = 112
        const val NavigationAnimationMillis = 650
        const val FollowAnimationMillis = 520
        const val RouteFitAnimationMillis = 900
    }
}
