package com.msa.professionalmap.core.routing

import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import java.util.LinkedHashMap
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel

/** Bounded LRU route cache with monotonic TTL and duplicate-request coalescing. */
class CachedRoutingRepository(
    private val delegate: RoutingRepository,
    private val ttlMillis: Long = 5 * 60 * 1000L,
    private val maxEntries: Int = 50,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) : RoutingRepository {
    private val lock = Any()
    private val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = object : LinkedHashMap<String, CacheEntry>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean =
            size > maxEntries
    }
    private val inFlight = mutableMapOf<String, Deferred<RoutingResult>>()
    private var closed = false

    init {
        require(ttlMillis >= 0L) { "ttlMillis must be non-negative." }
        require(maxEntries > 0) { "maxEntries must be positive." }
    }

    override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
        val key = request.cacheKey()
        val shared = synchronized(lock) {
            check(!closed) { "Routing repository is closed." }

            cache[key]
                ?.takeIf { nowMillis() - it.createdAtMillis <= ttlMillis }
                ?.let { return it.result }

            inFlight[key] ?: requestScope.async(start = CoroutineStart.LAZY) {
                delegate.calculateRoute(request).also { result ->
                    synchronized(lock) {
                        if (!closed) cache[key] = CacheEntry(result, nowMillis())
                    }
                }
            }.also { deferred ->
                inFlight[key] = deferred
                deferred.invokeOnCompletion {
                    synchronized(lock) {
                        if (inFlight[key] === deferred) inFlight.remove(key)
                    }
                }
            }
        }

        shared.start()
        return shared.await()
    }

    override fun close() {
        val pending = synchronized(lock) {
            if (closed) return
            closed = true
            cache.clear()
            inFlight.values.toList().also { inFlight.clear() }
        }
        pending.forEach { it.cancel() }
        requestScope.cancel()
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
            append('|').append(point.latitude.roundForRouteKey())
            append(',').append(point.longitude.roundForRouteKey())
        }
    }

    private fun Double.roundForRouteKey(): String = String.format(Locale.US, "%.5f", this)
}
