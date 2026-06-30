package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.model.RoutingResult
import com.msa.professionalmap.core.observability.domain.AppMonitor
import com.msa.professionalmap.core.observability.domain.AppTrace
import com.msa.professionalmap.core.observability.domain.MonitorEvents
import com.msa.professionalmap.core.observability.domain.MonitorTraces

/**
 * Feature-level telemetry facade for the map screen.
 *
 * The ViewModel should not know analytics event names, custom-key names or trace
 * metric keys. Keeping those details here preserves Single Responsibility and
 * makes observability replaceable through the AppMonitor abstraction.
 */
class MapFeatureTelemetry(
    private val monitor: AppMonitor,
) {
    fun navigationStarted(routeDistanceMeters: Double, hasGps: Boolean) {
        setNavigationActive(true)
        monitor.logEvent(
            MonitorEvents.NAVIGATION_STARTED,
            mapOf(
                TelemetryKey.ROUTE_DISTANCE_METERS.key to routeDistanceMeters.toLong().toString(),
                TelemetryKey.HAS_GPS.key to hasGps.toString(),
            ),
        )
    }

    fun navigationCancelled() {
        monitor.logEvent(MonitorEvents.NAVIGATION_CANCELLED)
    }

    fun navigationCompleted(remainingDistanceMeters: Double) {
        setNavigationActive(false)
        monitor.logEvent(
            MonitorEvents.NAVIGATION_COMPLETED,
            mapOf(TelemetryKey.REMAINING_DISTANCE_METERS.key to remainingDistanceMeters.toLong().toString()),
        )
    }

    fun setNavigationActive(active: Boolean) {
        monitor.setCustomKey(TelemetryKey.NAVIGATION_ACTIVE.key, active.toString())
    }

    fun startRouteCalculationTrace(): AppTrace = monitor.startTrace(MonitorTraces.ROUTE_CALCULATION)

    fun routeCalculated(result: RoutingResult, trace: AppTrace) {
        trace.putMetric(TelemetryKey.ROUTE_COUNT.key, result.routes.size.toLong())
        trace.putMetric(TelemetryKey.DISTANCE_METERS.key, result.primaryRoute.distanceMeters.toLong())
        monitor.logEvent(
            MonitorEvents.ROUTE_CALCULATED,
            mapOf(
                TelemetryKey.PROVIDER.key to result.provider,
                TelemetryKey.ROUTE_COUNT.key to result.routes.size.toString(),
                TelemetryKey.DISTANCE_METERS.key to result.primaryRoute.distanceMeters.toLong().toString(),
                TelemetryKey.DURATION_SECONDS.key to result.primaryRoute.durationSeconds.toLong().toString(),
            ),
        )
    }

    fun routeCalculationFailed(cause: Throwable) {
        record(TelemetryArea.RouteCalculation, cause)
        monitor.logEvent(
            MonitorEvents.ROUTE_CALCULATION_FAILED,
            mapOf(TelemetryKey.MESSAGE.key to (cause.message ?: TelemetryValue.Unknown.value)),
        )
    }

    fun offRouteDetected(distanceMeters: Double) {
        monitor.logEvent(
            MonitorEvents.OFF_ROUTE_DETECTED,
            mapOf(TelemetryKey.DISTANCE_METERS.key to distanceMeters.toLong().toString()),
        )
    }

    fun rerouteTriggered(source: RerouteSource) {
        monitor.logEvent(
            MonitorEvents.REROUTE_TRIGGERED,
            mapOf(TelemetryKey.SOURCE.key to source.analyticsValue),
        )
    }

    fun startOfflineDownloadTrace(): AppTrace = monitor.startTrace(MonitorTraces.OFFLINE_DOWNLOAD_ENQUEUE)

    fun offlineDownloadQueued(workId: String, styleUrl: String, trace: AppTrace) {
        trace.incrementMetric(TelemetryKey.QUEUED.key, 1L)
        monitor.logEvent(
            MonitorEvents.OFFLINE_DOWNLOAD_QUEUED,
            mapOf(
                TelemetryKey.WORK_ID.key to workId,
                TelemetryKey.STYLE.key to styleUrl,
            ),
        )
    }

    fun voiceGuidanceError(area: TelemetryArea, throwable: Throwable) {
        record(area, throwable)
        monitor.logEvent(MonitorEvents.VOICE_GUIDANCE_ERROR)
    }

    fun record(area: TelemetryArea, throwable: Throwable) {
        monitor.recordException(throwable, mapOf(TelemetryKey.AREA.key to area.analyticsValue))
    }
}

enum class RerouteSource(val analyticsValue: String) {
    Debounced("debounced"),
    Immediate("immediate"),
}

enum class TelemetryArea(val analyticsValue: String) {
    LoadInitialScene("load_initial_scene"),
    RouteCalculation("route_calculation"),
    OfflineDownloadEnqueue("offline_download_enqueue"),
    OfflineRegionRefresh("offline_region_refresh"),
    OfflineRegionPause("offline_region_pause"),
    OfflineRegionResume("offline_region_resume"),
    OfflineRegionDelete("offline_region_delete"),
    OfflineAmbientCacheClear("offline_ambient_cache_clear"),
    OfflineDatabasePack("offline_database_pack"),
    VoiceGuidance("voice_guidance"),
    VoiceGuidanceTest("voice_guidance_test"),
}

private enum class TelemetryKey(val key: String) {
    AREA("area"),
    DISTANCE_METERS("distance_m"),
    DURATION_SECONDS("duration_s"),
    HAS_GPS("has_gps"),
    MESSAGE("message"),
    NAVIGATION_ACTIVE("navigation_active"),
    PROVIDER("provider"),
    QUEUED("queued"),
    REMAINING_DISTANCE_METERS("remaining_distance_m"),
    ROUTE_COUNT("route_count"),
    ROUTE_DISTANCE_METERS("route_distance_m"),
    SOURCE("source"),
    STYLE("style"),
    WORK_ID("work_id"),
}

private enum class TelemetryValue(val value: String) {
    Unknown("unknown"),
}
