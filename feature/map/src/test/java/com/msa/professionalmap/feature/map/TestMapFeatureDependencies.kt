package com.msa.professionalmap.feature.map

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.guidance.data.DefaultVoiceGuidanceRepository
import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.TtsEngine
import com.msa.professionalmap.core.guidance.domain.toLanguageTag
import com.msa.professionalmap.core.location.LocationConfig
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.location.LocationProvidersState
import com.msa.professionalmap.core.location.LocationRepository
import com.msa.professionalmap.core.location.LocationStatus
import com.msa.professionalmap.core.location.LocationTrackingState
import com.msa.professionalmap.core.mapdata.StaticMapRepository
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import com.msa.professionalmap.core.observability.domain.DisabledAppMonitor
import com.msa.professionalmap.core.offline.domain.OfflineDownloadManager
import com.msa.professionalmap.core.offline.domain.OfflineDownloadWorkRequest
import com.msa.professionalmap.core.offline.domain.OfflineManagerState
import com.msa.professionalmap.core.offline.domain.OfflineMapRepository
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import com.msa.professionalmap.core.offline.domain.OfflineWorkManagerState
import com.msa.professionalmap.core.progress.data.DefaultRouteProgressRepository
import com.msa.professionalmap.core.routing.RoutingRepository
import com.msa.professionalmap.core.service.domain.NavigationRoutingConfig
import com.msa.professionalmap.core.service.domain.NavigationRuntimeState
import com.msa.professionalmap.core.service.domain.NavigationServiceController
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import com.msa.professionalmap.core.service.domain.NavigationSession
import com.msa.professionalmap.feature.map.di.MapFeatureDependencies
import com.msa.professionalmap.feature.map.domain.LanguagePreferenceStore
import com.msa.professionalmap.feature.map.domain.MapFeatureConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal fun testMapFeatureDependencies(
    routingRepository: RoutingRepository = TestRoutingRepository(),
): MapFeatureDependencies = MapFeatureDependencies(
    repository = StaticMapRepository(),
    geoEngine = KotlinGeoEngine,
    locationRepository = TestLocationRepository(),
    routingRepository = routingRepository,
    offlineRepository = TestOfflineMapRepository(),
    offlineDownloadManager = TestOfflineDownloadManager(),
    progressRepository = DefaultRouteProgressRepository(),
    guidanceRepository = DefaultVoiceGuidanceRepository(),
    ttsEngine = TestTtsEngine(),
    navigationServiceController = TestNavigationServiceController(),
    navigationRoutingConfig = NavigationRoutingConfig(
        baseUrl = "https://routing.test",
        userAgent = "ProfessionalMapPro-Test/1.0",
    ),
    appMonitor = DisabledAppMonitor,
    languagePreferenceStore = InMemoryLanguagePreferenceStore(),
    config = MapFeatureConfig(),
)

internal class TestLocationRepository : LocationRepository {
    private val _trackingState = MutableStateFlow(
        LocationTrackingState(status = LocationStatus.Idle, permissionLevel = LocationPermissionLevel.None)
    )
    override val trackingState: StateFlow<LocationTrackingState> = _trackingState

    override fun setPermissionLevel(permissionLevel: LocationPermissionLevel) {
        _trackingState.value = _trackingState.value.copy(permissionLevel = permissionLevel)
    }

    override suspend fun refreshProviderState(): LocationProvidersState = LocationProvidersState.Unknown
    override suspend fun refreshLastKnownLocation() = Unit

    override fun start(config: LocationConfig) {
        _trackingState.value = _trackingState.value.copy(status = LocationStatus.Starting)
    }

    override fun stop() {
        _trackingState.value = _trackingState.value.copy(status = LocationStatus.Stopped)
    }
}

internal class TestOfflineMapRepository : OfflineMapRepository {
    private val _state = MutableStateFlow(OfflineManagerState())
    override val state: StateFlow<OfflineManagerState> = _state

    override suspend fun refreshRegions() = Unit
    override suspend fun downloadRegion(request: OfflineRegionRequest) = Unit
    override suspend fun pauseRegion(clientId: String) = Unit
    override suspend fun resumeRegion(clientId: String) = Unit
    override suspend fun deleteRegion(clientId: String) = Unit
    override suspend fun setAmbientCacheSize(bytes: Long) = Unit
    override suspend fun clearAmbientCache() = Unit
    override suspend fun invalidateAmbientCache() = Unit
    override suspend fun packDatabase() = Unit
}

internal class TestOfflineDownloadManager : OfflineDownloadManager {
    private val _state = MutableStateFlow(OfflineWorkManagerState())
    override val state: StateFlow<OfflineWorkManagerState> = _state

    override suspend fun enqueueDownload(request: OfflineDownloadWorkRequest): String = "test-work-${request.request.clientId}"
    override suspend fun cancel(clientId: String) = Unit
    override suspend fun cancelAll() = Unit
}

internal class TestTtsEngine : TtsEngine {
    override val isSpeaking: Boolean = false
    override suspend fun speak(text: String, language: GuidanceLanguage, volume: Float): Result<Unit> = Result.success(Unit)
    override fun stop() = Unit
}

internal class TestNavigationServiceController : NavigationServiceController {
    private val mutableRuntimeState = MutableStateFlow(NavigationRuntimeState())
    override val runtimeState: StateFlow<NavigationRuntimeState> = mutableRuntimeState

    val sessions = mutableListOf<NavigationSession>()
    var stopped = false

    override fun start(session: NavigationSession) {
        stopped = false
        sessions += session
        mutableRuntimeState.value = NavigationRuntimeState(
            status = NavigationServiceStatus.Active,
            session = session,
            snapshot = NavigationServiceSnapshot(
                status = NavigationServiceStatus.Active,
                destinationTitle = session.destinationTitle,
                destination = session.destination,
                languageTag = session.languageTag,
            ),
            remainingRoutePoints = session.route.points,
        )
    }

    override fun pause() {
        mutableRuntimeState.value = mutableRuntimeState.value.copy(status = NavigationServiceStatus.Paused)
    }

    override fun resume() {
        mutableRuntimeState.value = mutableRuntimeState.value.copy(status = NavigationServiceStatus.Active)
    }

    override fun updateGuidance(config: GuidanceConfig) {
        mutableRuntimeState.value = mutableRuntimeState.value.copy(
            session = mutableRuntimeState.value.session?.copy(
                languageTag = config.language.toLanguageTag(),
                guidanceConfig = config,
            ),
        )
    }

    override fun stop() {
        stopped = true
        mutableRuntimeState.value = NavigationRuntimeState()
    }

    override fun restore() = Unit
}

internal class InMemoryLanguagePreferenceStore : LanguagePreferenceStore {
    private var language: GuidanceLanguage? = null
    override fun loadLanguage(): GuidanceLanguage? = language
    override fun saveLanguage(language: GuidanceLanguage) {
        this.language = language
    }
}

internal class TestRoutingRepository : RoutingRepository {
    override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
        val points = listOf(request.origin, request.destination)
        return RoutingResult(
            provider = "test-router",
            routes = listOf(
                RouteAlternative(
                    id = "test-route-0",
                    title = "Test route",
                    summary = "Deterministic two-point route",
                    points = points,
                    distanceMeters = 1000.0,
                    durationSeconds = 600.0,
                    provider = "test-router",
                )
            ),
        )
    }
}

internal class FailingRoutingRepository : RoutingRepository {
    override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
        error("Route provider unavailable in this test.")
    }
}
