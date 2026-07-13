package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.model.GeoPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

/** Performs ordered hit-testing so markers and routes win over generic map taps. */
internal class MapInteractionResolver {
    fun resolve(map: MapLibreMap, latLng: LatLng): MapSceneInteraction? {
        val screenPoint = map.projection.toScreenLocation(latLng)
        val cluster = map.queryRenderedFeatures(screenPoint, *ClusterLayers).firstOrNull()
        if (cluster != null) {
            expandCluster(map, cluster)
            return null
        }

        val poi = map.queryRenderedFeatures(screenPoint, *PoiLayers)
            .firstNotNullOfOrNull { it.stringProperty(MapStyleIds.PoiId) }
        if (poi != null) return MapSceneInteraction.Poi(poi)

        val route = map.queryRenderedFeatures(screenPoint, *RouteLayers)
            .firstNotNullOfOrNull { it.stringProperty(MapStyleIds.RouteId) }
        if (route != null) return MapSceneInteraction.Route(route)

        return MapSceneInteraction.MapPoint(GeoPoint(latLng.latitude, latLng.longitude))
    }

    private fun expandCluster(map: MapLibreMap, feature: Feature) {
        val source = map.style?.getSourceAs<GeoJsonSource>(MapStyleIds.PoiSource) ?: return
        val point = feature.geometry() as? Point ?: return
        val zoom = runCatching { source.getClusterExpansionZoom(feature) }
            .getOrDefault((map.cameraPosition.zoom + 2.0).toInt())
            .toDouble()
        map.easeCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude(), point.longitude()), zoom),
            ClusterAnimationMillis,
        )
    }

    private fun Feature.stringProperty(name: String): String? =
        if (hasProperty(name)) getStringProperty(name)?.takeIf(String::isNotBlank) else null

    companion object {
        private const val ClusterAnimationMillis = 520
        private val ClusterLayers = arrayOf(
            MapStyleIds.ClusterCountLayer,
            MapStyleIds.ClusterLayerLarge,
            MapStyleIds.ClusterLayerMedium,
            MapStyleIds.ClusterLayerSmall,
            MapStyleIds.ClusterShadowLayer,
        )
        private val PoiLayers = arrayOf(
            MapStyleIds.PoiLabelLayer,
            MapStyleIds.PoiSymbolLayer,
            "professional-poi-alert",
            "professional-poi-customer",
            "professional-poi-warehouse",
            "professional-poi-office",
            "professional-poi-general",
            MapStyleIds.PoiShadowLayer,
        )
        private val RouteLayers = arrayOf(
            MapStyleIds.RouteHitLayer,
            MapStyleIds.SelectedRouteLayer,
            MapStyleIds.SelectedRouteCasingLayer,
            MapStyleIds.AlternativeRouteLayer,
        )
    }
}
