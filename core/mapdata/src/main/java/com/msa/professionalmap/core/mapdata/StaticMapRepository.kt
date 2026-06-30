package com.msa.professionalmap.core.mapdata

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.core.model.PoiCategory

class StaticMapRepository : MapCatalogRepository {
    override fun styles(): List<MapStyleConfig> = MapStyleConfig.DefaultStyles

    override fun referenceRoutePoints(): List<GeoPoint> = listOf(
        GeoPoint(35.68920, 51.38900), // Tehran center
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
        MapPoi(
            id = "hq",
            title = "HQ",
            subtitle = "Main office reference point",
            position = GeoPoint(35.70000, 51.41000),
            category = PoiCategory.Office,
        ),
        MapPoi(
            id = "warehouse-east",
            title = "Warehouse East",
            subtitle = "Logistics reference point",
            position = GeoPoint(35.72540, 51.46220),
            category = PoiCategory.Warehouse,
        ),
        MapPoi(
            id = "customer-zone",
            title = "Customer Zone",
            subtitle = "Customer cluster reference point",
            position = GeoPoint(35.74900, 51.51840),
            category = PoiCategory.Customer,
        ),
    )
}
