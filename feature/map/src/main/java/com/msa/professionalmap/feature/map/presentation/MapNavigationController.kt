package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.guidance.domain.toLanguageTag
import com.msa.professionalmap.core.location.LocationConfig
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.service.domain.NavigationRoutingConfig
import com.msa.professionalmap.core.service.domain.NavigationRuntimeState
import com.msa.professionalmap.core.service.domain.NavigationServiceController
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import com.msa.professionalmap.core.service.domain.NavigationSession
import com.msa.professionalmap.feature.map.domain.MapFeatureConfig
import com.msa.professionalmap.feature.map.domain.MapFeatureTelemetry
import com.msa.professionalmap.feature.map.domain.StartNavigationDecision
import com.msa.professionalmap.feature.map.domain.StartNavigationInput
import com.msa.professionalmap.feature.map.domain.StartNavigationUseCase
import com.msa.professionalmap.feature.map.domain.TelemetryArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Presentation bridge for the service-owned navigation runtime. */
internal class MapNavigationController(
    private val state: MutableStateFlow<MapUiState>,
    private val scope: CoroutineScope,
    private val featureConfig: MapFeatureConfig,
    private val routingConfig: NavigationRoutingConfig,
    private val startNavigationUseCase: StartNavigationUseCase,
    private val navigationServiceController: NavigationServiceController,
    private val guidanceController: MapGuidanceController,
    private val telemetry: MapFeatureTelemetry,
) {
    private var observationJob: Job? = null
    private var lastStatus: NavigationServiceStatus = NavigationServiceStatus.Idle

    fun observe() {
        if (observationJob?.isActive == true) return
        observationJob = scope.launch {
            navigationServiceController.runtimeState.collectLatest(::applyRuntimeState)
        }
        navigationServiceController.restore()
    }

    fun startNavigation() {
        val current = state.value
        when (
            val decision = startNavigationUseCase(
                StartNavigationInput(
                    selectedRoute = current.selectedRouteAlternative,
                    routePoints = current.routePoints,
                    metrics = current.metrics,
                    currentLocation = current.currentLocation,
                )
            )
        ) {
            StartNavigationDecision.MissingNavigableRoute -> {
                state.update { it.copy(lastAction = MapUiMessage.RouteBeforeNavigation) }
            }
            StartNavigationDecision.MissingCurrentLocation -> {
                state.update { it.copy(lastAction = MapUiMessage.StartGpsBeforeNavigation) }
            }
            is StartNavigationDecision.Ready -> startNavigation(decision, current)
        }
    }

    fun pauseNavigation() = navigationServiceController.pause()

    fun resumeNavigation() {
        navigationServiceController.resume()
    }

    fun updateGuidance() {
        navigationServiceController.updateGuidance(state.value.guidanceConfig)
    }

    fun stopNavigation() {
        telemetry.navigationCancelled()
        navigationServiceController.stop()
        telemetry.setNavigationActive(false)
        state.update {
            it.copy(
                followUserLocation = false,
                navigationActive = false,
                navigationStatus = NavigationServiceStatus.Idle,
                progressState = ProgressState.Idle,
                completedRoutePoints = emptyList(),
                remainingRoutePoints = it.routePoints,
                snappedLocation = null,
                firstOffRouteTimestampMillis = null,
                lastAction = MapUiMessage.NavigationStopped,
            )
        }
    }

    fun stopNavigationInternal(action: MapUiMessage) {
        navigationServiceController.stop()
        telemetry.setNavigationActive(false)
        state.update {
            it.copy(
                followUserLocation = false,
                navigationActive = false,
                navigationStatus = NavigationServiceStatus.Idle,
                progressState = ProgressState.Idle,
                completedRoutePoints = emptyList(),
                remainingRoutePoints = it.routePoints,
                snappedLocation = null,
                firstOffRouteTimestampMillis = null,
                lastAction = action,
            )
        }
    }

    fun close() {
        // Deliberately do not stop the service. Navigation is independent of this ViewModel.
        observationJob?.cancel()
        observationJob = null
    }

    private fun startNavigation(
        decision: StartNavigationDecision.Ready,
        current: MapUiState,
    ) {
        val destinationTitle = if (current.guidanceConfig.language.toLanguageTag().startsWith("fa")) {
            "مقصد"
        } else {
            "Destination"
        }
        val session = NavigationSession(
            id = UUID.randomUUID().toString(),
            route = decision.route,
            destinationTitle = destinationTitle,
            languageTag = current.guidanceConfig.language.toLanguageTag(),
            guidanceConfig = current.guidanceConfig,
            routingConfig = routingConfig,
            locationConfig = LocationConfig(
                intervalMillis = featureConfig.locationUpdateIntervalMillis,
                minUpdateIntervalMillis = featureConfig.locationMinUpdateIntervalMillis,
                minDistanceMeters = featureConfig.locationMinDistanceMeters,
                waitForAccurateLocation = featureConfig.waitForAccurateLocation,
            ),
        )
        guidanceController.resetForCurrentConfig()
        telemetry.navigationStarted(routeDistanceMeters = decision.routeDistanceMeters, hasGps = true)
        state.update {
            it.copy(
                navigationActive = true,
                navigationStatus = NavigationServiceStatus.Starting,
                followUserLocation = true,
                routePoints = decision.route.points,
                remainingRoutePoints = decision.route.points,
                progressState = ProgressState.Idle,
                lastAction = MapUiMessage.NavigationStarted,
            )
        }
        navigationServiceController.start(session)
    }

    private fun applyRuntimeState(runtime: NavigationRuntimeState) {
        val wasActive = lastStatus == NavigationServiceStatus.Active ||
            lastStatus == NavigationServiceStatus.Starting ||
            lastStatus == NavigationServiceStatus.Rerouting ||
            lastStatus == NavigationServiceStatus.Paused
        val isActive = runtime.isRunning
        state.update { current -> runtime.applyToUiState(current, previouslyActive = wasActive) }

        when {
            runtime.status == NavigationServiceStatus.Completed && lastStatus != NavigationServiceStatus.Completed -> {
                telemetry.navigationCompleted(runtime.progressState.remainingDistanceMeters())
            }
            runtime.status == NavigationServiceStatus.Failed && lastStatus != NavigationServiceStatus.Failed -> {
                telemetry.setNavigationActive(false)
                telemetry.record(
                    TelemetryArea.NavigationRuntime,
                    IllegalStateException("navigation_runtime_failed:${runtime.errorCode?.name ?: "unknown"}"),
                )
            }
            isActive -> telemetry.setNavigationActive(true)
            wasActive -> telemetry.setNavigationActive(false)
        }
        lastStatus = runtime.status
    }


}
