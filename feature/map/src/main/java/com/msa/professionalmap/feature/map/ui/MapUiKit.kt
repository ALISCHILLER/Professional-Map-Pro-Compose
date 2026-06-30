package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Text
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
    val PanelShape = RoundedCornerShape(32.dp)
    val SectionShape = RoundedCornerShape(24.dp)
    val TileShape = RoundedCornerShape(20.dp)
    val ChipShape = RoundedCornerShape(999.dp)
    val ActionShape = RoundedCornerShape(18.dp)
}

@Composable
internal fun MapGlassPanel(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.then(
            if (contentDescription == null) {
                Modifier
            } else {
                Modifier.semantics {
                    this.contentDescription = contentDescription
                }
            }
        ),
        shape = MapUiTokens.PanelShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 18.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        ),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
                        ),
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MapUiTokens.SectionShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionAccent(accentColor)
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trailing()
            }
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
        color = containerColor.copy(alpha = 0.88f),
        contentColor = contentColor,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SectionAccent(color: Color) {
    Box(
        modifier = Modifier
            .width(22.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(color),
    )
}
