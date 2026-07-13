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

/** Selected-POI emphasis isolated from the base marker stack. */
internal class MapSelectedPoiLayers {
    fun ensure(style: Style, dark: Boolean) {
        addHalo(style, dark)
        addCore(style, dark)
        addSymbol(style)
        addLabel(style, dark)
    }

    private fun addHalo(style: Style, dark: Boolean) {
        if (style.getLayer(MapStyleIds.PoiSelectedHaloLayer) != null) return
        style.addLayer(
            CircleLayer(MapStyleIds.PoiSelectedHaloLayer, MapStyleIds.PoiSelectedSource).withProperties(
                circleColor(if (dark) "#FFCA68" else "#F59E0B"),
                circleRadius(19f),
                circleOpacity(0.26f),
                circleStrokeColor(if (dark) "#FFE0A3" else "#C56A00"),
                circleStrokeWidth(2.5f),
            )
        )
    }

    private fun addCore(style: Style, dark: Boolean) {
        if (style.getLayer(MapStyleIds.PoiSelectedCoreLayer) != null) return
        style.addLayer(
            CircleLayer(MapStyleIds.PoiSelectedCoreLayer, MapStyleIds.PoiSelectedSource).withProperties(
                circleColor(if (dark) "#FFCA68" else "#F59E0B"),
                circleRadius(11.5f),
                circleOpacity(1f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(3f),
            )
        )
    }

    private fun addSymbol(style: Style) {
        if (style.getLayer(MapStyleIds.PoiSelectedSymbolLayer) != null) return
        style.addLayer(
            SymbolLayer(MapStyleIds.PoiSelectedSymbolLayer, MapStyleIds.PoiSelectedSource).withProperties(
                textField(Expression.get(MapStyleIds.MarkerSymbol)),
                textSize(10.5f),
                textColor("#263238"),
                textHaloColor("#FFFFFF"),
                textHaloWidth(0.25f),
                textAllowOverlap(true),
            )
        )
    }

    private fun addLabel(style: Style, dark: Boolean) {
        if (style.getLayer(MapStyleIds.PoiSelectedLabelLayer) != null) return
        style.addLayer(
            SymbolLayer(MapStyleIds.PoiSelectedLabelLayer, MapStyleIds.PoiSelectedSource).withProperties(
                textField(Expression.get(MapStyleIds.Title)),
                textSize(13f),
                textColor(if (dark) "#FFF4D6" else "#4B2A00"),
                textHaloColor(if (dark) "#07101E" else "#FFFFFF"),
                textHaloWidth(1.6f),
                textOffset(arrayOf(0f, 1.65f)),
                textAnchor(Property.TEXT_ANCHOR_TOP),
                textAllowOverlap(true),
            )
        )
    }
}
