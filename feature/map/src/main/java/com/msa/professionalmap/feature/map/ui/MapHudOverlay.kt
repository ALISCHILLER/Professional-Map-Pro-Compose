@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.messageText
import com.msa.professionalmap.feature.map.presentation.MapUiState
import com.msa.professionalmap.feature.map.presentation.RoutingUiState

@Composable
internal fun MapHudOverlay(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    onRequestLocationPermission: () -> Unit,
    onStartLocation: () -> Unit,
    onCalculateRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onDownloadOfflineRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MapGlassPanel(
        modifier = modifier.widthIn(max = MapUiLayout.HudMaxWidth),
        contentDescription = strings.appTitle,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MapUiTokens.MediumGap),
        ) {
            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = strings.messageText(state.lastAction) ?: strings.ready,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MapSystemStatusStrip(
                state = state,
                strings = strings,
                permissionLevel = permissionLevel,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
                verticalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
            ) {
                StatusPill(text = state.selectedStyle?.title ?: strings.sectionFoundation)
                StatusPill(text = strings.tolerance(state.simplificationToleranceMeters.toInt()))
                state.metrics?.let { metrics ->
                    StatusPill(text = strings.routeMetricKm(metrics.totalDistanceKm))
                }
            }
            MapRouteFocusCard(state = state, strings = strings)
            MapQuickActionBar {
                if (permissionLevel == LocationPermissionLevel.None) {
                    MapPrimaryAction(label = strings.enableGps, onClick = onRequestLocationPermission)
                } else if (!state.locationTrackingState.isTracking) {
                    MapSecondaryAction(label = strings.startGps, onClick = onStartLocation)
                }
                MapPrimaryAction(
                    label = strings.routeHere,
                    enabled = state.selectedPoint != null && state.routingState !is RoutingUiState.Loading,
                    onClick = onCalculateRoute,
                )
                MapSecondaryAction(
                    label = strings.startNavigation,
                    enabled = state.canStartNavigation,
                    onClick = onStartNavigation,
                )
                MapSecondaryAction(
                    label = strings.downloadRoute,
                    enabled = state.routePoints.size >= 2,
                    onClick = onDownloadOfflineRoute,
                )
            }
        }
    }
}
