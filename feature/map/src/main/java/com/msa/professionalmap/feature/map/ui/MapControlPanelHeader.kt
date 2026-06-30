package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.messageText
import com.msa.professionalmap.feature.map.presentation.MapUiState
import com.msa.professionalmap.feature.map.presentation.RoutingUiState

@Composable
internal fun MapControlPanelHeader(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = MapUiLayout.SignalBannerMinHeight),
        verticalArrangement = Arrangement.spacedBy(MapUiTokens.MediumGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MapUiTokens.MediumGap),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = strings.sectionFoundation,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.appSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(text = if (state.navigationActive) strings.active else strings.idle)
        }
        MapSignalBanner(
            title = panelStatusTitle(state = state, strings = strings, permissionLevel = permissionLevel),
            message = strings.messageText(state.lastAction) ?: strings.ready,
            tone = panelStatusTone(state = state, permissionLevel = permissionLevel),
        )
        MapSystemStatusStrip(
            state = state,
            strings = strings,
            permissionLevel = permissionLevel,
        )
    }
}

@Composable
internal fun MapSignalBanner(
    title: String,
    message: String,
    tone: MapSignalTone,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = when (tone) {
        MapSignalTone.Neutral -> colorScheme.primary
        MapSignalTone.Success -> colorScheme.secondary
        MapSignalTone.Warning -> colorScheme.tertiary
        MapSignalTone.Error -> colorScheme.error
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MapUiTokens.TileShape,
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal enum class MapSignalTone { Neutral, Success, Warning, Error }

private fun panelStatusTitle(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
): String = when {
    permissionLevel == LocationPermissionLevel.None -> strings.locationPermissionRequired
    state.routingState is RoutingUiState.Loading -> strings.routingLoading
    state.routingState is RoutingUiState.Error -> strings.mapFailed
    state.navigationActive -> strings.navigationOnRoute
    state.selectedPoint != null -> strings.routingReady
    else -> strings.ready
}

private fun panelStatusTone(
    state: MapUiState,
    permissionLevel: LocationPermissionLevel,
): MapSignalTone = when {
    permissionLevel == LocationPermissionLevel.None -> MapSignalTone.Warning
    state.routingState is RoutingUiState.Error -> MapSignalTone.Error
    state.routingState is RoutingUiState.Loading -> MapSignalTone.Warning
    state.navigationActive || state.routePoints.size >= 2 -> MapSignalTone.Success
    else -> MapSignalTone.Neutral
}
