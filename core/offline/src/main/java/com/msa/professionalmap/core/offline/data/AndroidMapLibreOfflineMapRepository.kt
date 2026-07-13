package com.msa.professionalmap.core.offline.data

import android.content.Context
import com.msa.professionalmap.core.offline.domain.OfflineDownloadProgress
import com.msa.professionalmap.core.offline.domain.OfflineManagerState
import com.msa.professionalmap.core.offline.domain.OfflineMapRepository
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

/**
 * MapLibre-backed offline repository.
 *
 * The repository owns high-level offline region orchestration and state publication. Low-level
 * MapLibre callback adapters, metadata encoding, and summary mapping live in focused helpers so
 * this class remains readable and aligned with SRP.
 */
class AndroidMapLibreOfflineMapRepository(
    context: Context,
) : OfflineMapRepository {

    private val appContext = context.applicationContext
    private val manager: OfflineManager by lazy { OfflineManager.getInstance(appContext) }
    private val observers = linkedMapOf<String, OfflineRegion>()
    private val _state = MutableStateFlow(OfflineManagerState())
    override val state: StateFlow<OfflineManagerState> = _state

    override suspend fun refreshRegions() = withContext(Dispatchers.Main) {
        updateState { it.copy(isBusy = true, lastErrorMessage = null) }
        try {
            val regions = listOfflineRegions()
            val summaries = regions.map { region -> region.toSummary(region.getStatusAwait()) }
            updateState {
                it.copy(
                    regions = summaries,
                    isBusy = false,
                    lastMessage = "Offline regions refreshed: ${summaries.size}",
                )
            }
        } catch (cancellation: CancellationException) {
            updateState { it.copy(isBusy = false) }
            throw cancellation
        } catch (_: Throwable) {
            updateState {
                it.copy(
                    isBusy = false,
                    lastErrorMessage = "Could not load offline regions.",
                )
            }
        }
    }

    override suspend fun downloadRegion(request: OfflineRegionRequest) = withContext(Dispatchers.Main) {
        updateState {
            it.copy(
                isBusy = true,
                lastErrorMessage = null,
                downloads = it.downloads + (request.clientId to request.initialProgress()),
            )
        }
        try {
            val region = findRegion(request.clientId) ?: createRegion(request)
            attachObserver(request.clientId, request.title, region)
            region.setDownloadState(OfflineRegion.STATE_ACTIVE)
            updateState {
                it.copy(
                    isBusy = false,
                    lastMessage = "Started offline download: ${request.title}",
                )
            }
            refreshRegions()
        } catch (cancellation: CancellationException) {
            updateState { it.copy(isBusy = false) }
            throw cancellation
        } catch (throwable: Throwable) {
            updateState {
                it.copy(
                    isBusy = false,
                    downloads = it.downloads + (request.clientId to request.failedProgress(throwable)),
                    lastErrorMessage = "Offline download failed.",
                )
            }
        }
    }

    override suspend fun pauseRegion(clientId: String) = withContext(Dispatchers.Main) {
        findRegion(clientId)?.setDownloadState(OfflineRegion.STATE_INACTIVE)
        markDownload(clientId, isDownloading = false, message = "Offline download paused.")
    }

    override suspend fun resumeRegion(clientId: String) = withContext(Dispatchers.Main) {
        val region = findRegion(clientId) ?: return@withContext
        val metadata = region.metadata.decodeOfflineRegionMetadata()
        attachObserver(metadata.clientId, metadata.title, region)
        region.setDownloadState(OfflineRegion.STATE_ACTIVE)
        markDownload(clientId, isDownloading = true, message = "Offline download resumed.")
    }

    override suspend fun deleteRegion(clientId: String) = withContext(Dispatchers.Main) {
        val region = findRegion(clientId) ?: return@withContext
        region.setObserver(null)
        region.deleteAwait()
        observers.remove(clientId)
        updateState {
            it.copy(
                regions = it.regions.filterNot { region -> region.clientId == clientId },
                downloads = it.downloads - clientId,
                lastMessage = "Offline region deleted.",
            )
        }
        refreshRegions()
    }

    override suspend fun setAmbientCacheSize(bytes: Long) = withContext(Dispatchers.Main) {
        require(bytes > 0L) { "Ambient cache size must be positive." }
        manager.setMaximumAmbientCacheSizeAwait(bytes)
        updateState { it.copy(lastMessage = "Ambient cache size set to ${bytes / 1024 / 1024} MB.") }
    }

    override suspend fun clearAmbientCache() = withContext(Dispatchers.Main) {
        manager.clearAmbientCacheAwait()
        updateState { it.copy(lastMessage = "Ambient cache cleared.") }
    }

    override suspend fun invalidateAmbientCache() = withContext(Dispatchers.Main) {
        manager.invalidateAmbientCacheAwait()
        updateState { it.copy(lastMessage = "Ambient cache invalidated.") }
    }

    override suspend fun packDatabase() = withContext(Dispatchers.Main) {
        manager.packDatabaseAwait()
        updateState { it.copy(lastMessage = "Offline database packed.") }
    }

    override fun close() {
        observers.values.forEach { it.setObserver(null) }
        observers.clear()
    }

    private suspend fun createRegion(request: OfflineRegionRequest): OfflineRegion {
        val definition = OfflineTilePyramidRegionDefinition(
            request.styleUrl,
            request.bounds.toLatLngBounds(),
            request.minZoom,
            request.maxZoom,
            request.pixelRatio,
            request.includeIdeographs,
        )
        return manager.createOfflineRegionAwait(definition, request.toMetadata().encode())
    }

    private suspend fun findRegion(clientId: String): OfflineRegion? {
        observers[clientId]?.let { return it }
        return listOfflineRegions().firstOrNull {
            it.metadata.decodeOfflineRegionMetadata().clientId == clientId
        }
    }

    private suspend fun listOfflineRegions(): List<OfflineRegion> = manager.listOfflineRegionsAwait().toList()

    private fun attachObserver(clientId: String, title: String, region: OfflineRegion) {
        observers[clientId] = region
        region.setObserver(RegionDownloadObserver(clientId, title, region))
    }

    private fun markDownload(clientId: String, isDownloading: Boolean, message: String) {
        updateState { state ->
            val current = state.downloads[clientId]
            if (current != null) {
                state.copy(
                    downloads = state.downloads + (clientId to current.copy(isDownloading = isDownloading)),
                    lastMessage = message,
                )
            } else {
                state.copy(lastMessage = message)
            }
        }
    }

    private fun updateState(transform: (OfflineManagerState) -> OfflineManagerState) {
        _state.update(transform)
    }

    private inner class RegionDownloadObserver(
        private val clientId: String,
        private val title: String,
        private val region: OfflineRegion,
    ) : OfflineRegion.OfflineRegionObserver {
        override fun onStatusChanged(status: OfflineRegionStatus) {
            val progress = OfflineDownloadProgress(
                sdkRegionId = region.id,
                clientId = clientId,
                title = title,
                completedResourceCount = status.completedResourceCount,
                requiredResourceCount = status.requiredResourceCount,
                completedTileCount = status.completedTileCount,
                completedBytes = status.completedResourceSize,
                isComplete = status.isComplete,
                isDownloading = !status.isComplete && status.downloadState == OfflineRegion.STATE_ACTIVE,
            )
            updateState {
                it.copy(
                    downloads = it.downloads + (clientId to progress),
                    lastMessage = if (status.isComplete) "Offline region ready: $title" else it.lastMessage,
                )
            }
        }

        override fun onError(error: OfflineRegionError) {
            val safeMessage = "Offline region error."
            updateState {
                it.copy(
                    downloads = it.downloads + (clientId to errorProgress(safeMessage)),
                    lastErrorMessage = safeMessage,
                )
            }
        }

        override fun mapboxTileCountLimitExceeded(limit: Long) {
            updateState {
                it.copy(lastErrorMessage = "Offline tile count limit exceeded: $limit tiles.")
            }
        }

        private fun errorProgress(message: String) = OfflineDownloadProgress(
            sdkRegionId = region.id,
            clientId = clientId,
            title = title,
            isDownloading = false,
            errorMessage = message,
        )
    }
}

private fun OfflineRegionRequest.initialProgress() = OfflineDownloadProgress(
    sdkRegionId = null,
    clientId = clientId,
    title = title,
    isDownloading = true,
)

private fun OfflineRegionRequest.failedProgress(@Suppress("UNUSED_PARAMETER") throwable: Throwable) = OfflineDownloadProgress(
    sdkRegionId = null,
    clientId = clientId,
    title = title,
    isDownloading = false,
    errorMessage = "Offline download failed.",
)
