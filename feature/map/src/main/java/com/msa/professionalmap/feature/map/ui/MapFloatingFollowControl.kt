package com.msa.professionalmap.feature.map.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Map-native follow control kept outside the bottom dock for one-tap recentering. */
@Composable
internal fun MapFloatingFollowControl(
    following: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (following) colors.primary else colors.surface,
        animationSpec = tween(durationMillis = 220),
        label = "follow-container",
    )
    val content by animateColorAsState(
        targetValue = if (following) colors.onPrimary else colors.primary,
        animationSpec = tween(durationMillis = 220),
        label = "follow-content",
    )

    Surface(
        modifier = modifier
            .size(54.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                selected = following
            },
        onClick = onClick,
        shape = MapUiTokens.ActionShape,
        color = container.copy(alpha = 0.98f),
        contentColor = content,
        shadowElevation = MapUiTokens.CompactShadow,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, content.copy(alpha = 0.20f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            MapGlyphIcon(
                glyph = MapGlyph.Target,
                tint = content,
                size = 24.dp,
            )
        }
    }
}
