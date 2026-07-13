package com.msa.professionalmap.core.model

data class MapPoi(
    val id: String,
    val title: String,
    val subtitle: String,
    val position: GeoPoint,
    val category: PoiCategory = PoiCategory.General,
)

enum class PoiCategory {
    General,
    Office,
    Warehouse,
    Customer,
    Alert,
}
