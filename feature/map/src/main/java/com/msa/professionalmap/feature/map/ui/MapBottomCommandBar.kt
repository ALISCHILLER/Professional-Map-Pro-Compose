package com.msa.professionalmap.feature.map.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.feature.map.i18n.MapStrings

@Composable
internal fun MapBottomCommandBar(
    strings: MapStrings,
    locationTracking: Boolean,
    hasSelectedRoute: Boolean,
    navigationActive: Boolean,
    onOpenMenu: (MapMenuSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = MapUiLayout.CompactPanelMaxWidth)
            .semantics { contentDescription = strings.menu },
        shape = MapUiTokens.DockShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.985f),
        tonalElevation = 6.dp,
        shadowElevation = MapUiTokens.FloatingShadow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MapDockItem(
                label = strings.menu,
                glyph = MapGlyph.Menu,
                selected = false,
                onClick = { onOpenMenu(MapMenuSection.Settings) },
                modifier = Modifier.weight(1.08f),
            )
            MapDockItem(
                label = MapMenuSection.Location.shortLabel(strings),
                glyph = MapGlyph.Location,
                selected = locationTracking,
                onClick = { onOpenMenu(MapMenuSection.Location) },
                modifier = Modifier.weight(1f),
            )
            MapDockItem(
                label = MapMenuSection.Routing.shortLabel(strings),
                glyph = MapGlyph.Route,
                selected = hasSelectedRoute,
                onClick = { onOpenMenu(MapMenuSection.Routing) },
                modifier = Modifier.weight(1f),
            )
            MapDockItem(
                label = MapMenuSection.Navigation.shortLabel(strings),
                glyph = MapGlyph.Navigation,
                selected = navigationActive,
                onClick = { onOpenMenu(MapMenuSection.Navigation) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MapDockItem(
    label: String,
    glyph: MapGlyph,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent by animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 220),
        label = "dock-accent",
    )
    val container by animateColorAsState(
        targetValue = if (selected) colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "dock-container",
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 23.dp else 20.dp,
        animationSpec = tween(durationMillis = 220),
        label = "dock-icon-size",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = tween(durationMillis = 220),
        label = "dock-scale",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 26.dp else 10.dp,
        animationSpec = tween(durationMillis = 220),
        label = "dock-indicator-width",
    )

    Surface(
        modifier = modifier
            .heightIn(min = 60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics {
                contentDescription = label
                role = Role.Tab
                this.selected = selected
            },
        onClick = onClick,
        shape = MapUiTokens.ActionShape,
        color = container,
        contentColor = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .width(indicatorWidth)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) accent else Color.Transparent),
            )
            MapGlyphIcon(glyph = glyph, tint = accent, size = iconSize)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
