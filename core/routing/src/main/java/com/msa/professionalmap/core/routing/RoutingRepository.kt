package com.msa.professionalmap.core.routing

import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import kotlinx.coroutines.CancellationException

/** Provider-neutral routing port. */
interface RoutingRepository : AutoCloseable {
    suspend fun calculateRoute(request: RoutingRequest): RoutingResult
    override fun close() = Unit
}

enum class RoutingErrorCode {
    NetworkUnavailable,
    Timeout,
    ProviderRejected,
    EmptyResponse,
    InvalidResponse,
    Configuration,
    Unknown,
}

/**
 * Sanitized routing failure. Raw HTTP URLs, coordinates, tokens and provider exception messages
 * must never cross this module boundary.
 */
class RoutingException(
    val code: RoutingErrorCode,
    val provider: String = "routing",
) : RuntimeException("Routing failed: ${code.name}") {
    constructor(@Suppress("UNUSED_PARAMETER") unsafeMessage: String) : this(code = RoutingErrorCode.Unknown)
}

fun Throwable.routingErrorCode(): RoutingErrorCode =
    (this as? RoutingException)?.code ?: RoutingErrorCode.Unknown
