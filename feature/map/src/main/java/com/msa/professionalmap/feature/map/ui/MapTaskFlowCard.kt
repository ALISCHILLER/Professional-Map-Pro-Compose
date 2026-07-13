@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.messageText
import com.msa.professionalmap.feature.map.presentation.MapUiMessage
import com.msa.professionalmap.feature.map.presentation.MapUiState
import com.msa.professionalmap.feature.map.presentation.RoutingUiState

@Composable
internal fun MapTaskFlowCard(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    MapSectionCard(
        modifier = modifier,
        title = strings.nextAction,
        subtitle = state.nextActionText(strings, permissionLevel),
        accentColor = MaterialTheme.colorScheme.primary,
        trailing = { StatusPill(text = state.workflowProgressText(strings)) },
    ) {
        if (compact) {
            Text(
                text = state.compactWorkflowText(strings, permissionLevel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
                verticalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
            ) {
                WorkflowPill(text = strings.routingTapDestination, done = state.selectedPoint != null)
                WorkflowPill(text = strings.routeHere, done = state.selectedRouteAlternative != null)
                WorkflowPill(text = strings.startGps, done = state.currentLocation != null)
                WorkflowPill(text = strings.startNavigation, done = state.navigationActive)
                WorkflowPill(text = strings.downloadRoute, done = state.offlineState.regions.isNotEmpty())
            }
        }
    }
}

@Composable
private fun WorkflowPill(text: String, done: Boolean) {
    StatusPill(
        text = text,
        containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (done) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun MapUiState.workflowProgressText(strings: MapStrings): String {
    val completed = listOf(
        selectedPoint != null,
        selectedRouteAlternative != null,
        currentLocation != null,
        navigationActive,
        offlineState.regions.isNotEmpty(),
    ).count { it }
    return strings.localizeNumberText("$completed/5 ${strings.completed}")
}

private fun MapUiState.compactWorkflowText(
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
): String = strings.localizeNumberText("${workflowProgressText(strings)} · ${nextActionText(strings, permissionLevel)}")

private fun MapUiState.nextActionText(
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
): String = when {
    permissionLevel == LocationPermissionLevel.None -> strings.messageText(MapUiMessage.LocationPermissionRequired) ?: strings.enableGps
    selectedPoint == null -> strings.routingTapDestination
    routingState is RoutingUiState.Loading -> strings.routingLoading
    selectedRouteAlternative == null -> strings.messageText(MapUiMessage.SelectDestinationFirst) ?: strings.routeHere
    currentLocation == null -> strings.messageText(MapUiMessage.StartGpsBeforeNavigation) ?: strings.startGps
    !navigationActive -> strings.navigationStartHint
    else -> strings.navigationOnRoute
}
