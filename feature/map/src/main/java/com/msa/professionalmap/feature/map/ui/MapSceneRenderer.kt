package com.msa.professionalmap.feature.map.ui

import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/** Coordinates differential sources, one-time layer setup and navigation-aware camera updates. */
internal class MapSceneRenderer {
    private val sources = MapGeoJsonSources()
    private val routeLayers = MapRouteLayers()
    private val markerLayers = MapMarkerLayers()
    private val camera = MapCameraController()
    private var styleUrl: String? = null
    private var preparedStyle: Style? = null

    fun invalidateStyle() {
        styleUrl = null
        preparedStyle = null
        sources.clear()
        camera.reset()
    }

    fun render(map: MapLibreMap, scene: MapScene) {
        if (styleUrl != scene.style.url) {
            styleUrl = scene.style.url
            preparedStyle = null
            sources.clear()
            camera.reset()
            map.setStyle(Style.Builder().fromUri(scene.style.url)) { style ->
                renderStyle(map, style, scene)
            }
            return
        }
        map.style?.let { renderStyle(map, it, scene) }
    }

    private fun renderStyle(map: MapLibreMap, style: Style, scene: MapScene) {
        sources.update(style, scene)
        if (preparedStyle !== style) {
            routeLayers.ensure(style, scene.style.isDark)
            markerLayers.ensure(style, scene.style.isDark)
            preparedStyle = style
        }
        camera.update(map, scene)
    }
}
