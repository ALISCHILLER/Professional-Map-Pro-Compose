@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.mapStyleTitle
import com.msa.professionalmap.feature.map.presentation.MapUiState
import com.msa.professionalmap.feature.map.presentation.RoutingUiState

@Composable
internal fun MapCommandSectionContent(
    state: MapUiState,
    strings: MapStrings,
    appLanguage: AppLanguage,
    themeMode: AppThemeMode,
    permissionLevel: LocationPermissionLevel,
    activeSection: MapMenuSection,
    actions: MapActionHandlers,
) {
    when (activeSection) {
        MapMenuSection.Settings -> MapAppearanceSettingsCard(
            strings = strings,
            appLanguage = appLanguage,
            themeMode = themeMode,
            onLanguageSelected = actions.onAppLanguageSelected,
            onThemeModeSelected = actions.onThemeModeSelected,
        )
        MapMenuSection.Map -> MapCommands(state, strings, actions)
        MapMenuSection.Location -> LocationCommands(state, strings, permissionLevel, actions)
        MapMenuSection.Routing -> RoutingCommands(state, strings, actions)
        MapMenuSection.Navigation -> NavigationCommands(state, strings, actions)
        MapMenuSection.Guidance -> GuidanceCommands(state, strings, actions)
        MapMenuSection.Offline -> OfflineCommands(state, strings, actions)
    }
}

@Composable
private fun MapCommands(
    state: MapUiState,
    strings: MapStrings,
    actions: MapActionHandlers,
) {
    CommandCard(
        title = strings.mapStyle,
        subtitle = state.selectedStyle?.let(strings::mapStyleTitle) ?: strings.ready,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.styles.forEach { style ->
                FilterChip(
                    selected = style.id == state.selectedStyle?.id,
                    onClick = { actions.onStyleSelected(style) },
                    label = {
                        Text(
                            text = strings.mapStyleTitle(style),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
        CommandActions {
            SecondaryCommand(strings.lessSimplify, actions.onDecreaseSimplification)
            PrimaryCommand(strings.moreSimplify, actions.onIncreaseSimplification)
            if (state.selectedPoint != null) {
                SecondaryCommand(strings.clear, actions.onClearSelection)
            }
        }
    }
}

@Composable
private fun LocationCommands(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    actions: MapActionHandlers,
) {
    CommandCard(
        title = strings.sectionLocation,
        subtitle = strings.locationStatusLabel(state.locationTrackingState.status),
    ) {
        Text(
            text = state.currentLocationText(strings),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CommandActions {
            if (permissionLevel == LocationPermissionLevel.None) {
                PrimaryCommand(strings.enableGps, actions.onRequestLocationPermission)
            } else if (state.locationTrackingState.isTracking) {
                SecondaryCommand(strings.stopGps, actions.onStopLocation)
            } else {
                PrimaryCommand(strings.startGps, actions.onStartLocation)
            }
            SecondaryCommand(
                label = if (state.followUserLocation) strings.following else strings.follow,
                onClick = actions.onToggleFollowUser,
                enabled = state.currentLocation != null,
            )
        }
    }
}

@Composable
private fun RoutingCommands(
    state: MapUiState,
    strings: MapStrings,
    actions: MapActionHandlers,
) {
    CommandCard(
        title = strings.sectionRouting,
        subtitle = strings.routingLabel(state.routingState, state.selectedPoint != null),
    ) {
        Text(
            text = state.routeDestinationText(strings),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.selectedRouteAlternative?.let { route -> RouteSummary(route, strings) }
        if (state.routeAlternatives.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.routeAlternatives.forEach { route ->
                    FilterChip(
                        selected = route.id == state.selectedRouteAlternativeId,
                        onClick = { actions.onSelectRouteAlternative(route.id) },
                        label = {
                            Text(
                                text = strings.routeAlternative(route.title, route.distanceKm),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }
        CommandActions {
            PrimaryCommand(
                label = strings.routeHere,
                onClick = actions.onCalculateRoute,
                enabled = state.routingState !is RoutingUiState.Loading,
            )
            SecondaryCommand(strings.resetRoute, actions.onResetReferenceRoute)
        }
    }
}

@Composable
private fun NavigationCommands(
    state: MapUiState,
    strings: MapStrings,
    actions: MapActionHandlers,
) {
    val progress = state.progressState.progressOrNull()
    CommandCard(
        title = strings.sectionNavigation,
        subtitle = state.progressState.navigationLabel(strings),
    ) {
        Text(
            text = state.navigationHint(strings),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        progress?.let {
            Text(
                text = strings.progressLine(
                    it.remainingDistanceKm,
                    it.remainingMinutes,
                    strings.percentText(it.progressFraction),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        CommandActions {
            if (state.navigationActive) {
                SecondaryCommand(strings.stopNavigation, actions.onStopNavigation)
            } else {
                PrimaryCommand(strings.startNavigation, actions.onStartNavigation)
            }
        }
    }
}

@Composable
private fun GuidanceCommands(
    state: MapUiState,
    strings: MapStrings,
    actions: MapActionHandlers,
) {
    CommandCard(
        title = strings.sectionGuidance,
        subtitle = strings.guidanceStatusText(state.guidanceState),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GuidanceLanguage.entries.forEach { language ->
                FilterChip(
                    selected = language == state.guidanceConfig.language,
                    onClick = { actions.onGuidanceLanguageSelected(language) },
                    label = {
                        Text(
                            strings.guidanceLanguageLabel(language),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
            StatusPill(text = strings.volumeText(state.guidanceConfig.volume))
        }
        CommandActions {
            SecondaryCommand(
                label = if (state.guidanceConfig.muted) strings.voiceOn else strings.muted,
                onClick = actions.onToggleGuidanceMuted,
            )
            SecondaryCommand(strings.volumeDown, actions.onDecreaseGuidanceVolume)
            PrimaryCommand(strings.test, actions.onTestGuidance)
            SecondaryCommand(strings.volumeUp, actions.onIncreaseGuidanceVolume)
        }
    }
}

@Composable
private fun OfflineCommands(
    state: MapUiState,
    strings: MapStrings,
    actions: MapActionHandlers,
) {
    CommandCard(
        title = strings.sectionOffline,
        subtitle = strings.offlineStatusText(state.offlineState),
    ) {
        Text(
            text = strings.localizeNumberText("${state.offlineState.regions.size} ${strings.savedRegions}"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.offlineWorkState.activeJobs.firstOrNull()?.let { job ->
            OfflineJobRow(
                job = job,
                strings = strings,
                onPause = actions.onPauseOffline,
                onResume = actions.onResumeOffline,
                onDelete = actions.onDeleteOffline,
            )
        }
        CommandActions {
            SecondaryCommand(strings.refresh, actions.onRefreshOffline)
            PrimaryCommand(strings.downloadRoute, actions.onDownloadOfflineRoute)
            SecondaryCommand(strings.clearCache, actions.onClearAmbientCache)
            SecondaryCommand(strings.packDb, actions.onPackOfflineDatabase)
        }
    }
}
