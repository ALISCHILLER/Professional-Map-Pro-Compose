@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.feature.map.i18n.MapStrings
import java.util.Locale

@Composable
internal fun MapPoiSelectionCard(
    poi: MapPoi,
    strings: MapStrings,
    onRoute: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localized = poi.localizedForMap(strings)
    val accent = poiAccentColor(poi.category)
    Surface(
        modifier = modifier
            .widthIn(max = 440.dp)
            .animateContentSize(animationSpec = tween(durationMillis = 220)),
        shape = MapUiTokens.PanelShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 5.dp,
        shadowElevation = MapUiTokens.FloatingShadow,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapGlyphBadge(
                    glyph = MapGlyph.Pin,
                    tint = accent,
                    containerColor = accent.copy(alpha = 0.13f),
                    size = 48.dp,
                    iconSize = 24.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = localized.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = localized.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusPill(
                    text = poi.category.localizedLabel(strings),
                    containerColor = accent.copy(alpha = 0.13f),
                    contentColor = accent,
                )
            }
            SectionAccent(accent)

            Surface(
                shape = MapUiTokens.ActionShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MapGlyphIcon(MapGlyph.Target, accent, size = 18.dp)
                    Text(
                        text = "${strings.markerCoordinateLabel()}: ${coordinateText(poi, strings)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            MapQuickActionBar {
                MapSecondaryAction(label = strings.close, onClick = onDismiss)
                MapPrimaryAction(label = strings.routeHere, onClick = onRoute)
            }
        }
    }
}

private fun coordinateText(poi: MapPoi, strings: MapStrings): String {
    val raw = String.format(Locale.US, "%.5f, %.5f", poi.position.latitude, poi.position.longitude)
    return strings.localizeNumberText(raw)
}
