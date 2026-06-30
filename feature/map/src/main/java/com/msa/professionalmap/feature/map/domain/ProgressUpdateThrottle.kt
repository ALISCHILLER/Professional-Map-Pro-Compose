package com.msa.professionalmap.feature.map.domain

/**
 * Small state holder that gates expensive progress calculations.
 *
 * GPS can emit noisy/high-frequency updates. Keeping throttling in this class
 * avoids sprinkling timestamp state across the ViewModel and makes the behavior
 * easy to unit-test.
 */
class ProgressUpdateThrottle(
    private val config: MapFeatureConfig,
) {
    private var lastAcceptedTimestampMillis: Long = 0L

    fun shouldAccept(timestampMillis: Long): Boolean {
        val elapsed = timestampMillis - lastAcceptedTimestampMillis
        if (elapsed < config.progressThrottleMillis) return false
        lastAcceptedTimestampMillis = timestampMillis
        return true
    }

    fun reset() {
        lastAcceptedTimestampMillis = 0L
    }
}
