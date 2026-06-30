package com.msa.professionalmap.core.offline.data

import com.msa.professionalmap.core.model.GeoBounds
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import java.nio.charset.StandardCharsets

private const val METADATA_SEPARATOR = "\u001F"

internal data class MapLibreOfflineRegionMetadata(
    val clientId: String,
    val title: String,
    val styleUrl: String,
) {
    fun encode(): ByteArray = listOf(clientId, title, styleUrl)
        .joinToString(separator = METADATA_SEPARATOR) { it.replace(METADATA_SEPARATOR, " ") }
        .toByteArray(StandardCharsets.UTF_8)
}

internal fun OfflineRegionRequest.toMetadata(): MapLibreOfflineRegionMetadata = MapLibreOfflineRegionMetadata(
    clientId = clientId,
    title = title,
    styleUrl = styleUrl,
)

internal fun ByteArray.decodeOfflineRegionMetadata(): MapLibreOfflineRegionMetadata {
    val parts = toString(StandardCharsets.UTF_8).split(METADATA_SEPARATOR)
    return MapLibreOfflineRegionMetadata(
        clientId = parts.getOrNull(0).orEmpty().ifBlank { "region-${contentHashCode()}" },
        title = parts.getOrNull(1).orEmpty().ifBlank { "Offline region" },
        styleUrl = parts.getOrNull(2).orEmpty(),
    )
}

internal fun GeoBounds.toLatLngBounds(): LatLngBounds = LatLngBounds.Builder()
    .include(LatLng(southWest.latitude, southWest.longitude))
    .include(LatLng(northEast.latitude, northEast.longitude))
    .build()
