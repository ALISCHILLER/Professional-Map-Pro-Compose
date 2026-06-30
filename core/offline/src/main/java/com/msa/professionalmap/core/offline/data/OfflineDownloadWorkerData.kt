package com.msa.professionalmap.core.offline.data

import androidx.work.Data
import androidx.work.workDataOf
import com.msa.professionalmap.core.model.GeoBounds
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest

internal object OfflineDownloadWorkerKeys {
    const val ClientId = "client_id"
    const val Title = "title"
    const val StyleUrl = "style_url"
    const val SouthWestLat = "sw_lat"
    const val SouthWestLon = "sw_lon"
    const val NorthEastLat = "ne_lat"
    const val NorthEastLon = "ne_lon"
    const val MinZoom = "min_zoom"
    const val MaxZoom = "max_zoom"
    const val PixelRatio = "pixel_ratio"
    const val Ideographs = "include_ideographs"
    const val LanguageTag = "language_tag"
    const val ProgressPercent = "progress_percent"
    const val DownloadedTiles = "downloaded_tiles"
    const val TotalTiles = "total_tiles"
    const val Message = "message"
    const val Error = "error"
}

internal object OfflineDownloadWorkerDataMapper {
    fun inputData(request: OfflineRegionRequest): Data = workDataOf(
        OfflineDownloadWorkerKeys.ClientId to request.clientId,
        OfflineDownloadWorkerKeys.Title to request.title,
        OfflineDownloadWorkerKeys.StyleUrl to request.styleUrl,
        OfflineDownloadWorkerKeys.SouthWestLat to request.bounds.southWest.latitude,
        OfflineDownloadWorkerKeys.SouthWestLon to request.bounds.southWest.longitude,
        OfflineDownloadWorkerKeys.NorthEastLat to request.bounds.northEast.latitude,
        OfflineDownloadWorkerKeys.NorthEastLon to request.bounds.northEast.longitude,
        OfflineDownloadWorkerKeys.MinZoom to request.minZoom,
        OfflineDownloadWorkerKeys.MaxZoom to request.maxZoom,
        OfflineDownloadWorkerKeys.PixelRatio to request.pixelRatio,
        OfflineDownloadWorkerKeys.Ideographs to request.includeIdeographs,
    )

    fun inputData(request: OfflineRegionRequest, languageTag: String): Data = Data.Builder()
        .putAll(inputData(request))
        .putString(OfflineDownloadWorkerKeys.LanguageTag, languageTag.ifBlank { OfflineDownloadMessages.DefaultLanguageTag })
        .build()

    fun progressData(
        clientId: String,
        percent: Int,
        downloadedTiles: Long,
        totalTiles: Long,
        message: String,
    ): Data = workDataOf(
        OfflineDownloadWorkerKeys.ClientId to clientId,
        OfflineDownloadWorkerKeys.ProgressPercent to percent.coerceIn(0, 100),
        OfflineDownloadWorkerKeys.DownloadedTiles to downloadedTiles.coerceAtLeast(0L),
        OfflineDownloadWorkerKeys.TotalTiles to totalTiles.coerceAtLeast(0L),
        OfflineDownloadWorkerKeys.Message to message,
    )

    fun failureData(clientId: String?, message: String): Data = if (clientId.isNullOrBlank()) {
        workDataOf(OfflineDownloadWorkerKeys.Error to message)
    } else {
        workDataOf(
            OfflineDownloadWorkerKeys.ClientId to clientId,
            OfflineDownloadWorkerKeys.Error to message,
        )
    }

    fun Data.toOfflineRegionRequest(): OfflineRegionRequest {
        val bounds = GeoBounds(
            southWest = GeoPoint(
                latitude = getDouble(OfflineDownloadWorkerKeys.SouthWestLat, Double.NaN),
                longitude = getDouble(OfflineDownloadWorkerKeys.SouthWestLon, Double.NaN),
            ),
            northEast = GeoPoint(
                latitude = getDouble(OfflineDownloadWorkerKeys.NorthEastLat, Double.NaN),
                longitude = getDouble(OfflineDownloadWorkerKeys.NorthEastLon, Double.NaN),
            ),
        )
        return OfflineRegionRequest(
            clientId = getString(OfflineDownloadWorkerKeys.ClientId).orEmpty(),
            title = getString(OfflineDownloadWorkerKeys.Title).orEmpty(),
            styleUrl = getString(OfflineDownloadWorkerKeys.StyleUrl).orEmpty(),
            bounds = bounds,
            minZoom = getDouble(OfflineDownloadWorkerKeys.MinZoom, DefaultMinZoom),
            maxZoom = getDouble(OfflineDownloadWorkerKeys.MaxZoom, DefaultMaxZoom),
            pixelRatio = getFloat(OfflineDownloadWorkerKeys.PixelRatio, DefaultPixelRatio),
            includeIdeographs = getBoolean(OfflineDownloadWorkerKeys.Ideographs, false),
        )
    }

    private const val DefaultMinZoom = 10.0
    private const val DefaultMaxZoom = 15.0
    private const val DefaultPixelRatio = 2.0f
}
