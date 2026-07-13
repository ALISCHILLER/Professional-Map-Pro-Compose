package com.msa.professionalmap.core.service.domain

import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.location.LocationConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.TravelMode
import com.msa.professionalmap.core.progress.domain.ProgressState
import kotlinx.coroutines.flow.StateFlow

/** Lifecycle of the independent navigation runtime. */
enum class NavigationServiceStatus {
    Idle,
    Starting,
    Active,
    Paused,
    Rerouting,
    Stopping,
    Completed,
    Failed,
}

data class NavigationServiceConfig(
    val notificationChannelId: String = "navigation_service",
    val notificationChannelName: String = "Navigation",
    val notificationId: Int = 4041,
    val useWakeLock: Boolean = true,
    val autoStopAfterInactiveMillis: Long = 30L * 60L * 1000L,
) {
    init {
        require(notificationId > 0) { "notificationId must be positive." }
        require(autoStopAfterInactiveMillis > 0L) { "autoStopAfterInactiveMillis must be positive." }
    }
}

data class NavigationRoutingConfig(
    val baseUrl: String,
    val userAgent: String,
    val allowCleartextTraffic: Boolean = false,
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank." }
        require(userAgent.isNotBlank()) { "userAgent must not be blank." }
    }
}

/**
 * Complete navigation input persisted before the foreground service starts.
 * Precise live locations are deliberately not persisted.
 */
data class NavigationSession(
    val id: String,
    val route: RouteAlternative,
    val destinationTitle: String,
    val languageTag: String,
    val guidanceConfig: GuidanceConfig = GuidanceConfig(),
    val travelMode: TravelMode = TravelMode.Driving,
    val routingConfig: NavigationRoutingConfig,
    val locationConfig: LocationConfig = LocationConfig(),
    val startedAtMillis: Long = System.currentTimeMillis(),
) {
    init {
        require(id.isNotBlank()) { "id must not be blank." }
        require(route.isNavigationEligible) { "Only navigable routes can start a navigation session." }
        require(destinationTitle.isNotBlank()) { "destinationTitle must not be blank." }
        require(languageTag.isNotBlank()) { "languageTag must not be blank." }
    }

    val destination: GeoPoint get() = route.points.last()
}

data class NavigationServiceSnapshot(
    val status: NavigationServiceStatus = NavigationServiceStatus.Idle,
    val destinationTitle: String = "Destination",
    val remainingDistanceText: String = "--",
    val remainingDurationText: String = "--",
    val nextInstructionText: String? = null,
    val destination: GeoPoint? = null,
    val languageTag: String = "en",
    val lastUpdatedAtMillis: Long = 0L,
) {
    init {
        require(languageTag.isNotBlank()) { "languageTag must not be blank." }
    }
}

enum class NavigationRuntimeErrorCode {
    MissingLocationPermission,
    LocationProviderUnavailable,
    SessionUnavailable,
    SessionPersistenceFailed,
    ForegroundStartBlocked,
    RoutingFailed,
    UnexpectedFailure,
}

data class NavigationRuntimeState(
    val status: NavigationServiceStatus = NavigationServiceStatus.Idle,
    val session: NavigationSession? = null,
    val snapshot: NavigationServiceSnapshot = NavigationServiceSnapshot(),
    val currentLocation: DeviceLocation? = null,
    val progressState: ProgressState = ProgressState.Idle,
    val completedRoutePoints: List<GeoPoint> = emptyList(),
    val remainingRoutePoints: List<GeoPoint> = emptyList(),
    val snappedLocation: GeoPoint? = null,
    val errorCode: NavigationRuntimeErrorCode? = null,
) {
    val isRunning: Boolean
        get() = status == NavigationServiceStatus.Starting ||
            status == NavigationServiceStatus.Active ||
            status == NavigationServiceStatus.Paused ||
            status == NavigationServiceStatus.Rerouting
}

interface NavigationServiceController {
    val runtimeState: StateFlow<NavigationRuntimeState>

    fun start(session: NavigationSession)
    fun pause()
    fun resume()
    fun updateGuidance(config: GuidanceConfig)
    fun stop()

    /** Restores a persisted session while the app UI is visible. */
    fun restore()
}
