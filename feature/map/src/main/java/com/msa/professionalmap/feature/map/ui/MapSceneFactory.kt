package com.msa.professionalmap.feature.map.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState

/** Builds the heavy MapLibre render model only when map-relevant state changes. */
@Composable
internal fun rememberMapScene(
    state: MapUiState,
    strings: MapStrings,
): MapScene {
    val localizedPois = remember(state.pois, strings.language) {
        state.pois.map { poi -> poi.localizedForMap(strings) }
    }
    val selectedPoi = remember(localizedPois, state.selectedPoiId) {
        localizedPois.firstOrNull { it.id == state.selectedPoiId }
    }
    val selectedRoute = remember(state.routeAlternatives, state.selectedRouteAlternativeId) {
        state.routeAlternatives.firstOrNull { it.id == state.selectedRouteAlternativeId }
    }
    val activeRoutePoints = remember(selectedRoute, state.routePoints) {
        selectedRoute?.points ?: state.routePoints
    }
    val maneuverPoints = remember(selectedRoute) {
        MapManeuverMarkerReducer.reduce(selectedRoute)
    }
    val routeIdentity = remember(state.selectedRouteAlternativeId, state.routePoints) {
        state.selectedRouteAlternativeId
            ?: "reference-${state.routePoints.hashCode().toUInt().toString(16)}"
    }
    val instructionPoint = state.progressState.progressOrNull()
        ?.nextInstruction
        ?.sourceInstruction
        ?.location

    return remember(
        state.selectedStyle,
        routeIdentity,
        state.routePoints,
        state.routeAlternatives,
        state.selectedRouteAlternativeId,
        selectedRoute,
        activeRoutePoints,
        maneuverPoints,
        state.simplifiedRoutePoints,
        state.effectiveCompletedRoutePoints,
        state.effectiveRemainingRoutePoints,
        localizedPois,
        state.selectedPoiId,
        selectedPoi,
        state.selectedPoint,
        state.projectedPoint,
        state.currentLocation,
        state.snappedLocation,
        instructionPoint,
        state.followUserLocation,
        state.navigationActive,
    ) {
        MapScene(
            style = checkNotNull(state.selectedStyle),
            routeIdentity = routeIdentity,
            routePoints = state.routePoints,
            routeAlternatives = state.routeAlternatives,
            selectedRouteAlternativeId = state.selectedRouteAlternativeId,
            selectedRoute = selectedRoute,
            activeRoutePoints = activeRoutePoints,
            maneuverPoints = maneuverPoints,
            simplifiedRoutePoints = state.simplifiedRoutePoints,
            completedRoutePoints = state.effectiveCompletedRoutePoints,
            remainingRoutePoints = state.effectiveRemainingRoutePoints,
            pois = localizedPois,
            selectedPoiId = state.selectedPoiId,
            selectedPoi = selectedPoi,
            selectedPoint = state.selectedPoint,
            projectedPoint = state.projectedPoint,
            currentLocation = state.currentLocation,
            snappedLocation = state.snappedLocation,
            currentInstructionPoint = instructionPoint,
            followUserLocation = state.followUserLocation,
            navigationActive = state.navigationActive,
        )
    }
}
