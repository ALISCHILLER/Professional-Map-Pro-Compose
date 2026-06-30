package com.msa.professionalmap.core.offline.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

class OfflineDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val request = runCatching {
            OfflineDownloadInputValidator.validate(OfflineDownloadWorkerDataMapper.run { inputData.toOfflineRegionRequest() })
        }.getOrElse { throwable ->
            return Result.failure(
                OfflineDownloadWorkerDataMapper.failureData(
                    clientId = inputData.getString(KeyClientId),
                    message = throwable.message ?: "Invalid offline request.",
                )
            )
        }
        val messages = OfflineDownloadMessages.from(inputData.getString(KeyLanguageTag))
        val notificationFactory = OfflineDownloadNotificationFactory(applicationContext, messages)

        setForeground(notificationFactory.create(request.title, 0))
        setProgress(OfflineDownloadWorkerDataMapper.progressData(request.clientId, 0, 0L, 0L, messages.queued()))

        val repository = AndroidMapLibreOfflineMapRepository(applicationContext)
        return try {
            repository.setAmbientCacheSize(DefaultAmbientCacheBytes)
            repository.downloadRegion(request)
            OfflineDownloadProgressPoller(repository).awaitCompletion(
                request = request,
                messages = messages,
                onProgress = { data -> setProgress(data) },
                onForeground = { foregroundInfo -> setForeground(foregroundInfo) },
                foregroundFactory = notificationFactory,
            )
        } catch (throwable: TimeoutCancellationException) {
            Result.retry()
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Exception) {
            Result.failure(
                OfflineDownloadWorkerDataMapper.failureData(
                    clientId = request.clientId,
                    message = throwable.message ?: "Offline download failed.",
                )
            )
        } finally {
            repository.close()
        }
    }

    companion object {
        const val UniqueWorkPrefix = "offline-map-region-"
        private const val DefaultAmbientCacheBytes = 256L * 1024L * 1024L

        const val KeyClientId = OfflineDownloadWorkerKeys.ClientId
        const val KeyTitle = OfflineDownloadWorkerKeys.Title
        const val KeyStyleUrl = OfflineDownloadWorkerKeys.StyleUrl
        const val KeySouthWestLat = OfflineDownloadWorkerKeys.SouthWestLat
        const val KeySouthWestLon = OfflineDownloadWorkerKeys.SouthWestLon
        const val KeyNorthEastLat = OfflineDownloadWorkerKeys.NorthEastLat
        const val KeyNorthEastLon = OfflineDownloadWorkerKeys.NorthEastLon
        const val KeyMinZoom = OfflineDownloadWorkerKeys.MinZoom
        const val KeyMaxZoom = OfflineDownloadWorkerKeys.MaxZoom
        const val KeyPixelRatio = OfflineDownloadWorkerKeys.PixelRatio
        const val KeyIdeographs = OfflineDownloadWorkerKeys.Ideographs
        const val KeyLanguageTag = OfflineDownloadWorkerKeys.LanguageTag
        const val KeyProgressPercent = OfflineDownloadWorkerKeys.ProgressPercent
        const val KeyDownloadedTiles = OfflineDownloadWorkerKeys.DownloadedTiles
        const val KeyTotalTiles = OfflineDownloadWorkerKeys.TotalTiles
        const val KeyMessage = OfflineDownloadWorkerKeys.Message
        const val KeyError = OfflineDownloadWorkerKeys.Error

        fun inputData(request: OfflineRegionRequest): Data = OfflineDownloadWorkerDataMapper.inputData(request)

        fun inputData(request: OfflineRegionRequest, languageTag: String): Data =
            OfflineDownloadWorkerDataMapper.inputData(request, languageTag)

        fun progressData(
            clientId: String,
            percent: Int,
            downloadedTiles: Long,
            totalTiles: Long,
            message: String,
        ): Data = OfflineDownloadWorkerDataMapper.progressData(
            clientId = clientId,
            percent = percent,
            downloadedTiles = downloadedTiles,
            totalTiles = totalTiles,
            message = message,
        )
    }
}
