package com.msa.professionalmap.feature.map.ui

import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer

/** Category-aware clustered POI layers and route/navigation point markers. */
internal class MapMarkerLayers {
    private val selectedPoiLayers = MapSelectedPoiLayers()

    fun ensure(style: Style, dark: Boolean) {
        addClusters(style, dark)
        addPoiLayers(style, dark)
        selectedPoiLayers.ensure(style, dark)
        addEndpointLayers(style, dark)
        addManeuverLayers(style, dark)
        addNavigationPointLayers(style, dark)
    }

    private fun addClusters(style: Style, dark: Boolean) {
        if (style.getLayer(MapStyleIds.ClusterShadowLayer) == null) {
            style.addLayer(
                CircleLayer(MapStyleIds.ClusterShadowLayer, MapStyleIds.PoiSource).withFilter(
                    Expression.has(MapStyleIds.PointCount)
                ).withProperties(
                    circleColor("#000000"),
                    circleRadius(31f),
                    circleOpacity(if (dark) 0.34f else 0.18f),
                )
            )
        }
        addClusterCircle(style, MapStyleIds.ClusterLayerSmall, 0, 9, if (dark) "#5B8CFF" else "#1F5EFF", 18f)
        addClusterCircle(style, MapStyleIds.ClusterLayerMedium, 10, 49, if (dark) "#35C7A4" else "#007D68", 23f)
        addClusterCircle(style, MapStyleIds.ClusterLayerLarge, 50, Int.MAX_VALUE, if (dark) "#FFB45E" else "#9A5B00", 29f)
        if (style.getLayer(MapStyleIds.ClusterCountLayer) == null) {
            style.addLayer(
                SymbolLayer(MapStyleIds.ClusterCountLayer, MapStyleIds.PoiSource).withFilter(
                    Expression.has(MapStyleIds.PointCount)
                ).withProperties(
                    textField(Expression.get("point_count_abbreviated")),
                    textSize(13f),
                    textColor("#FFFFFF"),
                    textHaloColor("#000000"),
                    textHaloWidth(0.6f),
                    textAllowOverlap(true),
                )
            )
        }
    }

