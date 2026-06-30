package com.msa.professionalmap.core.location

/**
 * Runtime-tunable location request configuration.
 *
 * Defaults are suitable for an interactive map. Navigation/tracking screens can request a more
 * aggressive interval, while overview screens can use balanced priority to save battery.
 */
data class LocationConfig(
    val intervalMillis: Long = 2_000L,
    val minUpdateIntervalMillis: Long = 1_000L,
    val minDistanceMeters: Float = 2f,
    val priority: LocationPriority = LocationPriority.HighAccuracy,
    val waitForAccurateLocation: Boolean = false,
) {
    init {
        require(intervalMillis > 0L) { "intervalMillis must be positive." }
        require(minUpdateIntervalMillis > 0L) { "minUpdateIntervalMillis must be positive." }
        require(minDistanceMeters >= 0f) { "minDistanceMeters must be non-negative." }
    }
}

enum class LocationPriority {
    HighAccuracy,
    Balanced,
    LowPower,
    Passive,
}

enum class LocationPermissionLevel {
    None,
    Approximate,
    Precise,
}
