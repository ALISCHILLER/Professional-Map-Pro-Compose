package com.msa.professionalmap.core.offline.data

import com.msa.professionalmap.core.offline.domain.OfflineRegionSummary
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

internal fun OfflineRegion.toSummary(status: OfflineRegionStatus): OfflineRegionSummary {
    val metadata = metadata.decodeOfflineRegionMetadata()
    val styleUrl = when (val definition = definition) {
        is OfflineTilePyramidRegionDefinition -> definition.styleURL.orEmpty()
        else -> metadata.styleUrl
    }
    return OfflineRegionSummary(
        sdkRegionId = id,
        clientId = metadata.clientId,
        title = metadata.title,
        styleUrl = styleUrl,
        isComplete = status.isComplete,
        completedResourceCount = status.completedResourceCount,
        requiredResourceCount = status.requiredResourceCount,
        completedTileCount = status.completedTileCount,
        completedBytes = status.completedResourceSize,
    )
}
