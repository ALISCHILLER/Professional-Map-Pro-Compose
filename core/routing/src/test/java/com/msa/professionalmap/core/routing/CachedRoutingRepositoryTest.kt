package com.msa.professionalmap.core.routing

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedRoutingRepositoryTest {
    private val origin = GeoPoint(latitude = 35.0, longitude = 51.0)
    private val destination = GeoPoint(latitude = 35.1, longitude = 51.1)
    private val request = RoutingRequest(origin = origin, destination = destination)

    @Test
    fun `returns cached result for identical request inside ttl`() = runBlocking {
        var now = 1_000L
        val delegate = CountingRoutingRepository { callIndex -> providerResult("provider-$callIndex") }
        val repository = CachedRoutingRepository(
            delegate = delegate,
            ttlMillis = 5_000L,
            maxEntries = 10,
            nowMillis = { now },
        )

        val first = repository.calculateRoute(request)
        now += 1_000L
        val second = repository.calculateRoute(request)

        assertSame(first, second)
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun `refreshes cached result after ttl expires`() = runBlocking {
        var now = 1_000L
        val delegate = CountingRoutingRepository { callIndex -> providerResult("provider-$callIndex") }
        val repository = CachedRoutingRepository(
            delegate = delegate,
            ttlMillis = 500L,
            maxEntries = 10,
            nowMillis = { now },
        )

        val first = repository.calculateRoute(request)
        now += 600L
        val second = repository.calculateRoute(request)

        assertEquals("provider-1", first.provider)
        assertEquals("provider-2", second.provider)
        assertEquals(2, delegate.callCount)
    }

    @Test
    fun `does not cache provider failures`() = runBlocking {
        val delegate = FailsOnceRoutingRepository()
        val repository = CachedRoutingRepository(
            delegate = delegate,
            ttlMillis = 5_000L,
            maxEntries = 10,
            nowMillis = { 1_000L },
        )

        val firstFailure = runCatching { repository.calculateRoute(request) }
        val second = repository.calculateRoute(request)

        assertTrue(firstFailure.exceptionOrNull() is RoutingException)
        assertEquals("provider-success", second.provider)
        assertEquals(2, delegate.callCount)
    }

    @Test(expected = CancellationException::class)
    fun `propagates cancellation without caching it`() = runBlocking {
        val repository = CachedRoutingRepository(
            delegate = CancellingRoutingRepository,
            ttlMillis = 5_000L,
            maxEntries = 10,
            nowMillis = { 1_000L },
        )

        repository.calculateRoute(request)
    }

    @Test
    fun `close clears cache and closes delegate`() = runBlocking {
        val delegate = CountingRoutingRepository { callIndex -> providerResult("provider-$callIndex") }
        val repository = CachedRoutingRepository(
            delegate = delegate,
            ttlMillis = 5_000L,
            maxEntries = 10,
            nowMillis = { 1_000L },
        )

        repository.calculateRoute(request)
        repository.close()
        val resultAfterClose = repository.calculateRoute(request)

        assertTrue(delegate.closed)
        assertEquals("provider-2", resultAfterClose.provider)
        assertEquals(2, delegate.callCount)
    }

    private class CountingRoutingRepository(
        private val resultFactory: (Int) -> RoutingResult,
    ) : RoutingRepository {
        var callCount: Int = 0
            private set
        var closed: Boolean = false
            private set

        override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
            callCount += 1
            return resultFactory(callCount)
        }

        override fun close() {
            closed = true
        }
    }

    private class FailsOnceRoutingRepository : RoutingRepository {
        var callCount: Int = 0
            private set

        override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
            callCount += 1
            if (callCount == 1) throw RoutingException("temporary provider failure")
            return providerResult("provider-success")
        }
    }

    private object CancellingRoutingRepository : RoutingRepository {
        override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
            throw CancellationException("request was replaced")
        }
    }

    private companion object {
        fun providerResult(provider: String): RoutingResult = RoutingResult(
            routes = listOf(
                RouteAlternative(
                    id = provider,
                    title = "Primary",
                    summary = "Cached route",
                    points = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1)),
                    distanceMeters = 1_000.0,
                    durationSeconds = 120.0,
                    provider = provider,
                )
            ),
            provider = provider,
        )
    }
}
