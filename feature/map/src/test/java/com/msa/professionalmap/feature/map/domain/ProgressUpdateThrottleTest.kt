package com.msa.professionalmap.feature.map.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressUpdateThrottleTest {
    @Test
    fun `accepts first update and throttles subsequent updates inside window`() {
        val throttle = ProgressUpdateThrottle(MapFeatureConfig(progressThrottleMillis = 500L))

        assertTrue(throttle.shouldAccept(1_000L))
        assertFalse(throttle.shouldAccept(1_100L))
        assertTrue(throttle.shouldAccept(1_500L))
    }

    @Test
    fun `reset allows the next update immediately`() {
        val throttle = ProgressUpdateThrottle(MapFeatureConfig(progressThrottleMillis = 500L))

        assertTrue(throttle.shouldAccept(1_000L))
        assertFalse(throttle.shouldAccept(1_100L))
        throttle.reset()
        assertTrue(throttle.shouldAccept(1_100L))
    }
}
