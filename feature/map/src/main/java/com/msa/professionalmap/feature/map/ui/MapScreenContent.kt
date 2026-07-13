package com.msa.professionalmap.feature.map.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState

@Composable
internal fun ReadyMapContent(
    state: MapUiState,
    strings: MapStrings,
    appLanguage: AppLanguage,
    themeMode: AppThemeMode,
    permissionLevel: LocationPermissionLevel,
    actions: MapActionHandlers,
) {
    if (state.selectedStyle == null) return
    val scene = rememberMapScene(state = state, strings = strings)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expandedLayout = maxWidth >= MapUiLayout.ExpandedBreakpoint
        val controlPanelMaxHeight = maxHeight * MapUiLayout.ExpandedPanelHeightFraction
        var menuOpen by rememberSaveable { mutableStateOf(false) }
        var activeMenuName by rememberSaveable { mutableStateOf(MapMenuSection.Settings.name) }
        val activeMenuSection = remember(activeMenuName) {
            runCatching { MapMenuSection.valueOf(activeMenuName) }
                .getOrElse { MapMenuSection.Routing }
        }
        val openMenu = remember {
            { section: MapMenuSection ->
                activeMenuName = section.name
                menuOpen = true
            }
        }
        val onMapInteraction = remember(actions) {
            { interaction: MapSceneInteraction ->
                when (interaction) {
                    is MapSceneInteraction.Poi -> actions.onPoiSelected(interaction.id)
                    is MapSceneInteraction.Route -> actions.onSelectRouteAlternative(interaction.id)
                    is MapSceneInteraction.MapPoint -> actions.onMapClick(interaction.point)
                }
            }
        }

        MapLibreView(
            scene = scene,
            onInteraction = onMapInteraction,
            onMapLoadFailed = actions.onMapLoadFailed,
            modifier = Modifier.fillMaxSize(),
        )

        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f),
                                Color.Transparent,
                            )
                        )
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f),
                            )
                        )
                    ),
            )
        }

        MapHudOverlay(
            state = state,
            strings = strings,
            permissionLevel = permissionLevel,
            onRequestLocationPermission = actions.onRequestLocationPermission,
            onStartLocation = actions.onStartLocation,
            onCalculateRoute = actions.onCalculateRoute,
            onStartNavigation = actions.onStartNavigation,
            onDownloadOfflineRoute = actions.onDownloadOfflineRoute,
            compact = !expandedLayout,
            modifier = if (expandedLayout) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        horizontal = MapUiLayout.ExpandedHorizontalPadding,
                        vertical = MapUiLayout.ExpandedHudVerticalPadding,
                    )
            } else {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        horizontal = MapUiLayout.CompactHorizontalPadding,
                        vertical = MapUiLayout.CompactHudVerticalPadding,
                    )
            },
        )

        AnimatedContent(
            targetState = state.selectedPoi,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 5 }) togetherWith
                    (fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 6 })
            },
            contentKey = { poi -> poi?.id ?: "no-selected-poi" },
            label = "selected-poi-card",
            modifier = if (expandedLayout) {
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = MapUiLayout.ExpandedHorizontalPadding,
                        bottom = MapUiLayout.ExpandedVerticalPadding,
                    )
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = MapUiLayout.CompactHorizontalPadding,
                        end = MapUiLayout.CompactHorizontalPadding,
                        bottom = MapUiLayout.CompactPoiCardBottomPadding,
                    )
            },
        ) { poi ->
            if (poi != null) {
                MapPoiSelectionCard(
                    poi = poi,
                    strings = strings,
                    onRoute = actions.onCalculateRoute,
                    onDismiss = actions.onClearSelection,
                )
            }
        }

        if (!expandedLayout && state.currentLocation != null) {
            MapFloatingFollowControl(
                following = state.followUserLocation,
                contentDescription = if (state.followUserLocation) strings.following else strings.follow,
                onClick = actions.onToggleFollowUser,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            )
        }

        if (expandedLayout) {
            MapControlPanel(
                state = state,
                strings = strings,
                appLanguage = appLanguage,
                themeMode = themeMode,
                permissionLevel = permissionLevel,
                maxPanelHeight = controlPanelMaxHeight,
                actions = actions,
                compact = false,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(MapUiLayout.ExpandedPanelFillFraction)
                    .widthIn(
                        min = MapUiLayout.ExpandedPanelMinWidth,
                        max = MapUiLayout.ExpandedPanelMaxWidth,
                    )
                    .padding(
                        horizontal = MapUiLayout.ExpandedHorizontalPadding,
                        vertical = MapUiLayout.ExpandedVerticalPadding,
                    ),
            )
        } else {
            MapBottomCommandBar(
                strings = strings,
                locationTracking = state.locationTrackingState.isTracking,
                hasSelectedRoute = state.selectedRouteAlternativeId != null,
                navigationActive = state.navigationActive,
                onOpenMenu = openMenu,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .widthIn(max = MapUiLayout.CompactPanelMaxWidth)
                    .navigationBarsPadding()
                    .padding(
                        horizontal = MapUiLayout.CompactHorizontalPadding,
                        vertical = MapUiLayout.CompactVerticalPadding,
                    ),
            )
        }

        if (!expandedLayout && menuOpen) {
            MapCommandMenuSheet(
                state = state,
                strings = strings,
                appLanguage = appLanguage,
                themeMode = themeMode,
                permissionLevel = permissionLevel,
                activeSection = activeMenuSection,
                onSectionSelected = { activeMenuName = it.name },
                onDismiss = { menuOpen = false },
                actions = actions,
            )
        }
    }
}
