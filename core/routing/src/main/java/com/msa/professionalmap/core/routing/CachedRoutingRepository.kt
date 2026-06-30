package com.msa.professionalmap.core.routing

import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import java.util.LinkedHashMap
import java.util.Locale

/**
 * Small in-memory LRU cache for route requests. This avoids repeated OSRM calls when the user
 * retries the same origin/destination/profile within a short time window.
 */
class CachedRoutingRepository(
    private val delegate: RoutingRepository,
    private val ttlMillis: Long = 5 * 60 * 1000L,
    private val maxEntries: Int = 50,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : RoutingRepository {
    private val cache = object : LinkedHashMap<String, CacheEntry>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return size > maxEntries
        }
    }

    override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
        val key = request.cacheKey()
        val now = nowMillis()
        synchronized(cache) {
            cache[key]?.takeIf { now - it.createdAtMillis <= ttlMillis }?.let { return it.result }
        }
        val result = delegate.calculateRoute(request)
        synchronized(cache) {
            cache[key] = CacheEntry(result = result, createdAtMillis = nowMillis())
        }
        return result
    }

    override fun close() {
        synchronized(cache) { cache.clear() }
        delegate.close()
    }

    private data class CacheEntry(
        val result: RoutingResult,
        val createdAtMillis: Long,
    )

    private fun RoutingRequest.cacheKey(): String = buildString {
        append(travelMode.name)
        append('|').append(requestAlternatives)
        append('|').append(includeSteps)
        append('|').append(overview.name)
        allWaypoints.forEach { point ->
            append('|').append(point.latitude.roundForRouteKey()).append(',').append(point.longitude.roundForRouteKey())
        }
    }

    private fun Double.roundForRouteKey(): String = String.format(Locale.US, "%.5f", this)
}
