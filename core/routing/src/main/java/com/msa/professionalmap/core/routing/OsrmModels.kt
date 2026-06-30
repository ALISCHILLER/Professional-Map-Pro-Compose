package com.msa.professionalmap.core.routing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OsrmRouteResponse(
    val code: String,
    val message: String? = null,
    val routes: List<OsrmRoute> = emptyList(),
    val waypoints: List<OsrmWaypoint> = emptyList(),
)

@Serializable
internal data class OsrmRoute(
    val geometry: OsrmGeometry? = null,
    val legs: List<OsrmLeg> = emptyList(),
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val weight: Double? = null,
    @SerialName("weight_name") val weightName: String? = null,
)

@Serializable
internal data class OsrmLeg(
    val steps: List<OsrmStep> = emptyList(),
    val summary: String = "",
    val distance: Double = 0.0,
    val duration: Double = 0.0,
)

@Serializable
internal data class OsrmStep(
    val geometry: OsrmGeometry? = null,
    val maneuver: OsrmManeuver? = null,
    val mode: String? = null,
    val driving_side: String? = null,
    val name: String = "",
    val distance: Double = 0.0,
    val duration: Double = 0.0,
)

@Serializable
internal data class OsrmManeuver(
    val location: List<Double> = emptyList(),
    val bearing_before: Double? = null,
    val bearing_after: Double? = null,
    val type: String? = null,
    val modifier: String? = null,
    val exit: Int? = null,
)

@Serializable
internal data class OsrmGeometry(
    val type: String = "LineString",
    val coordinates: List<List<Double>> = emptyList(),
)

@Serializable
internal data class OsrmWaypoint(
    val name: String = "",
    val location: List<Double> = emptyList(),
    val distance: Double? = null,
)
