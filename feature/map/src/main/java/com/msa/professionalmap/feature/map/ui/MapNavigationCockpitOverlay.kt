package com.msa.professionalmap.feature.map.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState

/** Compact driving cockpit that keeps the next maneuver and remaining trip data glanceable. */
@Composable
internal fun MapNavigationCockpitOverlay(
    state: MapUiState,
    strings: MapStrings,
    modifier: Modifier = Modifier,
) {
    val progress = state.progressState.progressOrNull() ?: return
    val offRoute = state.progressState is ProgressState.OffRoute || state.progressState is ProgressState.Rerouting
    val targetAccent = if (offRoute) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val accent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 240),
        label = "navigation-accent",
    )
    val progressFraction by animateFloatAsState(
        targetValue = progress.progressFraction.toFloat().coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "navigation-progress",
    )
    val instruction = progress.nextInstruction
    val title = when (state.progressState) {
        is ProgressState.OffRoute -> strings.offRouteLabel(state.progressState.distanceFromRouteMeters)
        is ProgressState.Rerouting -> strings.navigationRerouting
        is ProgressState.Arrived -> strings.navigationArrived
        else -> instruction?.instruction ?: strings.navigationOnRoute
    }
    val distance = instruction?.let { strings.distanceText(it.distanceMeters) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 240))
            .semantics {
                contentDescription = title
                liveRegion = LiveRegionMode.Polite
            },
        shape = MapUiTokens.SectionShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.985f),
        tonalElevation = 6.dp,
        shadowElevation = MapUiTokens.FloatingShadow,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.34f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapGlyphBadge(
                    glyph = if (offRoute) MapGlyph.Route else MapGlyph.Navigation,
                    tint = accent,
                    containerColor = accent.copy(alpha = 0.14f),
                    size = 48.dp,
                    iconSize = 24.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    instruction?.roadName?.takeIf { it.isNotBlank() }?.let { roadName ->
                        Text(
                            text = roadName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                distance?.let {
                    StatusPill(
                        text = it,
                        containerColor = accent.copy(alpha = 0.14f),
                        contentColor = accent,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.14f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CockpitMetric(
                    value = strings.distanceText(progress.remainingDistanceMeters),
                    color = accent,
                )
                CockpitMetric(
                    value = strings.localizeNumberText(progress.remainingMinutes.toInt().toString()) +
                        " " + strings.minutesSuffix,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusPill(
                    text = strings.percentText(progress.progressFraction),
                    containerColor = accent.copy(alpha = 0.12f),
                    contentColor = accent,
                )
            }
        }
    }
}

@Composable
private fun CockpitMetric(value: String, color: Color) {
    Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
