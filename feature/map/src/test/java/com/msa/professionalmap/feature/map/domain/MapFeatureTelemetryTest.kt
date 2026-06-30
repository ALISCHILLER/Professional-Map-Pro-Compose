package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RoutingResult
import com.msa.professionalmap.core.observability.domain.AppMonitor
import com.msa.professionalmap.core.observability.domain.AppTrace
import com.msa.professionalmap.core.observability.domain.MonitorEvents
import org.junit.Assert.assertEquals
import org.junit.Test

class MapFeatureTelemetryTest {
    @Test
    fun `navigation start is reported through semantic telemetry facade`() {
        val monitor = RecordingAppMonitor()
        val telemetry = MapFeatureTelemetry(monitor)

        telemetry.navigationStarted(routeDistanceMeters = 1234.8, hasGps = true)

        assertEquals("true", monitor.customKeys["navigation_active"])
        assertEquals(MonitorEvents.NAVIGATION_STARTED, monitor.events.single().name)
        assertEquals("1234", monitor.events.single().params["route_distance_m"])
        assertEquals("true", monitor.events.single().params["has_gps"])
    }

    @Test
    fun `route calculated adds metrics and event parameters`() {
        val monitor = RecordingAppMonitor()
        val trace = monitor.trace
        val telemetry = MapFeatureTelemetry(monitor)
        val route = RouteAlternative(
            id = "primary",
            title = "Primary",
            summary = "Fastest",
            points = listOf(GeoPoint(35.0, 51.0), GeoPoint(35.1, 51.1)),
            distanceMeters = 1500.0,
            durationSeconds = 180.0,
            provider = "test-provider",
        )

        telemetry.routeCalculated(RoutingResult(routes = listOf(route), provider = "test-provider"), trace)

        assertEquals(1L, trace.metrics["route_count"])
        assertEquals(1500L, trace.metrics["distance_m"])
        assertEquals(MonitorEvents.ROUTE_CALCULATED, monitor.events.single().name)
        assertEquals("test-provider", monitor.events.single().params["provider"])
    }

    @Test
    fun `errors carry feature area without leaking raw map keys to ViewModel`() {
        val monitor = RecordingAppMonitor()
        val telemetry = MapFeatureTelemetry(monitor)
        val error = IllegalStateException("boom")

        telemetry.record(TelemetryArea.OfflineDownloadEnqueue, error)

        assertEquals(error, monitor.exceptions.single().throwable)
        assertEquals("offline_download_enqueue", monitor.exceptions.single().context["area"])
    }

    private class RecordingAppMonitor : AppMonitor {
        val customKeys = linkedMapOf<String, String>()
        val events = mutableListOf<Event>()
        val exceptions = mutableListOf<RecordedException>()
        val trace = RecordingTrace()

        override fun setUserId(userId: String?) = Unit
        override fun setCustomKey(key: String, value: String) {
            customKeys[key] = value
        }

        override fun recordException(throwable: Throwable, context: Map<String, String>) {
            exceptions += RecordedException(throwable, context)
        }

        override fun logEvent(name: String, params: Map<String, String>) {
            events += Event(name, params)
        }

        override fun startTrace(name: String): AppTrace {
            trace.startedNames += name
            return trace
        }
    }

    private data class Event(val name: String, val params: Map<String, String>)
    private data class RecordedException(val throwable: Throwable, val context: Map<String, String>)

    private class RecordingTrace : AppTrace {
        val startedNames = mutableListOf<String>()
        val metrics = linkedMapOf<String, Long>()
        var stopped = false

        override fun putMetric(name: String, value: Long) {
            metrics[name] = value
        }

        override fun incrementMetric(name: String, by: Long) {
            metrics[name] = (metrics[name] ?: 0L) + by
        }

        override fun stop() {
            stopped = true
        }
    }
}
