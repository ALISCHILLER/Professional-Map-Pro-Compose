package com.msa.professionalmap.core.offline.data

import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeout

/**
 * Observes MapLibre callback-driven download state until the region completes or fails.
 *
 * The historical class name is kept to avoid a needless public/internal rename, but this
 * implementation does not poll or wake on a timer. It suspends between StateFlow emissions.
 */
internal class OfflineDownloadProgressPoller(
    private val repository: AndroidMapLibreOfflineMapRepository,
    private val timeoutMillis: Long = DefaultTimeoutMillis,
) {
    suspend fun awaitCompletion(
        request: OfflineRegionRequest,
        messages: OfflineDownloadMessages,
        onProgress: suspend (Data) -> Unit,
        onForeground: suspend (ForegroundInfo) -> Unit,
        foregroundFactory: OfflineDownloadNotificationFactory,
    ): Result = withTimeout<Result>(timeoutMillis) {
        repository.state
            .mapNotNull { state -> state.downloads[request.clientId] }
            .distinctUntilChanged()
            .map { progress ->
                val percent = progress.progressPercent
                onProgress(
                    OfflineDownloadWorkerDataMapper.progressData(
                        clientId = request.clientId,
                        percent = percent,
                        downloadedTiles = progress.completedTileCount,
                        totalTiles = progress.requiredResourceCount,
                        message = progress.errorMessage ?: messages.progress(percent),
                    )
                )
                onForeground(foregroundFactory.create(request.title, percent))
                when {
                    progress.errorMessage != null -> Result.retry()
                    progress.isComplete -> Result.success(
                        OfflineDownloadWorkerDataMapper.progressData(
                            clientId = request.clientId,
                            percent = 100,
                            downloadedTiles = progress.completedTileCount,
                            totalTiles = progress.requiredResourceCount,
                            message = messages.ready(),
                        )
                    )
                    else -> null
                }
            }
            .filterNotNull()
            .first()
    }

    private companion object {
        const val DefaultTimeoutMillis = 6L * 60L * 60L * 1000L
    }
}
