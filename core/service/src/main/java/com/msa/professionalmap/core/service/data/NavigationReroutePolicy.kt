package com.msa.professionalmap.core.service.data

import com.msa.professionalmap.core.progress.domain.RouteProgressConfig
import kotlin.math.max

/** Prevents reroute storms caused by a single noisy fix or repeated provider failures. */
internal class NavigationReroutePolicy(
    private val debounceMillis: Long,
    private val retryCooldownMillis: Long = DefaultRetryCooldownMillis,
    private val minimumConsecutiveSamples: Int = DefaultConsecutiveSamples,
) {
    constructor(config: RouteProgressConfig) : this(config.rerouteDebounceMillis)

    private var consecutiveOffRouteSamples = 0
    private var firstOffRouteAtMillis: Long? = null
    private var lastAttemptAtMillis: Long? = null

    fun delayBeforeAttempt(nowMillis: Long): Long? {
        consecutiveOffRouteSamples += 1
        if (firstOffRouteAtMillis == null) firstOffRouteAtMillis = nowMillis
        if (consecutiveOffRouteSamples < minimumConsecutiveSamples) return null
        val debounceRemaining = debounceMillis - (nowMillis - checkNotNull(firstOffRouteAtMillis))
        val cooldownRemaining = lastAttemptAtMillis
            ?.let { retryCooldownMillis - (nowMillis - it) }
            ?: 0L
        return max(debounceRemaining, cooldownRemaining).coerceAtLeast(0L)
    }

    fun markAttempt(nowMillis: Long) {
        lastAttemptAtMillis = nowMillis
    }

    fun onRouteRecovered() {
        consecutiveOffRouteSamples = 0
        firstOffRouteAtMillis = null
    }

    fun onRerouteSucceeded() {
        reset()
    }

    fun reset() {
        consecutiveOffRouteSamples = 0
        firstOffRouteAtMillis = null
        lastAttemptAtMillis = null
    }

    private companion object {
        const val DefaultRetryCooldownMillis = 20_000L
        const val DefaultConsecutiveSamples = 2
    }
}
