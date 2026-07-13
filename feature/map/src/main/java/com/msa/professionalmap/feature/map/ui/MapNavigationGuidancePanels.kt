@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.GuidanceState
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.formatZero
import com.msa.professionalmap.feature.map.i18n.messageText
import com.msa.professionalmap.feature.map.presentation.MapUiMessage

@Composable
internal fun NavigationPanel(
    strings: MapStrings,
    progressState: ProgressState,
    navigationActive: Boolean,
    navigationPaused: Boolean,
    canStartNavigation: Boolean,
    hasNavigableRoute: Boolean,
    hasCurrentLocation: Boolean,
    onStartNavigation: () -> Unit,
    onPauseNavigation: () -> Unit,
    onResumeNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
) {
    val progress = progressState.progressOrNull()
    MapSectionCard(
        title = strings.sectionNavigation,
        subtitle = progressState.navigationLabel(strings),
        accentColor = MaterialTheme.colorScheme.primary,
        trailing = {
            StatusPill(
                text = if (navigationActive) strings.active else strings.idle,
                containerColor = if (navigationActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (navigationActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
    ) {
        Text(
            text = navigationActionHint(
                strings = strings,
                progressState = progressState,
                navigationActive = navigationActive,
                canStartNavigation = canStartNavigation,
                hasNavigableRoute = hasNavigableRoute,
                hasCurrentLocation = hasCurrentLocation,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        progress?.let { value ->
            LinearProgressIndicator(
                progress = { value.progressFraction.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = strings.progressLine(value.remainingDistanceKm, value.remainingMinutes, value.progressPercent()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            value.nextInstruction?.let { instruction ->
                Text(
                    text = strings.nextInstruction(instruction.instruction, strings.distanceText(instruction.distanceMeters)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        MapQuickActionBar {
            if (navigationActive) {
                if (navigationPaused) {
                    MapPrimaryAction(label = strings.resume, onClick = onResumeNavigation)
                } else {
                    MapSecondaryAction(label = strings.pause, onClick = onPauseNavigation)
                }
                MapSecondaryAction(label = strings.stopNavigation, onClick = onStopNavigation)
            } else {
                MapPrimaryAction(
                    label = strings.startNavigation,
                    enabled = true,
                    onClick = onStartNavigation,
                )
            }
        }
    }
}

@Composable
internal fun GuidancePanel(
    strings: MapStrings,
    guidanceState: GuidanceState,
    selectedLanguage: GuidanceLanguage,
    volume: Float,
    muted: Boolean,
    onToggleMuted: () -> Unit,
    onLanguageSelected: (GuidanceLanguage) -> Unit,
    onIncreaseVolume: () -> Unit,
    onDecreaseVolume: () -> Unit,
    onTestGuidance: () -> Unit,
) {
    MapSectionCard(
        title = strings.sectionGuidance,
        subtitle = strings.guidanceStatusText(guidanceState),
        accentColor = MaterialTheme.colorScheme.tertiary,
        trailing = {
            StatusPill(
                text = if (muted) strings.muted else strings.voiceOn,
                containerColor = if (muted) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
                contentColor = if (muted) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
            )
        },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GuidanceLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    label = { Text(strings.guidanceLanguageLabel(language)) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = strings.volumeText(volume),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MapSecondaryAction(label = strings.volumeDown, onClick = onDecreaseVolume)
            MapSecondaryAction(label = strings.volumeUp, onClick = onIncreaseVolume)
            MapPrimaryAction(label = strings.test, onClick = onTestGuidance)
        }
    }
}

private fun navigationActionHint(
    strings: MapStrings,
    progressState: ProgressState,
    navigationActive: Boolean,
    canStartNavigation: Boolean,
    hasNavigableRoute: Boolean,
    hasCurrentLocation: Boolean,
): String = when {
    navigationActive -> progressState.navigationLabel(strings)
    canStartNavigation -> strings.navigationStartHint
    !hasNavigableRoute -> strings.messageText(MapUiMessage.RouteBeforeNavigation) ?: strings.routeHere
    !hasCurrentLocation -> strings.messageText(MapUiMessage.StartGpsBeforeNavigation) ?: strings.startGps
    else -> strings.navigationStartHint
}

internal fun ProgressState.navigationLabel(strings: MapStrings): String = when (this) {
    ProgressState.Idle -> strings.navigationStartHint
    is ProgressState.Navigating -> strings.navigationOnRoute
    is ProgressState.OffRoute -> strings.offRouteLabel(distanceFromRouteMeters)
    is ProgressState.Rerouting -> strings.navigationRerouting
    is ProgressState.Arrived -> strings.navigationArrived
}

internal fun ProgressState.progressOrNull(): RouteProgress? = when (this) {
    is ProgressState.Navigating -> progress
    is ProgressState.OffRoute -> progress
    is ProgressState.Rerouting -> lastKnownProgress
    is ProgressState.Arrived -> progress
    ProgressState.Idle -> null
}

private fun RouteProgress.progressPercent(): String = (progressFraction * 100.0).formatZero()
