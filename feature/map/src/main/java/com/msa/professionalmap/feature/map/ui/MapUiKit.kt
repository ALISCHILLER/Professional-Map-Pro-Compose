package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal object MapUiTokens {
    val ScreenPadding = 16.dp
    val PanelPadding = 16.dp
    val SmallGap = 8.dp
    val MediumGap = 14.dp
    val PanelShape = RoundedCornerShape(30.dp)
    val SectionShape = RoundedCornerShape(22.dp)
    val TileShape = RoundedCornerShape(18.dp)
    val ChipShape = RoundedCornerShape(999.dp)
    val ActionShape = RoundedCornerShape(16.dp)
    val DockShape = RoundedCornerShape(28.dp)
    val CompactShadow = 10.dp
    val FloatingShadow = 16.dp
}

@Composable
internal fun MapGlassPanel(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.then(
            if (contentDescription == null) Modifier else Modifier.semantics {
                this.contentDescription = contentDescription
            }
        ),
        shape = MapUiTokens.PanelShape,
        color = colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.98f),
        tonalElevation = 4.dp,
        shadowElevation = MapUiTokens.FloatingShadow,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.40f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.surface.copy(alpha = 0.99f),
                            colorScheme.surfaceContainerLow.copy(alpha = 0.97f),
                        )
                    )
                )
                .padding(MapUiTokens.PanelPadding),
            verticalArrangement = Arrangement.spacedBy(MapUiTokens.MediumGap),
        ) {
            content()
        }
    }
}

@Composable
internal fun MapSectionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MapUiTokens.SectionShape,
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapGlyphBadge(
                    glyph = MapGlyph.Navigation,
                    tint = accentColor,
                    containerColor = accentColor.copy(alpha = 0.12f),
                    size = 40.dp,
                    iconSize = 20.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                trailing()
            }
            SectionAccent(accentColor)
            content()
        }
    }
}

@Composable
internal fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Surface(
        modifier = modifier,
        shape = MapUiTokens.ChipShape,
        color = containerColor.copy(alpha = 0.92f),
        contentColor = contentColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(contentColor.copy(alpha = 0.88f), CircleShape),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SectionAccent(color: Color) {
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(color, color.copy(alpha = 0.30f)),
                )
            ),
    )
}
