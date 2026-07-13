@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode
import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.presentation.MapUiState

@Composable
internal fun MapCommandMenuSheet(
    state: MapUiState,
    strings: MapStrings,
    appLanguage: AppLanguage,
    themeMode: AppThemeMode,
    permissionLevel: LocationPermissionLevel,
    activeSection: MapMenuSection,
    onSectionSelected: (MapMenuSection) -> Unit,
    onDismiss: () -> Unit,
    actions: MapActionHandlers,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { MapPanelHandle(modifier = Modifier.padding(vertical = 10.dp)) },
        shape = MapUiTokens.PanelShape,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f),
    ) {
        MapTextDirection(strings) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(MapUiLayout.CompactMenuSheetMaxHeightFraction)
                    .heightIn(max = MapUiLayout.CompactMenuSheetMaxHeight)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MapCommandSheetHeader(
                    state = state,
                    strings = strings,
                    permissionLevel = permissionLevel,
                    onDismiss = onDismiss,
                )
                MapCommandTabs(activeSection = activeSection, strings = strings, onSectionSelected = onSectionSelected)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(MapUiTokens.MediumGap),
                ) {
                    MapCommandSectionContent(
                        state = state,
                        strings = strings,
                        appLanguage = appLanguage,
                        themeMode = themeMode,
                        permissionLevel = permissionLevel,
                        activeSection = activeSection,
                        actions = actions,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapCommandSheetHeader(
    state: MapUiState,
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        MapGlyphBadge(
            glyph = MapGlyph.Menu,
            tint = MaterialTheme.colorScheme.primary,
            size = 44.dp,
            iconSize = 22.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = strings.menu, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = state.commandHeaderSubtitle(strings, permissionLevel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(
            modifier = Modifier.widthIn(min = MapUiLayout.MinimumTouchTarget),
            onClick = onDismiss,
        ) {
            Text(text = strings.close, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MapCommandTabs(
    activeSection: MapMenuSection,
    strings: MapStrings,
    onSectionSelected: (MapMenuSection) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = activeSection.ordinal,
        edgePadding = 16.dp,
        divider = {},
    ) {
        MapMenuSection.entries.forEach { section ->
            Tab(
                modifier = Modifier.semantics { contentDescription = section.label(strings) },
                selected = section == activeSection,
                onClick = { onSectionSelected(section) },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MapGlyphIcon(
                            glyph = section.glyph,
                            tint = if (section == activeSection) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            size = 17.dp,
                        )
                        Text(section.shortLabel(strings), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
            )
        }
    }
}

private fun MapUiState.commandHeaderSubtitle(
    strings: MapStrings,
    permissionLevel: LocationPermissionLevel,
): String = strings.permissionLabel(permissionLevel) + " · " +
    if (navigationActive) strings.navigationOnRoute else strings.routingLabel(routingState, selectedPoint != null)
