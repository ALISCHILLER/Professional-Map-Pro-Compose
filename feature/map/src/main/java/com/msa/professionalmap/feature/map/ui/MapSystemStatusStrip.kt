@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState

@Composable
internal fun MapSystemStatusStrip(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
    ) {
        MapStatusTile(
            label = strings.sectionLocation,
            value = strings.permissionLabel(permissionLevel),
            accentColor = when (permissionLevel) {
                LocationPermissionLevel.None -> MaterialTheme.colorScheme.error
                LocationPermissionLevel.Approximate -> MaterialTheme.colorScheme.tertiary
                LocationPermissionLevel.Precise -> MaterialTheme.colorScheme.primary
            },
        )
        MapStatusTile(
            label = strings.sectionRouting,
            value = strings.routingLabel(state.routingState, state.selectedPoint != null),
            accentColor = MaterialTheme.colorScheme.secondary,
        )
        MapStatusTile(
            label = strings.sectionGuidance,
            value = if (state.guidanceState.isSpeakable) strings.voiceOn else strings.muted,
            accentColor = MaterialTheme.colorScheme.primary,
        )
        MapStatusTile(
            label = strings.sectionOffline,
            value = offlineSummary(state, strings),
            accentColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

private fun offlineSummary(state: MapUiState, strings: MapStrings): String = when {
    state.offlineWorkState.activeJobs.isNotEmpty() -> strings.worker
    state.offlineState.regions.isNotEmpty() -> strings.localizeNumberText("${state.offlineState.regions.size} ${strings.savedRegions}")
    else -> strings.offlineDefaultMessage
}
