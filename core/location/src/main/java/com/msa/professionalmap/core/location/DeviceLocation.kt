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
    val accuracyMeters: Float?,
    val altitudeMeters: Double?,
    val bearingDegrees: Float?,
    val speedMetersPerSecond: Float?,
    val timestampMillis: Long,
    val provider: String?,
    val isMock: Boolean,
) {
    val hasBearing: Boolean get() = bearingDegrees != null && !bearingDegrees.isNaN()
    val hasSpeed: Boolean get() = speedMetersPerSecond != null && !speedMetersPerSecond.isNaN()
}
