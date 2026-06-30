package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState

@Composable
internal fun ReadyMapContent(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    onStyleSelected: (MapStyleConfig) -> Unit,
    onMapClick: (GeoPoint) -> Unit,
    onClearSelection: () -> Unit,
    onIncreaseSimplification: () -> Unit,
    onDecreaseSimplification: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onStartLocation: () -> Unit,
    onStopLocation: () -> Unit,
    onToggleFollowUser: () -> Unit,
    onCalculateRoute: () -> Unit,
    onSelectRouteAlternative: (String) -> Unit,
    onResetReferenceRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onToggleGuidanceMuted: () -> Unit,
    onGuidanceLanguageSelected: (GuidanceLanguage) -> Unit,
    onIncreaseGuidanceVolume: () -> Unit,
    onDecreaseGuidanceVolume: () -> Unit,
    onTestGuidance: () -> Unit,
    onRefreshOffline: () -> Unit,
    onDownloadOfflineRoute: () -> Unit,
    onPauseOffline: (String) -> Unit,
    onResumeOffline: (String) -> Unit,
    onDeleteOffline: (String) -> Unit,
    onClearAmbientCache: () -> Unit,
    onPackOfflineDatabase: () -> Unit,
) {
    val style = state.selectedStyle ?: return

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expandedLayout = maxWidth >= MapUiLayout.ExpandedBreakpoint
        val controlPanelMaxHeight = if (expandedLayout) {
            maxHeight * MapUiLayout.ExpandedPanelHeightFraction
        } else {
            maxHeight * MapUiLayout.CompactPanelHeightFraction
        }

        MapLibreView(
            scene = MapScene(
                style = style,
                routePoints = state.routePoints,
                simplifiedRoutePoints = state.simplifiedRoutePoints,
                completedRoutePoints = state.effectiveCompletedRoutePoints,
                remainingRoutePoints = state.effectiveRemainingRoutePoints,
                pois = state.pois,
                selectedPoint = state.selectedPoint,
                projectedPoint = state.projectedPoint,
                currentLocation = state.currentLocation,
                snappedLocation = state.snappedLocation,
                currentInstructionPoint = state.progressState.progressOrNull()?.nextInstruction?.sourceInstruction?.location,
                followUserLocation = state.followUserLocation,
            ),
            onMapClick = onMapClick,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f),
                        ),
                    )
                ),
        )

        MapHudOverlay(
            state = state,
            strings = strings,
            permissionLevel = permissionLevel,
            onRequestLocationPermission = onRequestLocationPermission,
            onStartLocation = onStartLocation,
            onCalculateRoute = onCalculateRoute,
            onStartNavigation = onStartNavigation,
            onDownloadOfflineRoute = onDownloadOfflineRoute,
            modifier = if (expandedLayout) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        horizontal = MapUiLayout.ExpandedHorizontalPadding,
                        vertical = MapUiLayout.ExpandedHudVerticalPadding,
                    )
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        horizontal = MapUiLayout.CompactHorizontalPadding,
                        vertical = MapUiLayout.CompactHudVerticalPadding,
                    )
            },
        )

        MapControlPanel(
            state = state,
            strings = strings,
            permissionLevel = permissionLevel,
            maxPanelHeight = controlPanelMaxHeight,
            onStyleSelected = onStyleSelected,
            onClearSelection = onClearSelection,
            onIncreaseSimplification = onIncreaseSimplification,
            onDecreaseSimplification = onDecreaseSimplification,
            onRequestLocationPermission = onRequestLocationPermission,
            onStartLocation = onStartLocation,
            onStopLocation = onStopLocation,
            onToggleFollowUser = onToggleFollowUser,
            onCalculateRoute = onCalculateRoute,
            onSelectRouteAlternative = onSelectRouteAlternative,
            onResetReferenceRoute = onResetReferenceRoute,
            onStartNavigation = onStartNavigation,
            onStopNavigation = onStopNavigation,
            onToggleGuidanceMuted = onToggleGuidanceMuted,
            onGuidanceLanguageSelected = onGuidanceLanguageSelected,
            onIncreaseGuidanceVolume = onIncreaseGuidanceVolume,
            onDecreaseGuidanceVolume = onDecreaseGuidanceVolume,
            onTestGuidance = onTestGuidance,
            onRefreshOffline = onRefreshOffline,
            onDownloadOfflineRoute = onDownloadOfflineRoute,
            onPauseOffline = onPauseOffline,
            onResumeOffline = onResumeOffline,
            onDeleteOffline = onDeleteOffline,
            onClearAmbientCache = onClearAmbientCache,
            onPackOfflineDatabase = onPackOfflineDatabase,
            modifier = if (expandedLayout) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(MapUiLayout.ExpandedPanelFillFraction)
                    .widthIn(
                        min = MapUiLayout.ExpandedPanelMinWidth,
                        max = MapUiLayout.ExpandedPanelMaxWidth,
                    )
                    .padding(
                        horizontal = MapUiLayout.ExpandedHorizontalPadding,
                        vertical = MapUiLayout.ExpandedVerticalPadding,
                    )
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .widthIn(max = MapUiLayout.CompactPanelMaxWidth)
                    .navigationBarsPadding()
                    .padding(
                        horizontal = MapUiLayout.CompactHorizontalPadding,
                        vertical = MapUiLayout.CompactVerticalPadding,
                    )
            },
        )
    }
}
