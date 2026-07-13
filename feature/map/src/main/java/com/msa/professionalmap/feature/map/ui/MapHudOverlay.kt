package com.msa.professionalmap.feature.map.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState

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
    compact: Boolean = false,
) {
    if (compact) {
        if (state.navigationActive && state.progressState.progressOrNull() != null) {
            MapNavigationCockpitOverlay(
                state = state,
                strings = strings,
                modifier = modifier,
            )
        } else {
            MapCompactHudOverlay(
                state = state,
                strings = strings,
                permissionLevel = permissionLevel,
                onRequestLocationPermission = onRequestLocationPermission,
                onStartLocation = onStartLocation,
                onCalculateRoute = onCalculateRoute,
                onStartNavigation = onStartNavigation,
                modifier = modifier,
            )
        }
    } else {
        MapExpandedHudOverlay(
            state = state,
            strings = strings,
            permissionLevel = permissionLevel,
            onRequestLocationPermission = onRequestLocationPermission,
            onStartLocation = onStartLocation,
            onCalculateRoute = onCalculateRoute,
            onStartNavigation = onStartNavigation,
            onDownloadOfflineRoute = onDownloadOfflineRoute,
            modifier = modifier,
        )
    }
}
