@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.msa.professionalmap.core.model.RouteMetrics
import com.msa.professionalmap.core.progress.domain.RouteProgress
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.formatZero
import com.msa.professionalmap.feature.map.presentation.MapUiState

@Composable
internal fun MapRouteFocusCard(
    state: MapUiState,
    strings: MapStrings,
    modifier: Modifier = Modifier,
) {
    val progress = state.progressState.progressOrNull()
    val selectedRoute = state.selectedRouteAlternative
    val metrics = state.metrics
    if (progress == null && selectedRoute == null && metrics == null) return

    val accent = if (state.navigationActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    MapSectionCard(
        modifier = modifier,
        title = selectedRoute?.title ?: strings.sectionNavigation,
        subtitle = progress?.let {
            strings.progressLine(it.remainingDistanceKm, it.remainingMinutes, it.progressPercentText())
        } ?: selectedRoute?.let { route ->
            strings.selectedRouteSummary(
                title = route.title,
                distanceKm = route.distanceKm,
                durationMinutes = route.durationMinutes,
                stepCount = route.legs.sumOf { it.steps.size },
            )
        } ?: metrics?.routeSummary(strings) ?: strings.navigationStartHint,
        accentColor = accent,
        trailing = {
            StatusPill(
                text = if (state.navigationActive) strings.active else strings.idle,
                containerColor = accent.copy(alpha = 0.13f),
                contentColor = accent,
            )
        },
    ) {
        progress?.let { value ->
            LinearProgressIndicator(
                progress = { value.progressFraction.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = accent.copy(alpha = 0.14f),
            )
            value.nextInstruction?.let { instruction ->
                Text(
                    text = strings.nextInstruction(
                        instruction.instruction,
                        strings.distanceText(instruction.distanceMeters),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
            verticalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
        ) {
            metrics?.let { value ->
                StatusPill(
                    text = strings.routeMetricKm(value.totalDistanceKm),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                StatusPill(
                    text = strings.metricPoints + " " +
                        strings.localizeNumberText("${value.simplifiedPointCount}/${value.originalPointCount}"),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            StatusPill(
                text = strings.tolerance(state.simplificationToleranceMeters.toInt()),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

private fun RouteMetrics.routeSummary(strings: MapStrings): String =
    "${strings.metricRoute}: ${strings.routeMetricKm(totalDistanceKm)} · " +
        "${strings.metricDirect}: ${strings.routeMetricKm(directDistanceKm)}"

private fun RouteProgress.progressPercentText(): String = (progressFraction * 100.0).formatZero()
