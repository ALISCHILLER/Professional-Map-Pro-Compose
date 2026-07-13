package com.msa.professionalmap.feature.map.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.messageText
import com.msa.professionalmap.feature.map.presentation.MapUiState

@Composable
internal fun MapCompactHudOverlay(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    onRequestLocationPermission: () -> Unit,
    onStartLocation: () -> Unit,
    onCalculateRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = state.progressState.progressOrNull()
    val tone = compactTone(state, permissionLevel)
    val targetAccent = when (tone) {
        MapSignalTone.Neutral -> MaterialTheme.colorScheme.primary
        MapSignalTone.Success -> MaterialTheme.colorScheme.secondary
        MapSignalTone.Warning -> MaterialTheme.colorScheme.tertiary
        MapSignalTone.Error -> MaterialTheme.colorScheme.error
    }
    val accent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 220),
        label = "compact-hud-accent",
    )
    val progressFraction by animateFloatAsState(
        targetValue = progress?.progressFraction?.toFloat()?.coerceIn(0f, 1f) ?: 0f,
        animationSpec = tween(durationMillis = 360),
        label = "compact-hud-progress",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 220))
            .semantics { contentDescription = strings.appTitle },
        shape = MapUiTokens.SectionShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        tonalElevation = 4.dp,
        shadowElevation = MapUiTokens.CompactShadow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapGlyphBadge(
                    glyph = if (state.navigationActive) MapGlyph.Navigation else MapGlyph.Map,
                    tint = accent,
                    containerColor = accent.copy(alpha = 0.12f),
                    size = 42.dp,
                    iconSize = 21.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (state.navigationActive) strings.navigationOnRoute else strings.appTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = strings.messageText(state.lastAction) ?: state.compactStatusLine(strings, permissionLevel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusPill(
                    text = state.compactPrimaryStatus(strings, permissionLevel),
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent,
                )
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.14f),
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun MapUiState.compactPrimaryStatus(
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
): String = when {
    permissionLevel == LocationPermissionLevel.None -> strings.noPermission
    !locationTrackingState.isTracking -> strings.idle
    navigationActive -> strings.active
    selectedRouteAlternative != null -> strings.navigationOnRoute
    else -> strings.ready
}

private fun MapUiState.compactStatusLine(
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
): String {
    val route = if (selectedRouteAlternative == null) strings.sectionRouting else strings.navigationOnRoute
    return strings.permissionLabel(permissionLevel) + " · " + route
}

private fun compactTone(
    state: MapUiState,
    permissionLevel: LocationPermissionLevel,
): MapSignalTone = when {
    permissionLevel == LocationPermissionLevel.None -> MapSignalTone.Warning
    state.navigationActive || state.selectedRouteAlternative != null -> MapSignalTone.Success
    state.locationTrackingState.isTracking -> MapSignalTone.Neutral
    else -> MapSignalTone.Neutral
}
