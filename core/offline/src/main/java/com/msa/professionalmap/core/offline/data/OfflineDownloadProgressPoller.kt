package com.msa.professionalmap.core.offline.data

import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

internal class OfflineDownloadProgressPoller(
    private val repository: AndroidMapLibreOfflineMapRepository,
    private val timeoutMillis: Long = DefaultTimeoutMillis,
    private val pollIntervalMillis: Long = DefaultPollIntervalMillis,
) {
    suspend fun awaitCompletion(
        request: OfflineRegionRequest,
        messages: OfflineDownloadMessages,
        onProgress: suspend (Data) -> Unit,
        onForeground: suspend (ForegroundInfo) -> Unit,
        foregroundFactory: OfflineDownloadNotificationFactory,
    ): Result = withTimeout<Result>(timeoutMillis) {
        while (true) {
            val progress = repository.state.first().downloads[request.clientId]
            if (progress != null) {
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
                if (progress.errorMessage != null) {
                    return@withTimeout Result.retry()
                }
                if (progress.isComplete) {
                    return@withTimeout Result.success(
                        OfflineDownloadWorkerDataMapper.progressData(
                            clientId = request.clientId,
                            percent = 100,
                            downloadedTiles = progress.completedTileCount,
                            totalTiles = progress.requiredResourceCount,
                            message = messages.ready(),
                        )
                    )
                }
            }
            delay(pollIntervalMillis)
        }
        Result.retry()
    }

    private companion object {
        const val DefaultTimeoutMillis = 6L * 60L * 60L * 1000L
        const val DefaultPollIntervalMillis = 750L
    }
}
