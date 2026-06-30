package com.msa.professionalmap.core.mapdata

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi

/**
 * Stable catalog boundary for map styles, reference routes and points of interest.
 *
 * Feature modules depend on this abstraction instead of a concrete data source so
 * the app can swap static assets for remote config, Room, or a backend without
 * touching presentation code.
 */
interface MapCatalogRepository {
    fun styles(): List<MapStyleConfig>
    fun referenceRoutePoints(): List<GeoPoint>
    fun pois(): List<MapPoi>
}
