@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState

@Composable
internal fun MapControlPanel(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    onStyleSelected: (MapStyleConfig) -> Unit,
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
    modifier: Modifier = Modifier,
    maxPanelHeight: Dp = 520.dp,
) {
    MapGlassPanel(
        modifier = modifier,
        contentDescription = strings.sectionFoundation,
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = maxPanelHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MapUiTokens.MediumGap),
        ) {
            MapPanelHandle()
            MapControlPanelHeader(
                state = state,
                strings = strings,
                permissionLevel = permissionLevel,
            )

            StyleSelector(state = state, strings = strings, onStyleSelected = onStyleSelected)
            MetricsRow(metrics = state.metrics, strings = strings)

            LocationPanel(
                strings = strings,
                permissionLevel = permissionLevel,
                trackingState = state.locationTrackingState,
                currentLocation = state.currentLocation,
                followUserLocation = state.followUserLocation,
                onRequestLocationPermission = onRequestLocationPermission,
                onStartLocation = onStartLocation,
                onStopLocation = onStopLocation,
                onToggleFollowUser = onToggleFollowUser,
            )
            RoutingPanel(
                strings = strings,
                selectedPoint = state.selectedPoint,
                routingState = state.routingState,
                alternatives = state.routeAlternatives,
                selectedAlternativeId = state.selectedRouteAlternativeId,
                selectedAlternative = state.selectedRouteAlternative,
                onCalculateRoute = onCalculateRoute,
                onSelectRouteAlternative = onSelectRouteAlternative,
                onResetReferenceRoute = onResetReferenceRoute,
            )
            NavigationPanel(
                strings = strings,
                progressState = state.progressState,
                navigationActive = state.navigationActive,
                canStartNavigation = state.canStartNavigation,
                onStartNavigation = onStartNavigation,
                onStopNavigation = onStopNavigation,
            )
            GuidancePanel(
                strings = strings,
                guidanceState = state.guidanceState,
                selectedLanguage = state.guidanceConfig.language,
                volume = state.guidanceConfig.volume,
                muted = state.guidanceConfig.muted,
                onToggleMuted = onToggleGuidanceMuted,
                onLanguageSelected = onGuidanceLanguageSelected,
                onIncreaseVolume = onIncreaseGuidanceVolume,
                onDecreaseVolume = onDecreaseGuidanceVolume,
                onTestGuidance = onTestGuidance,
            )
            OfflinePanel(
                strings = strings,
                offlineState = state.offlineState,
                offlineWorkState = state.offlineWorkState,
                onRefreshOffline = onRefreshOffline,
                onDownloadOfflineRoute = onDownloadOfflineRoute,
                onPauseOffline = onPauseOffline,
                onResumeOffline = onResumeOffline,
                onDeleteOffline = onDeleteOffline,
                onClearAmbientCache = onClearAmbientCache,
                onPackOfflineDatabase = onPackOfflineDatabase,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapSecondaryAction(label = strings.lessSimplify, onClick = onDecreaseSimplification)
                MapPrimaryAction(label = strings.moreSimplify, onClick = onIncreaseSimplification)
                if (state.selectedPoint != null) {
                    MapSecondaryAction(label = strings.clear, onClick = onClearSelection)
                }
            }
        }
    }
}

@Composable
private fun StyleSelector(
    state: MapUiState,
    strings: MapStrings,
    onStyleSelected: (MapStyleConfig) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.styles.forEach { style ->
                FilterChip(
                    selected = style.id == state.selectedStyle?.id,
                    onClick = { onStyleSelected(style) },
                    label = { Text(style.title) },
                )
            }
            StatusPill(text = strings.tolerance(state.simplificationToleranceMeters.toInt()))
        }
    }
}
