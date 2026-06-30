package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.GeoPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Imperative renderer that translates the immutable Compose [MapScene] into MapLibre sources
 * and layers. The class is deliberately stateful only for rendering cache keys such as
 * style URL and last followed location timestamp.
 */
class MapSceneRenderer {
    private var lastStyleUrl: String? = null
    private var fittedRouteId: String? = null
    private var lastFollowedLocationTimestamp: Long? = null

    fun render(map: MapLibreMap, scene: MapScene) {
        if (lastStyleUrl != scene.style.url) {
            lastStyleUrl = scene.style.url
            fittedRouteId = null
            lastFollowedLocationTimestamp = null
            map.setStyle(Style.Builder().fromUri(scene.style.url)) { style ->
                renderStyle(map, style, scene)
            }
            return
        }
        map.style?.let { style -> renderStyle(map, style, scene) }
    }

    private fun renderStyle(map: MapLibreMap, style: Style, scene: MapScene) {
        val remaining = scene.remainingRoutePoints.takeIf { it.size >= 2 } ?: scene.routePoints
        upsertLineSource(style, ROUTE_SOURCE, remaining)
        upsertLineSource(style, COMPLETED_ROUTE_SOURCE, scene.completedRoutePoints)
        upsertLineSource(style, SIMPLIFIED_ROUTE_SOURCE, scene.simplifiedRoutePoints)
        upsertPointSource(style, POI_SOURCE, scene.pois.map { it.position })
        upsertPointSource(style, PROJECTED_POINT_SOURCE, listOfNotNull(scene.projectedPoint))
        upsertPointSource(style, SELECTED_POINT_SOURCE, listOfNotNull(scene.selectedPoint))
        upsertPointSource(style, USER_LOCATION_SOURCE, listOfNotNull(scene.currentLocation?.position))
        upsertPointSource(style, SNAPPED_LOCATION_SOURCE, listOfNotNull(scene.snappedLocation))
        upsertPointSource(style, INSTRUCTION_POINT_SOURCE, listOfNotNull(scene.currentInstructionPoint))

        addCompletedRouteLayerIfMissing(style)
        addRouteLayerIfMissing(style)
        addSimplifiedRouteLayerIfMissing(style)
        addPoiLayerIfMissing(style)
        addProjectedPointLayerIfMissing(style)
        addSelectedPointLayerIfMissing(style)
        addUserLocationLayerIfMissing(style)
        addSnappedLocationLayerIfMissing(style)
        addInstructionPointLayerIfMissing(style)

        if (scene.followUserLocation && scene.currentLocation != null) {
            followUser(map, scene.currentLocation)
            return
        }

        val routeSignature = scene.routePoints.joinToString(separator = "|") { "${it.latitude},${it.longitude}" }
        if (scene.routePoints.isNotEmpty() && fittedRouteId != routeSignature) {
            fittedRouteId = routeSignature
            fitCameraToRoute(map, scene.routePoints)
        }
    }

    private fun upsertLineSource(style: Style, sourceId: String, points: List<GeoPoint>) {
        val geoJson = if (points.size >= 2) {
            FeatureCollection.fromFeature(
                Feature.fromGeometry(
                    LineString.fromLngLats(points.map { Point.fromLngLat(it.longitude, it.latitude) })
                )
            )
        } else {
            FeatureCollection.fromFeatures(emptyArray<Feature>())
        }
        val source = style.getSourceAs<GeoJsonSource>(sourceId)
        if (source == null) {
            style.addSource(GeoJsonSource(sourceId, geoJson))
        } else {
            source.setGeoJson(geoJson)
        }
    }

