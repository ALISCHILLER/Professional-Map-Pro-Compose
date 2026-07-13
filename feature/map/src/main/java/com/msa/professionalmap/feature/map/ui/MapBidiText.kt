package com.msa.professionalmap.feature.map.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.msa.professionalmap.feature.map.i18n.MapStrings

@Composable
internal fun MapTextDirection(
    strings: MapStrings,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}

internal fun MapStrings.stableTechnicalText(value: String): String {
    if (!isRtl) return value
    return "\u2066$value\u2069"
}
