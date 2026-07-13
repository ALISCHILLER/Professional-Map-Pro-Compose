package com.msa.professionalmap.feature.map.ui

import androidx.compose.ui.unit.dp

/**
 * Shared responsive layout tokens for the map feature.
 *
 * Compact phones must keep the map usable first. Panels therefore stay short and
 * the dense executive dashboard is reserved for expanded layouts.
 */
internal object MapUiLayout {
    val CompactPoiCardBottomPadding = 104.dp
    val ExpandedBreakpoint = 900.dp
    val CompactHorizontalPadding = 10.dp
    val ExpandedHorizontalPadding = 24.dp
    val CompactVerticalPadding = 8.dp
    val ExpandedVerticalPadding = 24.dp
    val CompactHudVerticalPadding = 8.dp
    val ExpandedHudVerticalPadding = 20.dp
    val CompactPanelMaxWidth = 760.dp
    val CompactMenuSheetMaxHeight = 540.dp
    val ExpandedPanelMinWidth = 392.dp
    val ExpandedPanelMaxWidth = 480.dp
    val HudMaxWidth = 584.dp
    val MinimumTouchTarget = 48.dp
    val CompactActionMinWidth = 104.dp
    val SignalBannerMinHeight = 72.dp
    const val CompactPanelHeightFraction = 0.38f
    const val CompactMenuSheetMaxHeightFraction = 0.78f
    const val ExpandedPanelHeightFraction = 0.92f
    const val ExpandedPanelFillFraction = 0.94f
}
