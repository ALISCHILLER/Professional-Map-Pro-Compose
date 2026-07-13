package com.msa.professionalmap.core.offline.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Observer
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.msa.professionalmap.core.offline.data.OfflineWorkInfoMapper.toOfflineWorkProgress
import com.msa.professionalmap.core.offline.domain.OfflineDownloadManager
import com.msa.professionalmap.core.offline.domain.OfflineDownloadWorkRequest
import com.msa.professionalmap.core.offline.domain.OfflineWorkManagerState
import com.msa.professionalmap.core.offline.domain.OfflineWorkProgress
import com.msa.professionalmap.core.offline.domain.OfflineWorkStatus
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** WorkManager-backed offline queue observed without polling. */
class AndroidWorkManagerOfflineDownloadManager(
    context: Context,
) : OfflineDownloadManager {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val knownWorkIds = ConcurrentHashMap<String, UUID>()
    private val _state = MutableStateFlow(OfflineWorkManagerState())
    override val state: StateFlow<OfflineWorkManagerState> = _state

    private val workInfosLiveData = workManager.getWorkInfosByTagLiveData(OfflineWorkTags.AllDownloadsTag)
    private val workInfosObserver = Observer<List<WorkInfo>> { infos ->
        runCatching { applyWorkInfos(infos.orEmpty()) }
            .onFailure { _state.update { it.copy(lastErrorMessage = PersistedObservationError) } }
    }

    init {
        mainHandler.post { workInfosLiveData.observeForever(workInfosObserver) }
    }

    override suspend fun enqueueDownload(request: OfflineDownloadWorkRequest): String {
        val clientId = request.request.clientId
        val work = OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
            .setInputData(OfflineDownloadWorker.inputData(appContext, request.request, request.notificationLanguageTag))
            .setConstraints(request.toConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkBackoffDelaySeconds, TimeUnit.SECONDS)
            .addTag(OfflineWorkTags.AllDownloadsTag)
            .addTag(OfflineWorkTags.clientTag(clientId))
            .build()

        knownWorkIds[clientId] = work.id
        workManager.enqueueUniqueWork(
            OfflineDownloadWorker.UniqueWorkPrefix + clientId,
            ExistingWorkPolicy.REPLACE,
            work,
        )
        setJob(
            clientId = clientId,
            progress = OfflineWorkProgress(
                clientId = clientId,
                workId = work.id.toString(),
                status = OfflineWorkStatus.Enqueued,
                message = QueuedMessage,
            ),
            message = QueuedMessage,
        )
        return work.id.toString()
    }

    override suspend fun cancel(clientId: String) {
        knownWorkIds.remove(clientId)?.let { workId ->
            workManager.cancelWorkById(workId)
        }
        workManager.cancelUniqueWork(OfflineDownloadWorker.UniqueWorkPrefix + clientId)
        setJob(
            clientId = clientId,
            progress = OfflineWorkProgress(clientId = clientId, status = OfflineWorkStatus.Cancelled),
            message = CancelledMessage,
            clearError = true,
        )
    }

    override suspend fun cancelAll() {
        workManager.cancelAllWorkByTag(OfflineWorkTags.AllDownloadsTag)
        knownWorkIds.clear()
        _state.value = OfflineWorkManagerState(lastMessage = QueueCancelledMessage)
    }

    override fun close() {
        mainHandler.post { workInfosLiveData.removeObserver(workInfosObserver) }
        knownWorkIds.clear()
    }

    private fun applyWorkInfos(infos: List<WorkInfo>) {
        infos.forEach { info ->
            val clientId = info.tags.firstNotNullOfOrNull(OfflineWorkTags::clientIdFrom)
                ?: return@forEach
            if (isStaleWorkInfo(clientId, info.id)) return@forEach
            // WorkManager keeps historical finished rows for a while. On process restart, do not
            // rebuild the UI from every old download. A finished item is relevant only when this
            // manager instance already knows it as the current work for the client.
            if (info.state.isFinished && knownWorkIds[clientId] == null) return@forEach
            knownWorkIds[clientId] = info.id
            val progress = info.toOfflineWorkProgress(clientId)
            setJob(
                clientId = clientId,
                progress = progress,
                message = progress.message,
                error = progress.errorMessage?.let { WorkObservationError },
                clearError = progress.errorMessage == null,
            )
            if (info.state.isFinished) {
                removeKnownWorkId(clientId, info.id)
            }
        }
    }

    private fun OfflineDownloadWorkRequest.toConstraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(if (requireUnmeteredNetwork) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(requireBatteryNotLow)
        .setRequiresStorageNotLow(requireStorageNotLow)
        .build()

    private fun isStaleWorkInfo(clientId: String, workId: UUID): Boolean {
        val currentWorkId = knownWorkIds[clientId]
        return currentWorkId != null && currentWorkId != workId
    }

    private fun removeKnownWorkId(clientId: String, workId: UUID) {
        if (knownWorkIds[clientId] == workId) knownWorkIds.remove(clientId)
    }

    private fun setJob(
        clientId: String,
        progress: OfflineWorkProgress,
        message: String? = null,
        error: String? = null,
        clearError: Boolean = progress.errorMessage == null && progress.status != OfflineWorkStatus.Failed,
    ) {
        _state.update { current ->
            current.copy(
                jobs = current.jobs + (clientId to progress),
                lastMessage = message ?: current.lastMessage,
                lastErrorMessage = when {
                    error != null -> error
                    progress.errorMessage != null -> WorkObservationError
                    clearError -> null
                    else -> current.lastErrorMessage
                },
            )
        }
    }

    private companion object {
        const val WorkBackoffDelaySeconds = 10L
        const val QueuedMessage = "Offline download queued with WorkManager."
        const val CancelledMessage = "Offline download cancelled."
        const val QueueCancelledMessage = "Offline download queue cancelled."
        const val PersistedObservationError = "Could not observe offline download queue."
        const val WorkObservationError = "Could not observe offline download."
    }
}

private val WorkInfo.State.isFinished: Boolean
    get() = this == WorkInfo.State.SUCCEEDED ||
        this == WorkInfo.State.FAILED ||
        this == WorkInfo.State.CANCELLED
