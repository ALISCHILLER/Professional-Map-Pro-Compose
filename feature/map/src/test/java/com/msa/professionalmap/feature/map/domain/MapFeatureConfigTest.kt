package com.msa.professionalmap.feature.map.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MapFeatureConfigTest {
    @Test
    fun `default config keeps route and progress throttles production friendly`() {
        val config = MapFeatureConfig()

        assertEquals(500L, config.routeRequestDebounceMillis)
        assertEquals(500L, config.progressThrottleMillis)
        assertEquals(3_000L, config.arrivalConfirmationMillis)
        assertEquals(2_000L, config.locationUpdateIntervalMillis)
        assertEquals(1_000L, config.locationMinUpdateIntervalMillis)
        assertEquals(2.0f, config.locationMinDistanceMeters, 0.0f)
        assertEquals(false, config.offlineRequireUnmeteredNetwork)
        assertEquals(true, config.offlineRequireBatteryNotLow)
        assertEquals(true, config.offlineRequireStorageNotLow)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects invalid offline pixel ratio`() {
        MapFeatureConfig(offlinePixelRatio = 0.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects invalid location interval`() {
        MapFeatureConfig(locationUpdateIntervalMillis = 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects negative location distance`() {
        MapFeatureConfig(locationMinDistanceMeters = -1.0f)
    }
}
