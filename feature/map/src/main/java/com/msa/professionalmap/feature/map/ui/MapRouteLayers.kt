package com.msa.professionalmap.feature.map.ui

import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth

/** Professional route hierarchy: alternatives, selected casing, progress and heading. */
internal class MapRouteLayers {
    fun ensure(style: Style, dark: Boolean) {
        val selected = Expression.eq(Expression.get(MapStyleIds.Selected), Expression.literal(true))
        val notSelected = Expression.eq(Expression.get(MapStyleIds.Selected), Expression.literal(false))

        addLine(
            style = style,
            id = MapStyleIds.RouteHitLayer,
            source = MapStyleIds.RouteAlternativesSource,
            color = "#000000",
            width = 24f,
            opacity = 0.01f,
        )
        addLine(
            style = style,
            id = MapStyleIds.AlternativeRouteLayer,
            source = MapStyleIds.RouteAlternativesSource,
            color = if (dark) "#7D8EA6" else "#738096",
            width = 5f,
            opacity = if (dark) 0.68f else 0.58f,
            filter = notSelected,
        )
        addLine(
            style = style,
            id = MapStyleIds.SelectedRouteCasingLayer,
            source = MapStyleIds.RouteAlternativesSource,
            color = if (dark) "#061224" else "#FFFFFF",
            width = 13f,
            opacity = 0.96f,
            filter = selected,
        )
        addLine(
            style = style,
            id = MapStyleIds.SelectedRouteLayer,
            source = MapStyleIds.RouteAlternativesSource,
            color = if (dark) "#6F9CFF" else "#1F5EFF",
            width = 8f,
            opacity = 0.98f,
            filter = selected,
        )
        addLine(style, MapStyleIds.CompletedRouteLayer, MapStyleIds.CompletedRouteSource, "#7D8EA6", 8f, 0.70f)
        addLine(
            style,
            MapStyleIds.RemainingRouteCasingLayer,
            MapStyleIds.RemainingRouteSource,
            if (dark) "#051020" else "#FFFFFF",
            13f,
            0.98f,
        )
        addLine(
            style,
            MapStyleIds.RemainingRouteLayer,
            MapStyleIds.RemainingRouteSource,
            if (dark) "#79A3FF" else "#174ECC",
            8.5f,
            1f,
        )
        addLine(
            style,
            MapStyleIds.SimplifiedRouteLayer,
            MapStyleIds.SimplifiedRouteSource,
            if (dark) "#DDE7FF" else "#FFFFFF",
            2.0f,
            if (dark) 0.48f else 0.62f,
        )
        addLine(
            style,
            MapStyleIds.UserHeadingLayer,
            MapStyleIds.UserHeadingSource,
            "#2F6BFF",
            4f,
            0.82f,
        )
    }

    private fun addLine(
        style: Style,
        id: String,
        source: String,
        color: String,
        width: Float,
        opacity: Float,
        filter: Expression? = null,
    ) {
        if (style.getLayer(id) != null) return
        val layer = LineLayer(id, source).withProperties(
            lineColor(color),
            lineWidth(width),
            lineOpacity(opacity),
            lineJoin(Property.LINE_JOIN_ROUND),
            lineCap(Property.LINE_CAP_ROUND),
        )
        if (filter != null) layer.setFilter(filter)
        style.addLayer(layer)
    }
}
