package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.guidance.domain.GuidanceState
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.location.LocationTrackingState
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteMetrics
import com.msa.professionalmap.core.offline.domain.OfflineManagerState
import com.msa.professionalmap.core.offline.domain.OfflineWorkManagerState
import com.msa.professionalmap.core.progress.domain.ProgressState

sealed interface MapLoadState {
    data object Idle : MapLoadState
    data object Loading : MapLoadState
    data object Ready : MapLoadState
    data class Error(val message: MapUiMessage) : MapLoadState
}

sealed interface RoutingUiState {
    data object Idle : RoutingUiState
    data object Loading : RoutingUiState
    data class Success(val message: MapUiMessage) : RoutingUiState
    data class Error(val message: MapUiMessage) : RoutingUiState
}

data class MapUiState(
    val loadState: MapLoadState = MapLoadState.Idle,
    val styles: List<MapStyleConfig> = emptyList(),
    val selectedStyle: MapStyleConfig? = null,
    val referenceRoutePoints: List<GeoPoint> = emptyList(),
    val routePoints: List<GeoPoint> = emptyList(),
    val simplifiedRoutePoints: List<GeoPoint> = emptyList(),
    val completedRoutePoints: List<GeoPoint> = emptyList(),
    val remainingRoutePoints: List<GeoPoint> = emptyList(),
    val simplificationToleranceMeters: Double = 18.0,
    val pois: List<MapPoi> = emptyList(),
    val selectedPoint: GeoPoint? = null,
    val projectedPoint: GeoPoint? = null,
    val currentLocation: DeviceLocation? = null,
    val snappedLocation: GeoPoint? = null,
    val locationTrackingState: LocationTrackingState = LocationTrackingState(),
    val followUserLocation: Boolean = false,
    val metrics: RouteMetrics? = null,
    val routingState: RoutingUiState = RoutingUiState.Idle,
    val routeAlternatives: List<RouteAlternative> = emptyList(),
    val selectedRouteAlternativeId: String? = null,
    val navigationActive: Boolean = false,
    val progressState: ProgressState = ProgressState.Idle,
    val guidanceConfig: GuidanceConfig = GuidanceConfig(),
    val guidanceState: GuidanceState = GuidanceState(),
    val offlineState: OfflineManagerState = OfflineManagerState(),
    val offlineWorkState: OfflineWorkManagerState = OfflineWorkManagerState(),
    val firstOffRouteTimestampMillis: Long? = null,
    val lastAction: MapUiMessage? = null,
) {
    val selectedRouteAlternative: RouteAlternative?
        get() = routeAlternatives.firstOrNull { it.id == selectedRouteAlternativeId }

    val canStartNavigation: Boolean
        get() = currentLocation != null && selectedRouteAlternative.canNavigate(routePoints)

    val effectiveCompletedRoutePoints: List<GeoPoint>
        get() = completedRoutePoints.takeIf { it.size >= 2 }.orEmpty()

    val effectiveRemainingRoutePoints: List<GeoPoint>
        get() = remainingRoutePoints.takeIf { it.size >= 2 } ?: routePoints
}

private fun RouteAlternative?.canNavigate(routePoints: List<GeoPoint>): Boolean =
    this?.isNavigationEligible ?: (routePoints.size >= 2)
