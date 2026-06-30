package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi

/**
 * Immutable render model for MapLibre. UI/presentation decides what should be drawn; renderer only
 * translates this state to MapLibre sources and layers.
 */
data class MapScene(
    val style: MapStyleConfig,
    val routePoints: List<GeoPoint>,
    val simplifiedRoutePoints: List<GeoPoint>,
    val completedRoutePoints: List<GeoPoint>,
    val remainingRoutePoints: List<GeoPoint>,
    val pois: List<MapPoi>,
    val selectedPoint: GeoPoint?,
    val projectedPoint: GeoPoint?,
    val currentLocation: DeviceLocation?,
    val snappedLocation: GeoPoint?,
    val currentInstructionPoint: GeoPoint?,
    val followUserLocation: Boolean,
)
