package com.msa.professionalmap.feature.map.presentation
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.observability.domain.AppTrace
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.routing.routingErrorCode
import com.msa.professionalmap.feature.map.domain.MapFeatureConfig
import com.msa.professionalmap.feature.map.domain.MapFeatureTelemetry
import com.msa.professionalmap.feature.map.domain.MapUseCases
import com.msa.professionalmap.feature.map.domain.RouteCalculationApplicationInput
import com.msa.professionalmap.feature.map.domain.RouteCalculationApplicationResult
import com.msa.professionalmap.feature.map.domain.RouteRequestDecision
import com.msa.professionalmap.feature.map.domain.RoutePresentation
import com.msa.professionalmap.feature.map.domain.RouteRequestInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
internal class MapRouteController(
    private val state: MutableStateFlow<MapUiState>,
    private val scope: CoroutineScope,
    private val featureConfig: MapFeatureConfig,
    private val useCases: MapUseCases,
    private val telemetry: MapFeatureTelemetry,
    private val onRouteMutationRequested: () -> Unit,
) {
    private var routingJob: Job? = null
    fun calculateRouteToSelectedPoint(preferCurrentLocation: Boolean) {
        onRouteMutationRequested()
        when (val decision = resolveRouteRequest(preferCurrentLocation)) {
            RouteRequestDecision.MissingDestination -> routeRequestError(MapUiMessage.SelectDestinationFirst)
            RouteRequestDecision.MissingOrigin -> routeRequestError(MapUiMessage.NoOriginAvailable)
            is RouteRequestDecision.Ready -> requestRoute(
                origin = decision.origin,
                destination = decision.destination,
                keepNavigationActive = false,
                originLocation = decision.originLocation,
            )
        }
    }
    fun selectRouteAlternative(routeId: String) {
        onRouteMutationRequested()
        val current = state.value
        val alternative = current.routeAlternatives.firstOrNull { it.id == routeId } ?: return
        val presentation = useCases.buildRoutePresentation.fromAlternative(
            alternative = alternative,
            simplificationToleranceMeters = current.simplificationToleranceMeters,
        )
        applyRouteAlternative(
            alternative = alternative,
            allAlternatives = current.routeAlternatives,
            presentation = presentation,
            action = MapUiMessage.RouteAlternativeSelected(alternative.title, alternative.distanceKm, alternative.durationMinutes),
        )
    }
    fun resetReferenceRoute() {
        cancel()
        onRouteMutationRequested()
        val referenceRoute = state.value.referenceRoutePoints
        if (referenceRoute.size < 2) return
        val presentation = useCases.buildRoutePresentation(
            points = referenceRoute,
            simplificationToleranceMeters = state.value.simplificationToleranceMeters,
        ) ?: return
        state.update {
            it.copy(
                routePoints = presentation.routePoints,
                simplifiedRoutePoints = presentation.simplifiedRoutePoints,
                completedRoutePoints = emptyList(),
                remainingRoutePoints = presentation.routePoints,
                snappedLocation = null,
                metrics = presentation.metrics,
                routeAlternatives = emptyList(),
                selectedRouteAlternativeId = null,
                routingState = RoutingUiState.Idle,
                navigationActive = false,
                progressState = ProgressState.Idle,
                firstOffRouteTimestampMillis = null,
                lastAction = MapUiMessage.ReferenceRouteRestored,
            )
        }
    }
    fun requestRoute(
        origin: GeoPoint,
        destination: GeoPoint,
        keepNavigationActive: Boolean,
        originLocation: DeviceLocation? = null,
    ) {
        cancel()
        val previousProgressState = state.value.progressState
        val previousNavigationActive = state.value.navigationActive
        routingJob = scope.launch {
            state.update {
                it.copy(
                    routingState = RoutingUiState.Loading,
                    progressState = if (keepNavigationActive) {
                        ProgressState.Rerouting(lastKnownProgress = it.progressState.routeProgressOrNull())
                    } else {
                        it.progressState
                    },
                    lastAction = if (keepNavigationActive) MapUiMessage.Rerouting else MapUiMessage.CalculatingRoute,
                )
            }
            delay(featureConfig.routeRequestDebounceMillis)
            val trace = telemetry.startRouteCalculationTrace()
            try {
                val outcome = useCases.calculateRoute(
                    origin = origin,
                    destination = destination,
                    originLocation = originLocation,
                )
                val application = useCases.applyRouteCalculationOutcome(
                    RouteCalculationApplicationInput(
                        outcome = outcome,
                        simplificationToleranceMeters = state.value.simplificationToleranceMeters,
                        allowPreviewFallback = !keepNavigationActive,
                    )
                )
                applyRouteCalculation(application, keepNavigationActive, previousProgressState, previousNavigationActive, trace)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                val message = MapUiMessage.ExternalError("routing", throwable.routingErrorCode().name)
                telemetry.routeCalculationFailed(throwable)
                state.update {
                    it.copy(
                        routingState = RoutingUiState.Error(message),
                        navigationActive = previousNavigationActive,
                        progressState = previousProgressState,
                        lastAction = message,
                    )
                }
            } finally {
                trace.stop()
            }
        }
    }
    fun cancel() {
        routingJob?.cancel()
    }
    private fun resolveRouteRequest(preferCurrentLocation: Boolean): RouteRequestDecision {
        val current = state.value
        return useCases.resolveRouteRequest(
            RouteRequestInput(
                selectedDestination = current.selectedPoint,
                currentLocation = current.currentLocation,
                referenceRoutePoints = current.referenceRoutePoints,
                currentRoutePoints = current.routePoints,
                preferCurrentLocation = preferCurrentLocation,
            )
        )
    }
    private fun routeRequestError(message: MapUiMessage) {
        state.update { it.copy(routingState = RoutingUiState.Error(message), lastAction = message) }
    }
    private fun applyRouteCalculation(
        application: RouteCalculationApplicationResult,
        keepNavigationActive: Boolean,
        previousProgressState: ProgressState,
        previousNavigationActive: Boolean,
        trace: AppTrace,
    ) {
        when (application) {
            is RouteCalculationApplicationResult.ProviderRouteApplied -> {
                telemetry.routeCalculated(application.result, trace)
                applyRouteAlternative(
                    alternative = application.selectedRoute,
                    allAlternatives = application.alternatives,
                    presentation = application.presentation,
                    action = MapUiMessage.RouteReady(
                        provider = application.result.provider,
                        distanceKm = application.selectedRoute.distanceKm,
                        durationMinutes = application.selectedRoute.durationMinutes,
                    ),
                )
                if (keepNavigationActive) {
                    state.update { it.copy(navigationActive = previousNavigationActive, firstOffRouteTimestampMillis = null) }
                }
            }
            is RouteCalculationApplicationResult.FallbackPreviewApplied -> {
                telemetry.routeCalculationFailed(application.cause)
                applyRouteAlternative(
                    alternative = application.route,
                    allAlternatives = listOf(application.route),
                    presentation = application.presentation,
                    action = MapUiMessage.ProviderRouteFailedFallbackShown,
                )
                state.update {
                    it.copy(
                        routingState = RoutingUiState.Error(MapUiMessage.ExternalError("routing", application.cause.routingErrorCode().name)),
                        lastAction = MapUiMessage.ProviderRouteFailedFallbackShown,
                    )
                }
            }
            is RouteCalculationApplicationResult.ProviderFailedDuringActiveNavigation -> {
                telemetry.routeCalculationFailed(application.cause)
                state.update {
                    it.copy(
                        routingState = RoutingUiState.Error(MapUiMessage.ExternalError("routing", application.cause.routingErrorCode().name)),
                        navigationActive = previousNavigationActive,
                        progressState = previousProgressState,
                        lastAction = MapUiMessage.ExternalError("routing", application.cause.routingErrorCode().name),
                    )
                }
            }
        }
    }
    private fun applyRouteAlternative(
        alternative: RouteAlternative,
        allAlternatives: List<RouteAlternative>,
        presentation: RoutePresentation,
        action: MapUiMessage,
    ) {
        state.update {
            it.copy(
                routePoints = presentation.routePoints,
                simplifiedRoutePoints = presentation.simplifiedRoutePoints,
                completedRoutePoints = emptyList(),
                remainingRoutePoints = presentation.routePoints,
                snappedLocation = null,
                metrics = presentation.metrics,
                routeAlternatives = allAlternatives,
                selectedRouteAlternativeId = alternative.id,
                routingState = RoutingUiState.Success(action),
                progressState = ProgressState.Idle,
                firstOffRouteTimestampMillis = null,
                lastAction = action,
            )
        }
    }
}
