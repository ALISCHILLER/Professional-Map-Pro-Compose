@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.location.LocationTrackingState
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.RoutingUiState
import java.util.Locale

@Composable
internal fun LocationPanel(
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    trackingState: LocationTrackingState,
    currentLocation: DeviceLocation?,
    followUserLocation: Boolean,
    onRequestLocationPermission: () -> Unit,
    onStartLocation: () -> Unit,
    onStopLocation: () -> Unit,
    onToggleFollowUser: () -> Unit,
) {
    MapSectionCard(
        title = strings.sectionLocation,
        subtitle = strings.locationStatusLabel(trackingState.status),
        accentColor = MaterialTheme.colorScheme.primary,
        trailing = {
            StatusPill(
                text = strings.permissionLabel(permissionLevel),
                containerColor = when (permissionLevel) {
                    LocationPermissionLevel.None -> MaterialTheme.colorScheme.errorContainer
                    LocationPermissionLevel.Approximate -> MaterialTheme.colorScheme.tertiaryContainer
                    LocationPermissionLevel.Precise -> MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = when (permissionLevel) {
                    LocationPermissionLevel.None -> MaterialTheme.colorScheme.onErrorContainer
                    LocationPermissionLevel.Approximate -> MaterialTheme.colorScheme.onTertiaryContainer
                    LocationPermissionLevel.Precise -> MaterialTheme.colorScheme.onPrimaryContainer
                },
            )
        },
    ) {
        currentLocation?.let { location ->
            Text(
                text = "${location.position.latitude.formatCoord()}, ${location.position.longitude.formatCoord()}" +
                    location.accuracyMeters?.let { " · ±${it.toInt()}m" }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } ?: Text(
            text = strings.currentLocationFallback,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        MapQuickActionBar {
            if (permissionLevel == LocationPermissionLevel.None) {
                MapPrimaryAction(label = strings.enableGps, onClick = onRequestLocationPermission)
            } else {
                if (trackingState.isTracking) {
                    MapSecondaryAction(label = strings.stopGps, onClick = onStopLocation)
                } else {
                    MapPrimaryAction(label = strings.startGps, onClick = onStartLocation)
                }
                MapSecondaryAction(
                    label = if (followUserLocation) strings.following else strings.follow,
                    enabled = currentLocation != null,
                    onClick = onToggleFollowUser,
                )
            }
        }
    }
}

@Composable
internal fun RoutingPanel(
    strings: MapStrings,
    selectedPoint: GeoPoint?,
    routingState: RoutingUiState,
    alternatives: List<RouteAlternative>,
    selectedAlternativeId: String?,
    selectedAlternative: RouteAlternative?,
    onCalculateRoute: () -> Unit,
    onSelectRouteAlternative: (String) -> Unit,
    onResetReferenceRoute: () -> Unit,
) {
    MapSectionCard(
        title = strings.sectionRouting,
        subtitle = strings.routingLabel(routingState, selectedPoint != null),
        accentColor = MaterialTheme.colorScheme.secondary,
        trailing = {
            if (routingState is RoutingUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.width(24.dp), strokeWidth = 2.dp)
            } else {
                StatusPill(text = strings.localizeNumberText(alternatives.size.toString()) + " " + strings.steps)
            }
        },
    ) {
        selectedAlternative?.let { route ->
            Text(
                text = strings.selectedRouteSummary(route.title, route.distanceKm, route.durationMinutes, route.legs.sumOf { it.steps.size }),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (alternatives.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                alternatives.forEach { route ->
                    FilterChip(
                        selected = route.id == selectedAlternativeId,
                        onClick = { onSelectRouteAlternative(route.id) },
                        label = { Text(strings.routeAlternative(route.title, route.distanceKm)) },
                    )
                }
            }
        }

        MapQuickActionBar {
            MapPrimaryAction(
                label = strings.routeHere,
                modifier = Modifier.weight(1f),
                enabled = selectedPoint != null && routingState !is RoutingUiState.Loading,
                onClick = onCalculateRoute,
            )
            MapSecondaryAction(label = strings.resetRoute, onClick = onResetReferenceRoute)
        }
    }
}

private fun Double.formatCoord(): String = String.format(Locale.US, "%.5f", this)
