@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.mapStyleTitle
import com.msa.professionalmap.feature.map.i18n.messageText
import com.msa.professionalmap.feature.map.presentation.MapUiState
import com.msa.professionalmap.feature.map.presentation.RoutingUiState

@Composable
internal fun MapExpandedHudOverlay(
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
    MapGlassPanel(modifier.widthIn(max = MapUiLayout.HudMaxWidth), strings.appTitle) {
        Column(verticalArrangement = Arrangement.spacedBy(MapUiTokens.MediumGap)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapGlyphBadge(
                    glyph = if (state.navigationActive) MapGlyph.Navigation else MapGlyph.Map,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 48.dp,
                    iconSize = 24.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        strings.appTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = strings.messageText(state.lastAction) ?: strings.ready,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            MapSystemStatusStrip(state = state, strings = strings, permissionLevel = permissionLevel)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
                verticalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
            ) {
                StatusPill(text = state.selectedStyle?.let(strings::mapStyleTitle) ?: strings.sectionFoundation)
                StatusPill(text = strings.tolerance(state.simplificationToleranceMeters.toInt()))
                state.metrics?.let { StatusPill(text = strings.routeMetricKm(it.totalDistanceKm)) }
            }
            MapRouteFocusCard(state = state, strings = strings)
            MapQuickActionBar {
                if (permissionLevel == LocationPermissionLevel.None) {
                    MapPrimaryAction(label = strings.enableGps, onClick = onRequestLocationPermission)
                } else if (!state.locationTrackingState.isTracking) {
                    MapSecondaryAction(label = strings.startGps, onClick = onStartLocation)
                }
                MapPrimaryAction(
                    strings.routeHere,
                    onCalculateRoute,
                    enabled = state.routingState !is RoutingUiState.Loading,
                )
                MapSecondaryAction(
                    strings.startNavigation,
                    onStartNavigation,
                    enabled = state.routingState !is RoutingUiState.Loading,
                )
                MapSecondaryAction(
                    strings.downloadRoute,
                    onDownloadOfflineRoute,
                    enabled = state.routingState !is RoutingUiState.Loading,
                )
            }
        }
    }
}