    private fun addClusterCircle(
        style: Style,
        layerId: String,
        minimum: Int,
        maximum: Int,
        color: String,
        radius: Float,
    ) {
        if (style.getLayer(layerId) != null) return
        val count = Expression.toNumber(Expression.get(MapStyleIds.PointCount))
        val range = if (maximum == Int.MAX_VALUE) {
            Expression.gte(count, Expression.literal(minimum))
        } else {
            Expression.all(
                Expression.gte(count, Expression.literal(minimum)),
                Expression.lte(count, Expression.literal(maximum)),
            )
        }
        style.addLayer(
            CircleLayer(layerId, MapStyleIds.PoiSource).withFilter(
                Expression.all(Expression.has(MapStyleIds.PointCount), range)
            ).withProperties(
                circleColor(color),
                circleRadius(radius),
                circleOpacity(0.94f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(3f),
            )
        )
    }

    private fun addPoiLayers(style: Style, dark: Boolean) {
        val unclustered = Expression.not(Expression.has(MapStyleIds.PointCount))
        if (style.getLayer(MapStyleIds.PoiShadowLayer) == null) {
            style.addLayer(
                CircleLayer(MapStyleIds.PoiShadowLayer, MapStyleIds.PoiSource).withFilter(unclustered)
                    .withProperties(
                        circleColor("#000000"),
                        circleRadius(14f),
                        circleOpacity(if (dark) 0.36f else 0.20f),
                    )
            )
        }
        categoryColors(dark).forEach { (category, color) ->
            val layerId = "professional-poi-$category"
            if (style.getLayer(layerId) == null) {
                style.addLayer(
                    CircleLayer(layerId, MapStyleIds.PoiSource).withFilter(
                        Expression.all(
                            unclustered,
                            Expression.eq(Expression.get(MapStyleIds.Category), Expression.literal(category)),
                        )
                    ).withProperties(
                        circleColor(color),
                        circleRadius(10.5f),
                        circleOpacity(0.98f),
                        circleStrokeColor("#FFFFFF"),
                        circleStrokeWidth(2.5f),
                    )
                )
            }
        }
        if (style.getLayer(MapStyleIds.PoiSymbolLayer) == null) {
            style.addLayer(
                SymbolLayer(MapStyleIds.PoiSymbolLayer, MapStyleIds.PoiSource).withFilter(unclustered)
                    .withProperties(
                        textField(Expression.get(MapStyleIds.MarkerSymbol)),
                        textSize(10.5f),
                        textColor("#FFFFFF"),
                        textHaloColor("#000000"),
                        textHaloWidth(0.35f),
                        textAllowOverlap(true),
                    )
            )
        }
        if (style.getLayer(MapStyleIds.PoiLabelLayer) == null) {
            val labelLayer = SymbolLayer(MapStyleIds.PoiLabelLayer, MapStyleIds.PoiSource)
                .withFilter(unclustered)
                .withProperties(
                    textField(Expression.get(MapStyleIds.Title)),
                    textSize(11.5f),
                    textColor(if (dark) "#F4F7FF" else "#101828"),
                    textHaloColor(if (dark) "#07101E" else "#FFFFFF"),
                    textHaloWidth(1.4f),
                    textOffset(arrayOf(0f, 1.55f)),
                    textAnchor(Property.TEXT_ANCHOR_TOP),
                )
            labelLayer.setMinZoom(12.25f)
            style.addLayer(labelLayer)
        }
    }

    private fun addEndpointLayers(style: Style, dark: Boolean) {
        if (style.getLayer(MapStyleIds.EndpointShadowLayer) == null) {
            style.addLayer(
                CircleLayer(MapStyleIds.EndpointShadowLayer, MapStyleIds.EndpointSource).withProperties(
                    circleColor("#000000"), circleRadius(15f), circleOpacity(if (dark) 0.4f else 0.24f)
                )
            )
        }
        if (style.getLayer(MapStyleIds.EndpointCircleLayer) == null) {
            style.addLayer(
                CircleLayer(MapStyleIds.EndpointCircleLayer, MapStyleIds.EndpointSource)
                    .withFilter(
                        Expression.eq(
                            Expression.get(MapStyleIds.Category),
                            Expression.literal("origin"),
                        )
                    )
                    .withProperties(
                        circleColor(if (dark) "#79A3FF" else "#1F5EFF"),
                        circleRadius(12f),
                        circleOpacity(0.98f),
                        circleStrokeColor("#FFFFFF"),
                        circleStrokeWidth(3f),
                    )
            )
        }
        if (style.getLayer(MapStyleIds.EndpointDestinationLayer) == null) {
            style.addLayer(
                CircleLayer(MapStyleIds.EndpointDestinationLayer, MapStyleIds.EndpointSource)
                    .withFilter(
                        Expression.eq(
                            Expression.get(MapStyleIds.Category),
                            Expression.literal("destination"),
                        )
                    )
                    .withProperties(
                        circleColor(if (dark) "#FF8FBC" else "#D92D69"),
                        circleRadius(12f),
                        circleOpacity(0.98f),
                        circleStrokeColor("#FFFFFF"),
                        circleStrokeWidth(3f),
                    )
            )
        }
        if (style.getLayer(MapStyleIds.EndpointLabelLayer) == null) {
            style.addLayer(
                SymbolLayer(MapStyleIds.EndpointLabelLayer, MapStyleIds.EndpointSource).withProperties(
                    textField(Expression.get(MapStyleIds.Label)),
                    textSize(11.5f),
                    textColor("#FFFFFF"),
                    textAllowOverlap(true),
                )
            )
        }
    }

    private fun addManeuverLayers(style: Style, dark: Boolean) {
        if (style.getLayer(MapStyleIds.ManeuverCircleLayer) == null) {
            style.addLayer(
                CircleLayer(MapStyleIds.ManeuverCircleLayer, MapStyleIds.ManeuverSource).withProperties(
                    circleColor(if (dark) "#455A64" else "#FFFFFF"),
                    circleRadius(7f),
                    circleOpacity(0.92f),
                    circleStrokeColor(if (dark) "#CFD8DC" else "#37474F"),
                    circleStrokeWidth(1.5f),
                )
            )
        }
        if (style.getLayer(MapStyleIds.ManeuverLabelLayer) == null) {
            style.addLayer(
                SymbolLayer(MapStyleIds.ManeuverLabelLayer, MapStyleIds.ManeuverSource).withProperties(
                    textField(Expression.toString(Expression.get(MapStyleIds.Label))),
                    textSize(8.5f),
                    textColor(if (dark) "#FFFFFF" else "#263238"),
                    textAllowOverlap(true),
                )
            )
        }
    }

    private fun addNavigationPointLayers(style: Style, dark: Boolean) {
        addCircle(style, MapStyleIds.SelectedPointHaloLayer, MapStyleIds.SelectedPointSource, "#F0528A", 17f, 0.22f, "#F8BBD0", 1.5f)
        addCircle(style, MapStyleIds.SelectedPointLayer, MapStyleIds.SelectedPointSource, "#D92D69", 9.5f, 0.98f, "#FFFFFF", 2.5f)
        addCircle(style, MapStyleIds.ProjectedPointLayer, MapStyleIds.ProjectedPointSource, "#E99A00", 7.5f, 0.94f, "#212121", 2f)
        addCircle(style, MapStyleIds.InstructionPointLayer, MapStyleIds.InstructionPointSource, "#F97316", 8f, 0.96f, "#FFFFFF", 2.5f)
        addCircle(style, MapStyleIds.UserLocationHaloLayer, MapStyleIds.UserLocationSource, "#2F6BFF", 20f, if (dark) 0.18f else 0.14f, "#9CB7FF", 1.5f)
        addCircle(style, MapStyleIds.UserLocationLayer, MapStyleIds.UserLocationSource, "#2F6BFF", 10.5f, 1f, "#FFFFFF", 3f)
        addCircle(style, MapStyleIds.SnappedLocationLayer, MapStyleIds.SnappedLocationSource, "#00A889", 5.5f, 0.98f, "#FFFFFF", 2f)
    }

    private fun addCircle(
        style: Style,
        layerId: String,
        sourceId: String,
        color: String,
        radius: Float,
        opacity: Float,
        strokeColor: String,
        strokeWidth: Float,
    ) {
        if (style.getLayer(layerId) != null) return
        style.addLayer(
            CircleLayer(layerId, sourceId).withProperties(
                circleColor(color),
                circleRadius(radius),
                circleOpacity(opacity),
                circleStrokeColor(strokeColor),
                circleStrokeWidth(strokeWidth),
            )
        )
    }

    private fun categoryColors(dark: Boolean): Map<String, String> = mapOf(
        "general" to if (dark) "#A5B4C7" else "#5B6676",
        "office" to if (dark) "#6F9CFF" else "#1F5EFF",
        "warehouse" to if (dark) "#FFC27A" else "#C56A00",
        "customer" to if (dark) "#62D8B5" else "#007D68",
        "alert" to if (dark) "#FF7B75" else "#C43131",
    )
}
