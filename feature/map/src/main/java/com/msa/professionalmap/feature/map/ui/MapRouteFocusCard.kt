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
import com.msa.professionalmap.core.model.RouteMetrics
import com.msa.professionalmap.core.progress.domain.RouteProgress
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState
import java.util.Locale

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

    MapSectionCard(
        modifier = modifier,
        title = selectedRoute?.title ?: strings.sectionNavigation,
        subtitle = progress?.let { strings.progressLine(it.remainingDistanceKm, it.remainingMinutes, it.progressPercentText()) }
            ?: selectedRoute?.let { route ->
                strings.selectedRouteSummary(
                    title = route.title,
                    distanceKm = route.distanceKm,
                    durationMinutes = route.durationMinutes,
                    stepCount = route.legs.sumOf { it.steps.size },
                )
            }
            ?: metrics?.routeSummary(strings)
            ?: strings.navigationStartHint,
        accentColor = if (state.navigationActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary
        },
        trailing = {
            StatusPill(text = if (state.navigationActive) strings.active else strings.idle)
        },
    ) {
        progress?.let { value ->
            LinearProgressIndicator(
                progress = { value.progressFraction.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            value.nextInstruction?.let { instruction ->
                Text(
                    text = strings.nextInstruction(instruction.instruction, strings.distanceText(instruction.distanceMeters)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
            verticalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
        ) {
            metrics?.let { value ->
                StatusPill(text = strings.routeMetricKm(value.totalDistanceKm))
                StatusPill(text = strings.metricPoints + " " + strings.localizeNumberText("${value.simplifiedPointCount}/${value.originalPointCount}"))
            }
            StatusPill(text = strings.tolerance(state.simplificationToleranceMeters.toInt()))
        }
    }
}

private fun RouteMetrics.routeSummary(strings: MapStrings): String =
    "${strings.metricRoute}: ${strings.routeMetricKm(totalDistanceKm)} · ${strings.metricDirect}: ${strings.routeMetricKm(directDistanceKm)}"

private fun RouteProgress.progressPercentText(): String = String.format(Locale.US, "%.0f", progressFraction * 100.0)
