package com.msa.professionalmap.core.offline.data

import androidx.work.WorkInfo
import com.msa.professionalmap.core.offline.domain.OfflineWorkProgress
import com.msa.professionalmap.core.offline.domain.OfflineWorkStatus

internal object OfflineWorkInfoMapper {
    fun WorkInfo.toOfflineWorkProgress(clientId: String): OfflineWorkProgress {
        val progressData = progress
        val output = outputData
        val error = output.getString(OfflineDownloadWorker.KeyError)
            ?: progressData.getString(OfflineDownloadWorker.KeyError)
        val message = output.getString(OfflineDownloadWorker.KeyMessage)
            ?: progressData.getString(OfflineDownloadWorker.KeyMessage)
        return OfflineWorkProgress(
            clientId = progressData.getString(OfflineDownloadWorker.KeyClientId)
                ?: output.getString(OfflineDownloadWorker.KeyClientId)
                ?: clientId,
            workId = id.toString(),
            status = state.toOfflineWorkStatus(),
            progressPercent = progressData.getInt(
                OfflineDownloadWorker.KeyProgressPercent,
                if (state == WorkInfo.State.SUCCEEDED) 100 else 0,
            ).coerceIn(0, 100),
            downloadedTiles = progressData.getLong(OfflineDownloadWorker.KeyDownloadedTiles, 0L).coerceAtLeast(0L),
            totalTiles = progressData.getLong(OfflineDownloadWorker.KeyTotalTiles, 0L).coerceAtLeast(0L),
            message = message,
            errorMessage = error,
        )
    }

    private fun WorkInfo.State.toOfflineWorkStatus(): OfflineWorkStatus = when (this) {
        WorkInfo.State.ENQUEUED -> OfflineWorkStatus.Enqueued
        WorkInfo.State.RUNNING -> OfflineWorkStatus.Running
        WorkInfo.State.SUCCEEDED -> OfflineWorkStatus.Succeeded
        WorkInfo.State.FAILED -> OfflineWorkStatus.Failed
        WorkInfo.State.BLOCKED -> OfflineWorkStatus.Enqueued
        WorkInfo.State.CANCELLED -> OfflineWorkStatus.Cancelled
    }
}