    private fun upsertPointSource(style: Style, sourceId: String, points: List<GeoPoint>) {
        val features = points.map { point ->
            Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude))
        }
        val geoJson = FeatureCollection.fromFeatures(features)
        val source = style.getSourceAs<GeoJsonSource>(sourceId)
        if (source == null) {
            style.addSource(GeoJsonSource(sourceId, geoJson))
        } else {
            source.setGeoJson(geoJson)
        }
    }

    private fun addCompletedRouteLayerIfMissing(style: Style) {
        if (style.getLayer(COMPLETED_ROUTE_LAYER) != null) return
        style.addLayer(
            LineLayer(COMPLETED_ROUTE_LAYER, COMPLETED_ROUTE_SOURCE).withProperties(
                lineColor("#757575"),
                lineWidth(8.0f),
                lineOpacity(0.78f),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineCap(Property.LINE_CAP_ROUND),
            )
        )
    }

    private fun addRouteLayerIfMissing(style: Style) {
        if (style.getLayer(ROUTE_LAYER) != null) return
        style.addLayer(
            LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                lineColor("#1565C0"),
                lineWidth(8.0f),
                lineOpacity(0.94f),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineCap(Property.LINE_CAP_ROUND),
            )
        )
    }

    private fun addSimplifiedRouteLayerIfMissing(style: Style) {
        if (style.getLayer(SIMPLIFIED_ROUTE_LAYER) != null) return
        style.addLayer(
            LineLayer(SIMPLIFIED_ROUTE_LAYER, SIMPLIFIED_ROUTE_SOURCE).withProperties(
                lineColor("#FFFFFF"),
                lineWidth(2.2f),
                lineOpacity(0.70f),
                lineJoin(Property.LINE_JOIN_ROUND),
                lineCap(Property.LINE_CAP_ROUND),
            )
        )
    }

    private fun addPoiLayerIfMissing(style: Style) {
        if (style.getLayer(POI_LAYER) != null) return
        style.addLayer(
            CircleLayer(POI_LAYER, POI_SOURCE).withProperties(
                circleColor("#2E7D32"),
                circleRadius(7.5f),
                circleOpacity(0.95f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(2.0f),
            )
        )
    }

    private fun addProjectedPointLayerIfMissing(style: Style) {
        if (style.getLayer(PROJECTED_POINT_LAYER) != null) return
        style.addLayer(
            CircleLayer(PROJECTED_POINT_LAYER, PROJECTED_POINT_SOURCE).withProperties(
                circleColor("#F9A825"),
                circleRadius(8.0f),
                circleOpacity(0.95f),
                circleStrokeColor("#212121"),
                circleStrokeWidth(2.0f),
            )
        )
    }

    private fun addSelectedPointLayerIfMissing(style: Style) {
        if (style.getLayer(SELECTED_POINT_LAYER) != null) return
        style.addLayer(
            CircleLayer(SELECTED_POINT_LAYER, SELECTED_POINT_SOURCE).withProperties(
                circleColor("#D81B60"),
                circleRadius(9.5f),
                circleOpacity(0.95f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(2.5f),
            )
        )
    }

    private fun addUserLocationLayerIfMissing(style: Style) {
        if (style.getLayer(USER_LOCATION_LAYER) != null) return
        style.addLayer(
            CircleLayer(USER_LOCATION_LAYER, USER_LOCATION_SOURCE).withProperties(
                circleColor("#2979FF"),
                circleRadius(10.0f),
                circleOpacity(0.96f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(3.0f),
            )
        )
    }

    private fun addSnappedLocationLayerIfMissing(style: Style) {
        if (style.getLayer(SNAPPED_LOCATION_LAYER) != null) return
        style.addLayer(
            CircleLayer(SNAPPED_LOCATION_LAYER, SNAPPED_LOCATION_SOURCE).withProperties(
                circleColor("#00C853"),
                circleRadius(6.5f),
                circleOpacity(0.98f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(2.0f),
            )
        )
    }

    private fun addInstructionPointLayerIfMissing(style: Style) {
        if (style.getLayer(INSTRUCTION_POINT_LAYER) != null) return
        style.addLayer(
            CircleLayer(INSTRUCTION_POINT_LAYER, INSTRUCTION_POINT_SOURCE).withProperties(
                circleColor("#FF6D00"),
                circleRadius(7.5f),
                circleOpacity(0.92f),
                circleStrokeColor("#FFFFFF"),
                circleStrokeWidth(2.5f),
            )
        )
    }

    private fun followUser(map: MapLibreMap, location: DeviceLocation) {
        if (lastFollowedLocationTimestamp == location.timestampMillis) return
        lastFollowedLocationTimestamp = location.timestampMillis
        val target = LatLng(location.position.latitude, location.position.longitude)
        val zoom = map.cameraPosition.zoom.coerceAtLeast(15.0)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom), 650)
    }

    private fun fitCameraToRoute(map: MapLibreMap, points: List<GeoPoint>) {
        if (points.size < 2) return
        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 96), 900)
    }

    companion object {
        private const val ROUTE_SOURCE = "route-source"
        private const val ROUTE_LAYER = "route-layer"
        private const val COMPLETED_ROUTE_SOURCE = "completed-route-source"
        private const val COMPLETED_ROUTE_LAYER = "completed-route-layer"
        private const val SIMPLIFIED_ROUTE_SOURCE = "simplified-route-source"
        private const val SIMPLIFIED_ROUTE_LAYER = "simplified-route-layer"
        private const val POI_SOURCE = "poi-source"
        private const val POI_LAYER = "poi-layer"
        private const val PROJECTED_POINT_SOURCE = "projected-point-source"
        private const val PROJECTED_POINT_LAYER = "projected-point-layer"
        private const val SELECTED_POINT_SOURCE = "selected-point-source"
        private const val SELECTED_POINT_LAYER = "selected-point-layer"
        private const val USER_LOCATION_SOURCE = "user-location-source"
        private const val USER_LOCATION_LAYER = "user-location-layer"
        private const val SNAPPED_LOCATION_SOURCE = "snapped-location-source"
        private const val SNAPPED_LOCATION_LAYER = "snapped-location-layer"
        private const val INSTRUCTION_POINT_SOURCE = "instruction-point-source"
        private const val INSTRUCTION_POINT_LAYER = "instruction-point-layer"
    }
}
