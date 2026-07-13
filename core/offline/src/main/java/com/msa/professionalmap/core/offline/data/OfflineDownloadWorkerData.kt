package com.msa.professionalmap.core.offline.data

import android.content.Context
import androidx.work.Data
import androidx.work.workDataOf
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest

internal object OfflineDownloadWorkerKeys {
    const val ClientId = "client_id"
    const val EncryptedRequest = "encrypted_request"
    const val ProgressPercent = "progress_percent"
    const val DownloadedTiles = "downloaded_tiles"
    const val TotalTiles = "total_tiles"
    const val Message = "message"
    const val Error = "error"
}

internal object OfflineDownloadWorkerDataMapper {
    fun inputData(
        context: Context,
        request: OfflineRegionRequest,
        languageTag: String,
    ): Data {
        val validatedRequest = OfflineDownloadInputValidator.validate(request)
        val plaintext = OfflineDownloadPayloadCodec.encode(validatedRequest, languageTag.take(MaxLanguageTagLength))
        val encryptedRequest = OfflineWorkPayloadCipher(context).encrypt(plaintext)
        return workDataOf(
            OfflineDownloadWorkerKeys.ClientId to request.clientId,
            OfflineDownloadWorkerKeys.EncryptedRequest to encryptedRequest,
        )
    }

    private const val MaxLanguageTagLength = 35

    fun Data.toOfflineDownloadPayload(context: Context): OfflineDownloadPayload {
        val encryptedRequest = getString(OfflineDownloadWorkerKeys.EncryptedRequest).orEmpty()
        val plaintext = OfflineWorkPayloadCipher(context).decrypt(encryptedRequest)
        return OfflineDownloadPayloadCodec.decode(plaintext)
    }

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
}
