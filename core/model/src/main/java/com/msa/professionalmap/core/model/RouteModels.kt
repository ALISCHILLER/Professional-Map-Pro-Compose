package com.msa.professionalmap.core.model

data class RouteMetrics(
    val totalDistanceMeters: Double,
    val directDistanceMeters: Double,
    val initialBearingDegrees: Double,
    val simplifiedPointCount: Int,
    val originalPointCount: Int,
) {
    val totalDistanceKm: Double get() = totalDistanceMeters / 1000.0
    val directDistanceKm: Double get() = directDistanceMeters / 1000.0
}
