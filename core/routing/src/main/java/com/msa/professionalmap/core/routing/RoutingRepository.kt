package com.msa.professionalmap.core.routing

import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import kotlinx.coroutines.CancellationException

/**
 * Provider-neutral routing port.
 *
 * Implementations may map provider/network failures to [RoutingException], but they must always
 * propagate [CancellationException] unchanged. Route requests are frequently replaced while the
 * user moves or selects another destination; swallowing cancellation can produce stale fallback
 * routes and break structured concurrency.
 */
interface RoutingRepository : AutoCloseable {
    suspend fun calculateRoute(request: RoutingRequest): RoutingResult

    override fun close() = Unit
}

class RoutingException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
