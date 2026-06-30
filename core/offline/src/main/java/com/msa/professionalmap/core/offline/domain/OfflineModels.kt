package com.msa.professionalmap.core.offline.domain

import com.msa.professionalmap.core.model.GeoBounds

/**
 * Provider-neutral offline map models. MapLibre-specific classes are kept in data layer.
 */
data class OfflineRegionRequest(
    val clientId: String,
    val title: String,
    val styleUrl: String,
    val bounds: GeoBounds,
    val minZoom: Double = 10.0,
    val maxZoom: Double = 15.0,
    val pixelRatio: Float = 2.0f,
    val includeIdeographs: Boolean = false,
) {
    init {
        require(clientId.isNotBlank()) { "clientId must not be blank." }
        require(title.isNotBlank()) { "title must not be blank." }
        require(styleUrl.isNotBlank()) { "styleUrl must not be blank." }
        require(minZoom.isFinite() && maxZoom.isFinite()) { "zoom levels must be finite." }
        require(minZoom >= 0.0) { "minZoom must be non-negative." }
        require(maxZoom >= minZoom) { "maxZoom must be greater than or equal to minZoom." }
        require(pixelRatio.isFinite() && pixelRatio > 0.0f) { "pixelRatio must be positive." }
    }
}

data class OfflineRegionSummary(
    val sdkRegionId: Long,
    val clientId: String,
    val title: String,
    val styleUrl: String,
    val isComplete: Boolean,
    val completedResourceCount: Long,
    val requiredResourceCount: Long,
    val completedTileCount: Long,
    val completedBytes: Long,
) {
    val progressFraction: Double
        get() = if (requiredResourceCount <= 0L) {
            if (isComplete) 1.0 else 0.0
        } else {
            (completedResourceCount.toDouble() / requiredResourceCount.toDouble()).coerceIn(0.0, 1.0)
        }
}

data class OfflineDownloadProgress(
    val sdkRegionId: Long?,
    val clientId: String,
    val title: String,
    val completedResourceCount: Long = 0L,
    val requiredResourceCount: Long = 0L,
    val completedTileCount: Long = 0L,
    val completedBytes: Long = 0L,
    val isComplete: Boolean = false,
    val isDownloading: Boolean = false,
    val errorMessage: String? = null,
) {
    val progressFraction: Double
        get() = if (requiredResourceCount <= 0L) {
            if (isComplete) 1.0 else 0.0
        } else {
            (completedResourceCount.toDouble() / requiredResourceCount.toDouble()).coerceIn(0.0, 1.0)
        }

    val progressPercent: Int get() = (progressFraction * 100.0).toInt().coerceIn(0, 100)
}

data class OfflineManagerState(
    val regions: List<OfflineRegionSummary> = emptyList(),
    val downloads: Map<String, OfflineDownloadProgress> = emptyMap(),
    val isBusy: Boolean = false,
    val lastMessage: String? = null,
    val lastErrorMessage: String? = null,
) {
    val activeDownloads: List<OfflineDownloadProgress>
        get() = downloads.values.sortedBy { it.title }
}

interface OfflineMapRepository : AutoCloseable {
    val state: kotlinx.coroutines.flow.StateFlow<OfflineManagerState>

    suspend fun refreshRegions()
    suspend fun downloadRegion(request: OfflineRegionRequest)
    suspend fun pauseRegion(clientId: String)
    suspend fun resumeRegion(clientId: String)
    suspend fun deleteRegion(clientId: String)
    suspend fun setAmbientCacheSize(bytes: Long)
    suspend fun clearAmbientCache()
    suspend fun invalidateAmbientCache()
    suspend fun packDatabase()
    override fun close() = Unit
}


data class OfflineDownloadWorkRequest(
    val request: OfflineRegionRequest,
    val requireUnmeteredNetwork: Boolean = false,
    val requireBatteryNotLow: Boolean = true,
    val requireStorageNotLow: Boolean = true,
    /**
     * BCP-47 language tag used only for user-visible WorkManager notifications.
     * The offline core remains independent from the guidance/localization modules.
     */
    val notificationLanguageTag: String = "en",
) {
    init {
        require(request.clientId.isNotBlank()) { "request.clientId must not be blank." }
        require(notificationLanguageTag.isNotBlank()) { "notificationLanguageTag must not be blank." }
    }
}

enum class OfflineWorkStatus {
    Idle,
    Enqueued,
    Running,
    Succeeded,
    Failed,
    Cancelled,
}

data class OfflineWorkProgress(
    val clientId: String,
    val workId: String? = null,
    val status: OfflineWorkStatus = OfflineWorkStatus.Idle,
    val progressPercent: Int = 0,
    val downloadedTiles: Long = 0L,
    val totalTiles: Long = 0L,
    val message: String? = null,
    val errorMessage: String? = null,
)

data class OfflineWorkManagerState(
    val jobs: Map<String, OfflineWorkProgress> = emptyMap(),
    val lastMessage: String? = null,
    val lastErrorMessage: String? = null,
) {
    val activeJobs: List<OfflineWorkProgress>
        get() = jobs.values.sortedBy { it.clientId }
}

interface OfflineDownloadManager : AutoCloseable {
    val state: kotlinx.coroutines.flow.StateFlow<OfflineWorkManagerState>
    suspend fun enqueueDownload(request: OfflineDownloadWorkRequest): String
    suspend fun cancel(clientId: String)
    suspend fun cancelAll()
    override fun close() = Unit
}
