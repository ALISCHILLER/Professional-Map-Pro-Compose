@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.msa.professionalmap.feature.map.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun MapQuickActionBar(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
        verticalArrangement = Arrangement.spacedBy(MapUiTokens.SmallGap),
        content = content,
    )
}

@Composable
internal fun MapPrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        modifier = modifier
            .heightIn(min = MapUiLayout.MinimumTouchTarget)
            .widthIn(min = MapUiLayout.CompactActionMinWidth),
        enabled = enabled,
        onClick = onClick,
        shape = MapUiTokens.ActionShape,
    ) {
        Text(label)
    }
}

@Composable
internal fun MapSecondaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        modifier = modifier
            .heightIn(min = MapUiLayout.MinimumTouchTarget)
            .widthIn(min = MapUiLayout.CompactActionMinWidth),
        enabled = enabled,
        onClick = onClick,
        shape = MapUiTokens.ActionShape,
    ) {
        Text(label)
    }
}
