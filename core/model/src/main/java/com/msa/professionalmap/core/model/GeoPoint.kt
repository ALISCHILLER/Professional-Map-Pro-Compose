package com.msa.professionalmap.core.model

import kotlin.math.max
import kotlin.math.min

@JvmInline
value class Latitude(val value: Double) {
    init { require(value in -90.0..90.0) { "Latitude must be in [-90, 90]." } }
}

@JvmInline
value class Longitude(val value: Double) {
    init { require(value in -180.0..180.0) { "Longitude must be in [-180, 180]." } }
}

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be in [-90, 90]." }
        require(longitude in -180.0..180.0) { "Longitude must be in [-180, 180]." }
    }

    val lat: Latitude get() = Latitude(latitude)
    val lon: Longitude get() = Longitude(longitude)
}

data class GeoBounds(
    val southWest: GeoPoint,
    val northEast: GeoPoint,
) {
    fun expandedByRatio(ratio: Double): GeoBounds {
        require(ratio >= 0.0) { "ratio must be non-negative." }
        val latPadding = (northEast.latitude - southWest.latitude) * ratio
        val lonPadding = (northEast.longitude - southWest.longitude) * ratio
        return GeoBounds(
            southWest = GeoPoint(
                latitude = max(-90.0, southWest.latitude - latPadding),
                longitude = max(-180.0, southWest.longitude - lonPadding),
            ),
            northEast = GeoPoint(
                latitude = min(90.0, northEast.latitude + latPadding),
                longitude = min(180.0, northEast.longitude + lonPadding),
            ),
        )
    }

    companion object {
        fun from(points: List<GeoPoint>, paddingDegrees: Double = 0.01): GeoBounds {
            require(points.isNotEmpty()) { "Cannot create bounds from an empty list." }
            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.maxOf { it.longitude }
            return GeoBounds(
                southWest = GeoPoint(
                    latitude = max(-90.0, minLat - paddingDegrees),
                    longitude = max(-180.0, minLon - paddingDegrees),
                ),
                northEast = GeoPoint(
                    latitude = min(90.0, maxLat + paddingDegrees),
                    longitude = min(180.0, maxLon + paddingDegrees),
                ),
            )
        }
    }
}
