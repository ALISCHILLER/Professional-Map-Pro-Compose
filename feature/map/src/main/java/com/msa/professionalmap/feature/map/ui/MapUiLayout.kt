package com.msa.professionalmap.feature.map.ui

import androidx.compose.ui.unit.dp

/**
 * Shared responsive layout tokens for the map feature.
 *
 * Keeping breakpoints and shell dimensions centralized prevents the map screen,
 * HUD, and control panel from drifting apart as the UI evolves.
 */
internal object MapUiLayout {
    val ExpandedBreakpoint = 900.dp
    val CompactHorizontalPadding = 16.dp
    val ExpandedHorizontalPadding = 24.dp
    val CompactVerticalPadding = 14.dp
    val ExpandedVerticalPadding = 24.dp
    val CompactHudVerticalPadding = 12.dp
    val ExpandedHudVerticalPadding = 20.dp
    val CompactPanelMaxWidth = 760.dp
    val ExpandedPanelMinWidth = 392.dp
    val ExpandedPanelMaxWidth = 480.dp
    val HudMaxWidth = 584.dp
    val MinimumTouchTarget = 48.dp
    val CompactActionMinWidth = 116.dp
    val SignalBannerMinHeight = 72.dp
    const val CompactPanelHeightFraction = 0.58f
    const val ExpandedPanelHeightFraction = 0.92f
    const val ExpandedPanelFillFraction = 0.94f
}
