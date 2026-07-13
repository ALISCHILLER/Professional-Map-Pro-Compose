package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.core.model.RouteAlternative
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Differential GeoJSON source updates for the professional map scene. */
internal class MapGeoJsonSources {
    private val cache = mutableMapOf<String, Any?>()

    fun clear() = cache.clear()

    fun update(style: Style, scene: MapScene) {
        updatePois(style, scene.pois)
        updateSelectedPoi(style, scene.selectedPoi)
        updateRouteAlternatives(style, scene)
        updateLine(style, MapStyleIds.CompletedRouteSource, scene.completedRoutePoints)
        updateLine(style, MapStyleIds.RemainingRouteSource, scene.remainingRoutePoints)
        updateLine(style, MapStyleIds.SimplifiedRouteSource, scene.simplifiedRoutePoints)
        updateEndpoints(style, scene.routeIdentity, scene.activeRoutePoints)
        updateManeuvers(style, scene.maneuverPoints)
        updatePoint(style, MapStyleIds.SelectedPointSource, scene.selectedPoint)
        updatePoint(style, MapStyleIds.ProjectedPointSource, scene.projectedPoint)
        updatePoint(style, MapStyleIds.InstructionPointSource, scene.currentInstructionPoint)
        updatePoint(style, MapStyleIds.UserLocationSource, scene.currentLocation?.position)
        updatePoint(style, MapStyleIds.SnappedLocationSource, scene.snappedLocation)
        updateHeading(style, scene)
    }

    private fun updatePois(style: Style, pois: List<MapPoi>) {
        if (!changed(MapStyleIds.PoiSource, pois)) return
        val collection = FeatureCollection.fromFeatures(pois.map(::poiFeature))
        val source = style.getSourceAs<GeoJsonSource>(MapStyleIds.PoiSource)
        if (source == null) {
            val options = GeoJsonOptions()
                .withCluster(true)
                .withClusterMaxZoom(14)
                .withClusterRadius(54)
                .withClusterMinPoints(2)
            style.addSource(GeoJsonSource(MapStyleIds.PoiSource, collection, options))
        } else {
            source.setGeoJson(collection)
        }
    }

    private fun updateSelectedPoi(style: Style, poi: MapPoi?) {
        if (!changed(MapStyleIds.PoiSelectedSource, poi)) return
        upsert(style, MapStyleIds.PoiSelectedSource, featureCollection(poi?.let(::poiFeature)))
    }

    private fun updateRouteAlternatives(style: Style, scene: MapScene) {
        val routeDataKey: Any = scene.routeAlternatives.takeIf { it.isNotEmpty() }
            ?: scene.routeIdentity
        val routesChanged = changed(
            "${MapStyleIds.RouteAlternativesSource}:routes",
            routeDataKey,
        )
        val selectionChanged = changed(
            "${MapStyleIds.RouteAlternativesSource}:selection",
            scene.selectedRouteAlternativeId,
        )
        if (!routesChanged && !selectionChanged) return

        val routes = scene.routeAlternatives.ifEmpty {
            if (scene.routePoints.size < 2) emptyList() else listOf(
                RouteAlternative(
                    id = scene.routeIdentity,
                    title = "Route",
                    summary = "",
                    points = scene.routePoints,
                    distanceMeters = 0.0,
                    durationSeconds = 0.0,
                    provider = "local-reference",
                )
            )
        }
        val selectedId = scene.selectedRouteAlternativeId ?: routes.firstOrNull()?.id
        val features = routes.mapNotNull { route ->
            if (route.points.size < 2) return@mapNotNull null
            Feature.fromGeometry(route.points.toLineString()).apply {
                addStringProperty(MapStyleIds.RouteId, route.id)
                addStringProperty(MapStyleIds.Title, route.title)
                addBooleanProperty(MapStyleIds.Selected, route.id == selectedId)
            }
        }
        upsert(style, MapStyleIds.RouteAlternativesSource, FeatureCollection.fromFeatures(features))
    }

    private fun updateEndpoints(
        style: Style,
        routeIdentity: String,
        points: List<GeoPoint>,
    ) {
        if (!changed(MapStyleIds.EndpointSource, routeIdentity)) return
        val endpoints = points.takeIf { it.size >= 2 }
            ?.let { listOf(it.first(), it.last()) }
            .orEmpty()
        val features = endpoints.mapIndexed { index, point ->
            Feature.fromGeometry(point.toMapLibrePoint()).apply {
                addStringProperty(MapStyleIds.Category, if (index == 0) "origin" else "destination")
                addStringProperty(MapStyleIds.Label, if (index == 0) "A" else "B")
            }
        }
        upsert(style, MapStyleIds.EndpointSource, FeatureCollection.fromFeatures(features))
    }

