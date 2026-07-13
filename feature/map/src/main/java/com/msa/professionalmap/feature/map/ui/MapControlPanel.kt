@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.mapStyleTitle
import com.msa.professionalmap.feature.map.presentation.MapUiState

@Composable
internal fun MapControlPanel(
    state: MapUiState,
    strings: MapStrings,
    appLanguage: AppLanguage,
    themeMode: AppThemeMode,
    permissionLevel: LocationPermissionLevel,
    actions: MapActionHandlers,
    modifier: Modifier = Modifier,
    maxPanelHeight: Dp = 520.dp,
    compact: Boolean = false,
) {
    MapGlassPanel(modifier = modifier, contentDescription = strings.sectionFoundation) {
        LazyColumn(
            modifier = Modifier.heightIn(max = maxPanelHeight),
            verticalArrangement = Arrangement.spacedBy(MapUiTokens.MediumGap),
        ) {
            item(key = "handle") { MapPanelHandle() }
            if (!compact) {
                item(key = "header") {
                    MapControlPanelHeader(state, strings, permissionLevel)
                }
                item(key = "appearance") {
                    MapAppearanceSettingsCard(
                        strings = strings,
                        appLanguage = appLanguage,
                        themeMode = themeMode,
                        onLanguageSelected = actions.onAppLanguageSelected,
                        onThemeModeSelected = actions.onThemeModeSelected,
                    )
                }
            }
            item(key = "task-flow") {
                MapTaskFlowCard(
                    state = state,
                    strings = strings,
                    permissionLevel = permissionLevel,
                    compact = compact,
                )
            }
            if (!compact) {
                item(key = "styles") {
                    StyleSelector(state, strings, actions.onStyleSelected)
                }
                item(key = "metrics") {
                    MetricsRow(state.metrics, strings)
                }
            }
            item(key = "location") {
                LocationPanel(
                    strings = strings,
                    permissionLevel = permissionLevel,
                    trackingState = state.locationTrackingState,
                    currentLocation = state.currentLocation,
                    followUserLocation = state.followUserLocation,
                    onRequestLocationPermission = actions.onRequestLocationPermission,
                    onStartLocation = actions.onStartLocation,
                    onStopLocation = actions.onStopLocation,
                    onToggleFollowUser = actions.onToggleFollowUser,
                )
            }
            item(key = "routing") {
                RoutingPanel(
                    strings = strings,
                    selectedPoint = state.selectedPoint,
                    routingState = state.routingState,
                    alternatives = state.routeAlternatives,
                    selectedAlternativeId = state.selectedRouteAlternativeId,
                    selectedAlternative = state.selectedRouteAlternative,
                    onCalculateRoute = actions.onCalculateRoute,
                    onSelectRouteAlternative = actions.onSelectRouteAlternative,
                    onResetReferenceRoute = actions.onResetReferenceRoute,
                )
            }
            item(key = "navigation") {
                NavigationPanel(
                    strings = strings,
                    progressState = state.progressState,
                    navigationActive = state.navigationActive,
                    navigationPaused = state.navigationStatus ==
                        com.msa.professionalmap.core.service.domain.NavigationServiceStatus.Paused,
                    canStartNavigation = state.canStartNavigation,
                    hasNavigableRoute = state.routePoints.size >= 2,
                    hasCurrentLocation = state.currentLocation != null,
                    onStartNavigation = actions.onStartNavigation,
                    onPauseNavigation = actions.onPauseNavigation,
                    onResumeNavigation = actions.onResumeNavigation,
                    onStopNavigation = actions.onStopNavigation,
                )
            }
            item(key = "guidance") {
                GuidancePanel(
                    strings = strings,
                    guidanceState = state.guidanceState,
                    selectedLanguage = state.guidanceConfig.language,
                    volume = state.guidanceConfig.volume,
                    muted = state.guidanceConfig.muted,
                    onToggleMuted = actions.onToggleGuidanceMuted,
                    onLanguageSelected = actions.onGuidanceLanguageSelected,
                    onIncreaseVolume = actions.onIncreaseGuidanceVolume,
                    onDecreaseVolume = actions.onDecreaseGuidanceVolume,
                    onTestGuidance = actions.onTestGuidance,
                )
            }
            if (!compact) {
                item(key = "offline") {
                    OfflinePanel(
                        strings = strings,
                        offlineState = state.offlineState,
                        offlineWorkState = state.offlineWorkState,
                        onRefreshOffline = actions.onRefreshOffline,
                        onDownloadOfflineRoute = actions.onDownloadOfflineRoute,
                        onPauseOffline = actions.onPauseOffline,
                        onResumeOffline = actions.onResumeOffline,
                        onDeleteOffline = actions.onDeleteOffline,
                        onClearAmbientCache = actions.onClearAmbientCache,
                        onPackOfflineDatabase = actions.onPackOfflineDatabase,
                    )
                }
            }
            item(key = "footer-actions") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MapSecondaryAction(strings.lessSimplify, actions.onDecreaseSimplification)
                    MapPrimaryAction(strings.moreSimplify, actions.onIncreaseSimplification)
                    if (state.selectedPoint != null) {
                        MapSecondaryAction(strings.clear, actions.onClearSelection)
                    }
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
                    leadingIcon = {
                        MapGlyphIcon(
                            glyph = MapGlyph.Map,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            size = 16.dp,
                        )
                    },
                    label = { Text(strings.mapStyleTitle(style)) },
                )
            }
            StatusPill(text = strings.tolerance(state.simplificationToleranceMeters.toInt()))
        }
    }
}
