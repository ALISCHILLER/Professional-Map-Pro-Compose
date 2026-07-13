package com.msa.professionalmap.core.mapdata

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.core.model.PoiCategory

class StaticMapRepository : MapCatalogRepository {
    override fun styles(): List<MapStyleConfig> = MapStyleConfig.DefaultStyles

    override fun referenceRoutePoints(): List<GeoPoint> = listOf(
        GeoPoint(35.68920, 51.38900),
        GeoPoint(35.69610, 51.39920),
        GeoPoint(35.70400, 51.41460),
        GeoPoint(35.71140, 51.43030),
        GeoPoint(35.71860, 51.44620),
        GeoPoint(35.72480, 51.46220),
        GeoPoint(35.73150, 51.48000),
        GeoPoint(35.73960, 51.49840),
        GeoPoint(35.74790, 51.51570),
        GeoPoint(35.75640, 51.53280),
    )

    override fun pois(): List<MapPoi> = listOf(
        poi("hq", "Head Office", "Operations center", 35.70000, 51.41000, PoiCategory.Office),
        poi("office-north", "North Office", "Regional operations", 35.70440, 51.41310, PoiCategory.Office),
        poi("service-center", "Service Center", "Support and dispatch", 35.70720, 51.41700, PoiCategory.Office),
        poi("warehouse-central", "Central Warehouse", "Primary inventory hub", 35.72120, 51.45720, PoiCategory.Warehouse),
        poi("warehouse-east", "East Warehouse", "Logistics and fulfillment", 35.72540, 51.46220, PoiCategory.Warehouse),
        poi("distribution-hub", "Distribution Hub", "Outbound loading area", 35.72810, 51.46650, PoiCategory.Warehouse),
        poi("customer-zone", "Customer Zone", "Main service territory", 35.74900, 51.51840, PoiCategory.Customer),
        poi("customer-north", "North Customer", "Scheduled visit", 35.75140, 51.52010, PoiCategory.Customer),
        poi("customer-south", "South Customer", "Delivery destination", 35.74630, 51.51520, PoiCategory.Customer),
        poi("priority-customer", "Priority Customer", "High-priority stop", 35.74820, 51.52210, PoiCategory.Customer),
        poi("checkpoint-west", "West Checkpoint", "Route control point", 35.69630, 51.39850, PoiCategory.General),
        poi("checkpoint-east", "East Checkpoint", "Route control point", 35.73930, 51.49810, PoiCategory.General),
        poi("traffic-alert", "Traffic Alert", "Possible congestion", 35.71490, 51.43720, PoiCategory.Alert),
        poi("restricted-zone", "Restricted Zone", "Access confirmation required", 35.73210, 51.48240, PoiCategory.Alert),
    )

    private fun poi(
        id: String,
        title: String,
        subtitle: String,
        latitude: Double,
        longitude: Double,
        category: PoiCategory,
    ): MapPoi = MapPoi(
        id = id,
        title = title,
        subtitle = subtitle,
        position = GeoPoint(latitude, longitude),
        category = category,
    )
}