    private fun updateManeuvers(style: Style, points: List<GeoPoint>) {
        if (!changed(MapStyleIds.ManeuverSource, points)) return
        val features = points.mapIndexed { index, point ->
            Feature.fromGeometry(point.toMapLibrePoint()).apply {
                addNumberProperty(MapStyleIds.Label, index + 1)
            }
        }
        upsert(style, MapStyleIds.ManeuverSource, FeatureCollection.fromFeatures(features))
    }

    private fun updateLine(style: Style, sourceId: String, points: List<GeoPoint>) {
        if (!changed(sourceId, points)) return
        val feature = points.takeIf { it.size >= 2 }?.let { Feature.fromGeometry(it.toLineString()) }
        upsert(
            style = style,
            sourceId = sourceId,
            collection = featureCollection(feature),
        )
    }

    private fun updatePoint(style: Style, sourceId: String, point: GeoPoint?) {
        if (!changed(sourceId, point)) return
        upsert(
            style = style,
            sourceId = sourceId,
            collection = featureCollection(point?.let { Feature.fromGeometry(it.toMapLibrePoint()) }),
        )
    }

    private fun updateHeading(style: Style, scene: MapScene) {
        val location = scene.currentLocation
        val bearingDegrees = location?.bearingDegrees
        val data = location?.position to bearingDegrees
        if (!changed(MapStyleIds.UserHeadingSource, data)) return
        val feature = if (location != null && bearingDegrees != null) {
            val end = destinationPoint(location.position, bearingDegrees.toDouble(), HeadingLengthMeters)
            Feature.fromGeometry(listOf(location.position, end).toLineString())
        } else {
            null
        }
        upsert(
            style = style,
            sourceId = MapStyleIds.UserHeadingSource,
            collection = featureCollection(feature),
        )
    }

    private fun poiFeature(poi: MapPoi): Feature = Feature.fromGeometry(poi.position.toMapLibrePoint()).apply {
        addStringProperty(MapStyleIds.PoiId, poi.id)
        addStringProperty(MapStyleIds.Title, poi.title)
        addStringProperty(MapStyleIds.Subtitle, poi.subtitle)
        addStringProperty(MapStyleIds.Category, poi.category.name.lowercase())
        addStringProperty(MapStyleIds.MarkerSymbol, poi.category.markerSymbol())
    }

    private fun com.msa.professionalmap.core.model.PoiCategory.markerSymbol(): String = when (name) {
        "Office" -> "◆"
        "Warehouse" -> "■"
        "Customer" -> "●"
        "Alert" -> "!"
        else -> "•"
    }

    private fun upsert(
        style: Style,
        sourceId: String,
        collection: FeatureCollection,
    ) {
        val source = style.getSourceAs<GeoJsonSource>(sourceId)
        if (source == null) {
            style.addSource(GeoJsonSource(sourceId, collection))
        } else {
            source.setGeoJson(collection)
        }
    }

    private fun changed(key: String, value: Any?): Boolean {
        val previous = cache[key]
        if (previous === value || previous == value) return false
        cache[key] = value
        return true
    }

    private fun featureCollection(feature: Feature?): FeatureCollection =
        FeatureCollection.fromFeatures(feature?.let(::listOf).orEmpty())

    private fun List<GeoPoint>.toLineString(): LineString =
        LineString.fromLngLats(map { point -> point.toMapLibrePoint() })

    private fun GeoPoint.toMapLibrePoint(): Point = Point.fromLngLat(longitude, latitude)

    private fun destinationPoint(origin: GeoPoint, bearingDegrees: Double, distanceMeters: Double): GeoPoint {
        val angularDistance = distanceMeters / EarthRadiusMeters
        val bearing = Math.toRadians(bearingDegrees)
        val latitude = Math.toRadians(origin.latitude)
        val longitude = Math.toRadians(origin.longitude)
        val targetLatitude = asin(
            sin(latitude) * cos(angularDistance) +
                cos(latitude) * sin(angularDistance) * cos(bearing)
        )
        val targetLongitude = longitude + atan2(
            sin(bearing) * sin(angularDistance) * cos(latitude),
            cos(angularDistance) - sin(latitude) * sin(targetLatitude),
        )
        return GeoPoint(Math.toDegrees(targetLatitude), Math.toDegrees(targetLongitude).normalizeLongitude())
    }

    private fun Double.normalizeLongitude(): Double = ((this + 540.0) % 360.0) - 180.0

    companion object {
        private const val EarthRadiusMeters = 6_371_000.0
        private const val HeadingLengthMeters = 42.0
    }
}
