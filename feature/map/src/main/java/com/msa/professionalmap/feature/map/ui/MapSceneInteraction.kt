package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.model.GeoPoint

/** Typed map hit-test result. Cluster expansion remains an internal renderer concern. */
sealed interface MapSceneInteraction {
    data class Poi(val id: String) : MapSceneInteraction
    data class Route(val id: String) : MapSceneInteraction
    data class MapPoint(val point: GeoPoint) : MapSceneInteraction
}
