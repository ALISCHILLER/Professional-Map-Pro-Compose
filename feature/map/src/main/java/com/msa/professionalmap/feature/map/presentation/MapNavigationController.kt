package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.ReroutingStrategy
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig
import com.msa.professionalmap.core.service.domain.NavigationServiceController
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import com.msa.professionalmap.feature.map.domain.BuildNavigationSnapshotUseCase
import com.msa.professionalmap.feature.map.domain.MapFeatureTelemetry
import com.msa.professionalmap.feature.map.domain.NavigationProgressInput
import com.msa.professionalmap.feature.map.domain.StartNavigationDecision
import com.msa.professionalmap.feature.map.domain.StartNavigationInput
import com.msa.professionalmap.feature.map.domain.StartNavigationUseCase
import com.msa.professionalmap.feature.map.domain.RerouteSource
import com.msa.professionalmap.feature.map.domain.UpdateNavigationProgressUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns active-navigation orchestration for the map screen.
 *
 * Route progress, service snapshots, reroute scheduling and completion handling are
 * intentionally kept outside MapViewModel. The ViewModel remains a thin intent API,
 * while this controller coordinates the stateful navigation workflow for one screen
 * instance.
 */
internal class MapNavigationController(
    private val state: MutableStateFlow<MapUiState>,
    private val scope: CoroutineScope,
    private val progressConfig: RouteProgressConfig,
    private val reroutingStrategy: ReroutingStrategy,
    private val startNavigationUseCase: StartNavigationUseCase,
    private val updateNavigationProgressUseCase: UpdateNavigationProgressUseCase,
    private val buildNavigationSnapshot: BuildNavigationSnapshotUseCase,
    private val navigationServiceController: NavigationServiceController,
    private val guidanceController: MapGuidanceController,
    private val telemetry: MapFeatureTelemetry,
    private val requestRoute: (origin: GeoPoint, destination: GeoPoint, keepNavigationActive: Boolean) -> Unit,
) {
    private var reroutingJob: Job? = null

    fun startNavigation() {
        val current = state.value
        val decision = startNavigationUseCase(
            StartNavigationInput(
                selectedRoute = current.selectedRouteAlternative,
                routePoints = current.routePoints,
                metrics = current.metrics,
                currentLocation = current.currentLocation,
            )
        )
        when (decision) {
            StartNavigationDecision.MissingNavigableRoute -> {
                state.update { it.copy(lastAction = MapUiMessage.RouteBeforeNavigation) }
                return
            }
            StartNavigationDecision.MissingCurrentLocation -> {
                state.update { it.copy(lastAction = MapUiMessage.StartGpsBeforeNavigation) }
                return
            }
            is StartNavigationDecision.Ready -> startNavigation(decision, current)
        }
    }

    fun stopNavigation() {
        telemetry.navigationCancelled()
        stopNavigationInternal(action = MapUiMessage.NavigationStopped)
    }

    fun stopNavigationInternal(action: MapUiMessage) {
        reroutingJob?.cancel()
        navigationServiceController.stop()
        resetProgressRuntimeState(cancelReroute = true)
        telemetry.setNavigationActive(false)
        state.update {
            it.copy(
                followUserLocation = false,
                navigationActive = false,
                progressState = ProgressState.Idle,
                completedRoutePoints = emptyList(),
                remainingRoutePoints = it.routePoints,
                snappedLocation = null,
                firstOffRouteTimestampMillis = null,
                lastAction = action,
            )
        }
    }

    fun updateProgress(location: DeviceLocation) {
        val current = state.value
        val update = updateNavigationProgressUseCase.execute(
            input = NavigationProgressInput(
                navigationActive = current.navigationActive,
                selectedRoute = current.selectedRouteAlternative,
                routePoints = current.routePoints,
                metrics = current.metrics,
                firstOffRouteTimestampMillis = current.firstOffRouteTimestampMillis,
            ),
            location = location,
        ) ?: return

        val progressState = update.progressState
        if (update.isFirstOffRouteEvent && progressState is ProgressState.OffRoute) {
            telemetry.offRouteDetected(progressState.distanceFromRouteMeters)
        }

        state.update {
            it.copy(
                progressState = progressState,
                completedRoutePoints = update.split.completedPoints,
                remainingRoutePoints = update.split.remainingPoints,
                snappedLocation = update.progress?.matchedLocation?.snappedLocation,
                firstOffRouteTimestampMillis = update.firstOffRouteTimestampMillis,
                lastAction = progressState.toUiMessage() ?: it.lastAction,
            )
        }
        navigationServiceController.update(
            serviceSnapshot(
                state = state.value,
                instructionOverride = progressState.toServiceText(state.value.guidanceConfig.language),
            )
        )

        guidanceController.handleProgress(
            progressState = progressState,
            speedMetersPerSecond = location.speedMetersPerSecond?.toDouble(),
            timestampMillis = location.timestampMillis,
        )

        when (progressState) {
            is ProgressState.Arrived -> finishNavigation(progressState)
            is ProgressState.OffRoute -> maybeScheduleReroute(location.timestampMillis)
            else -> reroutingJob?.cancel()
        }
    }

    fun resetProgressRuntimeState(cancelReroute: Boolean) {
        if (cancelReroute) reroutingJob?.cancel()
        updateNavigationProgressUseCase.reset()
    }

    fun close() {
        reroutingJob?.cancel()
        navigationServiceController.stop()
    }

    private fun startNavigation(decision: StartNavigationDecision.Ready, current: MapUiState) {
        resetProgressRuntimeState(cancelReroute = true)
        guidanceController.resetForCurrentConfig()
        state.update {
            it.copy(
                navigationActive = true,
                lastAction = MapUiMessage.NavigationStarted,
            )
        }
        telemetry.navigationStarted(routeDistanceMeters = decision.routeDistanceMeters, hasGps = true)
        navigationServiceController.start(
            serviceSnapshot(
                state = state.value,
                instructionOverride = MapUiMessageServiceFormatter.format(
                    MapUiMessage.NavigationStarted,
                    current.guidanceConfig.language,
                ),
                status = NavigationServiceStatus.Active,
            )
        )
        updateProgress(decision.currentLocation)
    }

    private fun finishNavigation(progressState: ProgressState.Arrived) {
        telemetry.navigationCompleted(progressState.progress.remainingDistanceMeters)
        reroutingJob?.cancel()
        navigationServiceController.stop()
        resetProgressRuntimeState(cancelReroute = true)
        state.update { it.copy(navigationActive = false, lastAction = MapUiMessage.ArrivedAtDestination) }
    }

    private fun maybeScheduleReroute(nowMillis: Long) {
        val current = state.value
        val destination = current.selectedPoint ?: current.routePoints.lastOrNull() ?: return
        val location = current.currentLocation ?: return
        if (!reroutingStrategy.shouldReroute(current.firstOffRouteTimestampMillis, nowMillis, isCurrentlyOffRoute = true)) {
            scheduleDebouncedReroute(destination)
            return
        }
        if (reroutingJob?.isActive != true) {
            telemetry.rerouteTriggered(RerouteSource.Immediate)
            requestRoute(location.position, destination, true)
        }
    }

    private fun scheduleDebouncedReroute(destination: GeoPoint) {
        if (reroutingJob?.isActive == true) return
        reroutingJob = scope.launch {
            delay(progressConfig.rerouteDebounceMillis)
            val refreshed = state.value
            val refreshedLocation = refreshed.currentLocation ?: return@launch
            val stillOffRoute = refreshed.progressState is ProgressState.OffRoute
            if (stillOffRoute && refreshed.navigationActive) {
                telemetry.rerouteTriggered(RerouteSource.Debounced)
                requestRoute(refreshedLocation.position, destination, true)
            }
        }
    }

    private fun serviceSnapshot(
        state: MapUiState,
        instructionOverride: String? = null,
        status: NavigationServiceStatus = if (state.navigationActive) NavigationServiceStatus.Active else NavigationServiceStatus.Idle,
    ): NavigationServiceSnapshot {
        return buildNavigationSnapshot(
            destination = state.selectedPoint,
            routePoints = state.routePoints,
            metrics = state.metrics,
            progressState = state.progressState,
            selectedRoute = state.selectedRouteAlternative,
            lastMessage = state.serviceMessage(),
            instructionOverride = instructionOverride,
            language = state.guidanceConfig.language,
            status = status,
        )
    }
}
