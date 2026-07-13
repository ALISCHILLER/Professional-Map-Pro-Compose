package com.msa.professionalmap.feature.map.ui

import androidx.compose.runtime.Immutable
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.core.model.RouteAlternative

/** Immutable render model consumed by the MapLibre renderer. */
@Immutable
data class MapScene(
    val style: MapStyleConfig,
    val routeIdentity: String,
    val routePoints: List<GeoPoint>,
    val routeAlternatives: List<RouteAlternative>,
    val selectedRouteAlternativeId: String?,
    val selectedRoute: RouteAlternative?,
    val activeRoutePoints: List<GeoPoint>,
    val maneuverPoints: List<GeoPoint>,
    val simplifiedRoutePoints: List<GeoPoint>,
    val completedRoutePoints: List<GeoPoint>,
    val remainingRoutePoints: List<GeoPoint>,
    val pois: List<MapPoi>,
    val selectedPoiId: String?,
    val selectedPoi: MapPoi?,
    val selectedPoint: GeoPoint?,
    val projectedPoint: GeoPoint?,
    val currentLocation: DeviceLocation?,
    val snappedLocation: GeoPoint?,
    val currentInstructionPoint: GeoPoint?,
    val followUserLocation: Boolean,
    val navigationActive: Boolean,
)
