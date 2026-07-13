package com.msa.professionalmap.core.location

import com.msa.professionalmap.core.model.GeoPoint

/**
 * Domain-friendly wrapper around Android's Location object.
 *
 * Keep Android framework types at the edge of the module so map/routing features can consume a
 * stable model that is easy to fake in tests.
 */
data class DeviceLocation(
    val position: GeoPoint,
    val accuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    val bearingDegrees: Float? = null,
    val speedMetersPerSecond: Float? = null,
    val timestampMillis: Long = 0L,
    val provider: String? = null,
    val isMock: Boolean = false,
) {
    init {
        require(accuracyMeters == null || (accuracyMeters.isFinite() && accuracyMeters >= 0f)) {
            "accuracyMeters must be null or finite and non-negative."
        }
        require(altitudeMeters == null || altitudeMeters.isFinite()) {
            "altitudeMeters must be null or finite."
        }
        require(bearingDegrees == null || (bearingDegrees.isFinite() && bearingDegrees in 0f..360f)) {
            "bearingDegrees must be null or finite and between 0 and 360 degrees."
        }
        require(speedMetersPerSecond == null || (speedMetersPerSecond.isFinite() && speedMetersPerSecond >= 0f)) {
            "speedMetersPerSecond must be null or finite and non-negative."
        }
        require(timestampMillis >= 0L) { "timestampMillis must be non-negative." }
    }

    val hasBearing: Boolean get() = bearingDegrees != null
    val hasSpeed: Boolean get() = speedMetersPerSecond != null
}
