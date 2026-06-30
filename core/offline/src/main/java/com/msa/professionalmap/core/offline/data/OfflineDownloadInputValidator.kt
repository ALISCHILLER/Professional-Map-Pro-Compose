package com.msa.professionalmap.core.offline.data

import com.msa.professionalmap.core.model.GeoBounds
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest

/** Validates WorkManager input before MapLibre native offline APIs are touched. */
internal object OfflineDownloadInputValidator {
    fun validate(request: OfflineRegionRequest): OfflineRegionRequest {
        require(request.clientId.isNotBlank()) { "Offline request clientId is required." }
        require(request.title.isNotBlank()) { "Offline request title is required." }
        require(request.styleUrl.startsWith("https://")) { "Offline request styleUrl must be HTTPS." }
        require(request.minZoom.isFinite() && request.maxZoom.isFinite()) { "Offline zoom levels must be finite." }
        require(request.minZoom <= request.maxZoom) { "Offline minZoom must be <= maxZoom." }
        require(request.minZoom >= 0.0 && request.maxZoom <= 22.0) { "Offline zoom levels must be in 0..22." }
        require(request.pixelRatio.isFinite() && request.pixelRatio > 0.0f) { "Offline pixelRatio must be positive." }
        validateBounds(request.bounds)
        return request
    }

    private fun validateBounds(bounds: GeoBounds) {
        val sw = bounds.southWest
        val ne = bounds.northEast
        require(sw.latitude.isFinite() && sw.longitude.isFinite() && ne.latitude.isFinite() && ne.longitude.isFinite()) {
            "Offline bounds must be finite."
        }
        require(sw.latitude in -90.0..90.0 && ne.latitude in -90.0..90.0) { "Offline latitude bounds are invalid." }
        require(sw.longitude in -180.0..180.0 && ne.longitude in -180.0..180.0) { "Offline longitude bounds are invalid." }
        require(sw.latitude <= ne.latitude) { "Offline southWest latitude must be <= northEast latitude." }
        require(sw.longitude <= ne.longitude) { "Offline southWest longitude must be <= northEast longitude." }
    }
}
