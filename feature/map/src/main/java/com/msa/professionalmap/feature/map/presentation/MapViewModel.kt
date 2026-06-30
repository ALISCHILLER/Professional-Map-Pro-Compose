package com.msa.professionalmap.feature.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.TtsEngine
import com.msa.professionalmap.core.guidance.domain.VoiceGuidanceRepository
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.location.LocationRepository
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.offline.domain.OfflineDownloadManager
import com.msa.professionalmap.core.offline.domain.OfflineMapRepository
import com.msa.professionalmap.core.routing.RoutingRepository
import com.msa.professionalmap.core.service.domain.NavigationServiceController
import com.msa.professionalmap.feature.map.domain.LanguagePreferenceStore
import com.msa.professionalmap.feature.map.domain.MapFeatureTelemetry
import com.msa.professionalmap.feature.map.domain.MapViewModelRuntime
import com.msa.professionalmap.feature.map.domain.TelemetryArea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Thin presentation orchestrator for the map feature.
 *
 * User intents enter here, but stateful workflows are delegated to focused
 * controllers and use cases. This keeps the ViewModel small, testable and aligned
 * with SRP: location, routing, navigation, guidance, offline work and geometry are
 * owned by their dedicated collaborators.
 */
class MapViewModel(
    locationRepository: LocationRepository,
    private val routingRepository: RoutingRepository,
    offlineRepository: OfflineMapRepository,
    offlineDownloadManager: OfflineDownloadManager,
    guidanceRepository: VoiceGuidanceRepository,
    ttsEngine: TtsEngine,
    navigationServiceController: NavigationServiceController,
    private val telemetry: MapFeatureTelemetry,
    languagePreferenceStore: LanguagePreferenceStore,
    private val runtime: MapViewModelRuntime,
) : ViewModel() {

    private val _uiState: MutableStateFlow<MapUiState> =
        MutableStateFlow(MapUiState(loadState = MapLoadState.Loading))

    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val guidanceController: MapGuidanceController = MapGuidanceController(
        state = _uiState,
        scope = viewModelScope,
        guidanceRepository = guidanceRepository,
        ttsEngine = ttsEngine,
        languagePreferenceStore = languagePreferenceStore,
        telemetry = telemetry,
    )

    private val navigationController: MapNavigationController = MapNavigationController(
        state = _uiState,
        scope = viewModelScope,
        progressConfig = runtime.progressConfig,
        reroutingStrategy = runtime.reroutingStrategy,
        startNavigationUseCase = runtime.useCases.startNavigation,
        updateNavigationProgressUseCase = runtime.updateNavigationProgress,
        buildNavigationSnapshot = runtime.useCases.buildNavigationSnapshot,
        navigationServiceController = navigationServiceController,
        guidanceController = guidanceController,
        telemetry = telemetry,
        requestRoute = { origin: GeoPoint, destination: GeoPoint, keepNavigationActive: Boolean ->
            routeController.requestRoute(origin, destination, keepNavigationActive)
        },
    )

    private val routeController: MapRouteController = MapRouteController(
        state = _uiState,
        scope = viewModelScope,
        featureConfig = runtime.featureConfig,
        useCases = runtime.useCases,
        telemetry = telemetry,
        resetProgressRuntime = { cancelReroute: Boolean ->
            navigationController.resetProgressRuntimeState(cancelReroute)
        },
        onRouteChangedForActiveNavigation = { location ->
            navigationController.updateProgress(location)
        },
    )

    private val locationController: MapLocationController = MapLocationController(
        state = _uiState,
        scope = viewModelScope,
        locationRepository = locationRepository,
        featureConfig = runtime.featureConfig,
        updateNavigationProgress = { location ->
            navigationController.updateProgress(location)
        },
        stopNavigation = { message ->
            navigationController.stopNavigationInternal(message)
        },
    )

    private val offlineController: MapOfflineController = MapOfflineController(
        state = _uiState,
        scope = viewModelScope,
        offlineRepository = offlineRepository,
        offlineDownloadManager = offlineDownloadManager,
        telemetry = telemetry,
        featureConfig = runtime.featureConfig,
        prepareOfflineRoutePack = runtime.useCases.prepareOfflineRoutePack,
    )

    init {
        loadInitialScene()
        guidanceController.restoreLanguagePreference()
        locationController.observe()
        offlineController.observe()
    }

    fun selectStyle(style: MapStyleConfig) {
        _uiState.update { current ->
            current.copy(
                selectedStyle = style,
                lastAction = MapUiMessage.StyleChanged(style.title),
            )
        }
    }

    fun onMapClicked(point: GeoPoint) {
        val distanceFromRouteStartKm = runtime.useCases.buildRoutePresentation
            .distanceFromRouteStartKm(routePoints = _uiState.value.routePoints, point = point)

        _uiState.update { current ->
            current.copy(
                selectedPoint = point,
                routingState = RoutingUiState.Idle,
                lastAction = MapUiMessage.PointSelected(
                    point = point,
                    distanceFromRouteStartKm = distanceFromRouteStartKm,
                ),
            )
        }
    }

    fun clearSelectedPoint() {
        _uiState.update { current ->
            current.copy(
                selectedPoint = null,
                routingState = RoutingUiState.Idle,
                lastAction = MapUiMessage.SelectionCleared,
            )
        }
    }

    fun increaseSimplification() =
        updateSimplification((_uiState.value.simplificationToleranceMeters + 10.0).coerceAtMost(120.0))

    fun decreaseSimplification() =
        updateSimplification((_uiState.value.simplificationToleranceMeters - 10.0).coerceAtLeast(0.0))

    fun onLocationPermissionChanged(permissionLevel: LocationPermissionLevel, autoStart: Boolean) =
        locationController.onPermissionChanged(permissionLevel, autoStart)

    fun startLocationTracking() = locationController.startTracking()

    fun stopLocationTracking() = locationController.stopTracking()

    fun toggleFollowUserLocation() = locationController.toggleFollowUserLocation()

    fun calculateRouteToSelectedPoint(useCurrentLocation: Boolean = true) =
        routeController.calculateRouteToSelectedPoint(preferCurrentLocation = useCurrentLocation)

    fun selectRouteAlternative(routeId: String) = routeController.selectRouteAlternative(routeId)

    fun resetReferenceRoute() = routeController.resetReferenceRoute()

    fun startNavigation() = navigationController.startNavigation()

    fun stopNavigation() = navigationController.stopNavigation()

    fun toggleVoiceGuidanceMuted() = guidanceController.toggleMuted()

    fun setGuidanceLanguage(language: GuidanceLanguage) = guidanceController.setLanguage(language)

    fun increaseGuidanceVolume() = guidanceController.increaseVolume()

    fun decreaseGuidanceVolume() = guidanceController.decreaseVolume()

    fun testVoiceGuidance() = guidanceController.testVoiceGuidance()

    fun refreshOfflineRegions() = offlineController.refreshRegions()

    fun downloadCurrentRouteOffline() = offlineController.downloadCurrentRoute()

    fun pauseOfflineRegion(clientId: String) = offlineController.pauseRegion(clientId)

    fun resumeOfflineRegion(clientId: String) = offlineController.resumeRegion(clientId)

    fun deleteOfflineRegion(clientId: String) = offlineController.deleteRegion(clientId)

    fun clearAmbientCache() = offlineController.clearAmbientCache()

    fun packOfflineDatabase() = offlineController.packDatabase()

    private fun loadInitialScene() {
        runCatching {
            val scene = runtime.useCases.loadInitialMapScene(_uiState.value.simplificationToleranceMeters)
            _uiState.value = MapUiState(
                loadState = MapLoadState.Ready,
                styles = scene.styles,
                selectedStyle = scene.selectedStyle,
                referenceRoutePoints = scene.referenceRoutePoints,
                routePoints = scene.referenceRoutePoints,
                simplifiedRoutePoints = scene.simplifiedRoutePoints,
                remainingRoutePoints = scene.referenceRoutePoints,
                pois = scene.pois,
                projectedPoint = scene.projectedPoint,
                metrics = scene.metrics,
                lastAction = MapUiMessage.NativeGeoEngineReady,
            )
        }.onFailure { throwable ->
            telemetry.record(TelemetryArea.LoadInitialScene, throwable)
            _uiState.value = MapUiState(loadState = MapLoadState.Error(MapUiMessage.LoadFailed(throwable.message)))
        }
    }

    private fun updateSimplification(toleranceMeters: Double) {
        val presentation = runtime.useCases.buildRoutePresentation(
            points = _uiState.value.routePoints,
            simplificationToleranceMeters = toleranceMeters,
        ) ?: return

        _uiState.update {
            it.copy(
                simplificationToleranceMeters = toleranceMeters,
                simplifiedRoutePoints = presentation.simplifiedRoutePoints,
                metrics = presentation.metrics,
                lastAction = MapUiMessage.SimplificationChanged(toleranceMeters.toInt()),
            )
        }
    }

    override fun onCleared() {
        routeController.cancel()
        locationController.close()
        routingRepository.close()
        offlineController.close()
        guidanceController.close()
        navigationController.close()
        super.onCleared()
    }
}
