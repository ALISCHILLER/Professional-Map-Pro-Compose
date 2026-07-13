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
        val payload = runCatching {
            OfflineDownloadWorkerDataMapper.run { inputData.toOfflineDownloadPayload(applicationContext) }
        }.getOrElse {
            return Result.failure(
                OfflineDownloadWorkerDataMapper.failureData(
                    clientId = inputData.getString(KeyClientId),
                    message = "Invalid offline request.",
                )
            )
        }
        val request = runCatching { OfflineDownloadInputValidator.validate(payload.request) }.getOrElse {
            return Result.failure(
                OfflineDownloadWorkerDataMapper.failureData(
                    clientId = payload.request.clientId,
                    message = "Invalid offline request.",
                )
            )
        }
        val messages = OfflineDownloadMessages.from(payload.languageTag)
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
        } catch (_: TimeoutCancellationException) {
            Result.retry()
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (_: Exception) {
            Result.failure(
                OfflineDownloadWorkerDataMapper.failureData(
                    clientId = request.clientId,
                    message = "Offline download failed.",
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
        const val KeyEncryptedRequest = OfflineDownloadWorkerKeys.EncryptedRequest
        const val KeyProgressPercent = OfflineDownloadWorkerKeys.ProgressPercent
        const val KeyDownloadedTiles = OfflineDownloadWorkerKeys.DownloadedTiles
        const val KeyTotalTiles = OfflineDownloadWorkerKeys.TotalTiles
        const val KeyMessage = OfflineDownloadWorkerKeys.Message
        const val KeyError = OfflineDownloadWorkerKeys.Error

        fun inputData(
            context: Context,
            request: OfflineRegionRequest,
            languageTag: String,
        ): Data = OfflineDownloadWorkerDataMapper.inputData(context, request, languageTag)

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
