package com.msa.professionalmap.feature.map.domain

/**
 * Runtime knobs for the map feature.
 *
 * Keeping these values outside MapViewModel makes navigation behavior explicit,
 * easy to test and simple to tune per build type or future product flavor.
 */
data class MapFeatureConfig(
    val routeRequestDebounceMillis: Long = 500L,
    val progressThrottleMillis: Long = 500L,
    val arrivalConfirmationMillis: Long = 3_000L,
    val arrivalSpeedThresholdMetersPerSecond: Double = 2.0,
    val locationUpdateIntervalMillis: Long = 2_000L,
    val locationMinUpdateIntervalMillis: Long = 1_000L,
    val locationMinDistanceMeters: Float = 2.0f,
    val waitForAccurateLocation: Boolean = false,
    val offlineAmbientCacheBytes: Long = 256L * 1024L * 1024L,
    val offlinePixelRatio: Float = 2.0f,
    val offlineIncludeIdeographs: Boolean = false,
    val offlineRequireUnmeteredNetwork: Boolean = false,
    val offlineRequireBatteryNotLow: Boolean = true,
    val offlineRequireStorageNotLow: Boolean = true,
) {
    init {
        require(routeRequestDebounceMillis >= 0L) { "routeRequestDebounceMillis must be non-negative." }
        require(progressThrottleMillis >= 0L) { "progressThrottleMillis must be non-negative." }
        require(arrivalConfirmationMillis >= 0L) { "arrivalConfirmationMillis must be non-negative." }
        require(arrivalSpeedThresholdMetersPerSecond >= 0.0) { "arrivalSpeedThresholdMetersPerSecond must be non-negative." }
        require(locationUpdateIntervalMillis > 0L) { "locationUpdateIntervalMillis must be positive." }
        require(locationMinUpdateIntervalMillis > 0L) { "locationMinUpdateIntervalMillis must be positive." }
        require(locationMinDistanceMeters >= 0.0f) { "locationMinDistanceMeters must be non-negative." }
        require(offlineAmbientCacheBytes > 0L) { "offlineAmbientCacheBytes must be positive." }
        require(offlinePixelRatio > 0.0f) { "offlinePixelRatio must be positive." }
    }
}
