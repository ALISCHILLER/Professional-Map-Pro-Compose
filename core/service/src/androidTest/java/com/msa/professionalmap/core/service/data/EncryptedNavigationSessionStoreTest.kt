package com.msa.professionalmap.core.service.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.service.domain.NavigationRoutingConfig
import com.msa.professionalmap.core.service.domain.NavigationSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EncryptedNavigationSessionStoreTest {
    @Test
    fun sessionRoundTripsWithoutPlaintextCoordinatesOnDisk() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = EncryptedNavigationSessionStore(context)
        store.clear()
        val session = NavigationSession(
            id = "instrumented-session",
            route = RouteAlternative(
                id = "route",
                title = "Route",
                summary = "Secure route",
                points = listOf(
                    GeoPoint(latitude = 35.6892, longitude = 51.3890),
                    GeoPoint(latitude = 35.7000, longitude = 51.4000),
                ),
                distanceMeters = 2_000.0,
                durationSeconds = 300.0,
                provider = "test",
            ),
            destinationTitle = "Destination",
            languageTag = "fa-IR",
            guidanceConfig = GuidanceConfig(
                enabled = true,
                muted = true,
                language = GuidanceLanguage.Persian,
                volume = 0.35f,
                minAnnouncementIntervalMillis = 7_000L,
                offRouteRepeatIntervalMillis = 20_000L,
                announceArrival = false,
            ),
            routingConfig = NavigationRoutingConfig(
                baseUrl = "https://routing.example.invalid",
                userAgent = "ProfessionalMapPro-Test/1.0",
            ),
        )

        store.save(session)
        val restored = store.read()
        val encryptedFile = context.noBackupFilesDir.resolve("navigation-session.enc")
        val raw = encryptedFile.readText()

        assertNotNull(restored)
        assertEquals(session.id, restored?.id)
        assertEquals(session.destination, restored?.destination)
        assertEquals(session.guidanceConfig, restored?.guidanceConfig)
        assertFalse(raw.contains("35.6892"))
        assertFalse(raw.contains("51.389"))
        store.clear()
    }
    @Test
    fun concurrentStoreInstancesDoNotCorruptEncryptedSession() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val firstStore = EncryptedNavigationSessionStore(context)
        val secondStore = EncryptedNavigationSessionStore(context)
        firstStore.clear()
        val first = testSession("concurrent-a", 35.6892, 51.3890)
        val second = testSession("concurrent-b", 36.2605, 59.6168)
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)

        listOf(firstStore to first, secondStore to second).forEach { (store, session) ->
            executor.execute {
                try {
                    start.await(5, TimeUnit.SECONDS)
                    repeat(5) {
                        store.save(session)
                        assertNotNull(store.read())
                    }
                } catch (throwable: Throwable) {
                    failures += throwable
                } finally {
                    done.countDown()
                }
            }
        }

        start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS))
        executor.shutdownNow()
        assertTrue(failures.joinToString("\n") { it.stackTraceToString() }, failures.isEmpty())
        assertTrue(firstStore.read()?.id in setOf(first.id, second.id))
        firstStore.clear()
    }

    private fun testSession(id: String, latitude: Double, longitude: Double): NavigationSession = NavigationSession(
        id = id,
        route = RouteAlternative(
            id = "route-$id",
            title = "Route",
            summary = "Concurrent encrypted route",
            points = listOf(
                GeoPoint(latitude = latitude, longitude = longitude),
                GeoPoint(latitude = latitude + 0.01, longitude = longitude + 0.01),
            ),
            distanceMeters = 2_000.0,
            durationSeconds = 300.0,
            provider = "test",
        ),
        destinationTitle = "Destination",
        languageTag = "en",
        guidanceConfig = GuidanceConfig(),
        routingConfig = NavigationRoutingConfig(
            baseUrl = "https://routing.example.invalid",
            userAgent = "ProfessionalMapPro-Test/1.0",
        ),
    )

}
