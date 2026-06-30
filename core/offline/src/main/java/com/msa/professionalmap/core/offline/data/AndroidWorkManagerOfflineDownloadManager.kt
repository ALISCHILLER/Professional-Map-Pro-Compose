package com.msa.professionalmap.core.offline.data

import android.content.Context
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AndroidWorkManagerOfflineDownloadManager(
    context: Context,
) : OfflineDownloadManager {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val knownWorkIds = ConcurrentHashMap<String, UUID>()
    private val ignoredFinishedWorkIds = ConcurrentHashMap.newKeySet<UUID>()
    private val observerJobs = ConcurrentHashMap<String, Job>()
    private val _state = MutableStateFlow(OfflineWorkManagerState())
    override val state: StateFlow<OfflineWorkManagerState> = _state

    init {
        observePersistedWork()
    }

    override suspend fun enqueueDownload(request: OfflineDownloadWorkRequest): String {
        val work = OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
            .setInputData(OfflineDownloadWorker.inputData(request.request, request.notificationLanguageTag))
            .setConstraints(request.toConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkBackoffDelaySeconds, TimeUnit.SECONDS)
            .addTag(OfflineWorkTags.AllDownloadsTag)
            .addTag(OfflineWorkTags.clientTag(request.request.clientId))
            .build()

        knownWorkIds[request.request.clientId] = work.id
        workManager.enqueueUniqueWork(
            OfflineDownloadWorker.UniqueWorkPrefix + request.request.clientId,
            ExistingWorkPolicy.REPLACE,
            work,
        )
        setJob(
            clientId = request.request.clientId,
            progress = OfflineWorkProgress(
                clientId = request.request.clientId,
                workId = work.id.toString(),
                status = OfflineWorkStatus.Enqueued,
                message = QueuedMessage,
            ),
            message = QueuedMessage,
        )
        observeWork(request.request.clientId, work.id)
        return work.id.toString()
    }

    override suspend fun cancel(clientId: String) {
        knownWorkIds.remove(clientId)?.let { workId ->
            ignoredFinishedWorkIds.add(workId)
            workManager.cancelWorkById(workId)
        }
        observerJobs.remove(clientId)?.cancel()
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
        ignoredFinishedWorkIds.addAll(knownWorkIds.values)
        knownWorkIds.clear()
        observerJobs.values.forEach { it.cancel() }
        observerJobs.clear()
        _state.update { OfflineWorkManagerState(lastMessage = QueueCancelledMessage) }
    }

    override fun close() {
        scope.cancel()
        knownWorkIds.clear()
        ignoredFinishedWorkIds.clear()
        observerJobs.clear()
    }

    private fun observePersistedWork() {
        scope.launch {
            while (true) {
                try {
                    val infos = awaitWorkInfosByTag(OfflineWorkTags.AllDownloadsTag)
                    infos.forEach { info ->
                        val clientId = info.tags.firstNotNullOfOrNull(OfflineWorkTags::clientIdFrom)
                            ?: return@forEach
                        if (ignoredFinishedWorkIds.contains(info.id) || isStaleWorkInfo(clientId, info.id)) {
                            return@forEach
                        }
                        knownWorkIds[clientId] = info.id
                        val progress = info.toOfflineWorkProgress(clientId)
                        setJob(
                            clientId = clientId,
                            progress = progress,
                            message = progress.message,
                            error = progress.errorMessage,
                            clearError = progress.errorMessage == null,
                        )
                        if (info.state.isFinished) {
                            ignoredFinishedWorkIds.add(info.id)
                            removeKnownWorkId(clientId, info.id)
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    _state.update {
                        it.copy(lastErrorMessage = throwable.message ?: PersistedObservationError)
                    }
                }
                delay(WorkObservationPollIntervalMillis)
            }
        }
    }

    private fun observeWork(clientId: String, workId: UUID) {
        observerJobs.remove(clientId)?.cancel()
        observerJobs[clientId] = scope.launch {
            while (true) {
                try {
                    val info = awaitWorkInfoById(workId)
                    if (info != null) {
                        knownWorkIds[clientId] = info.id
                        val progress = info.toOfflineWorkProgress(clientId)
                        setJob(
                            clientId = clientId,
                            progress = progress,
                            message = progress.message,
                            error = progress.errorMessage,
                            clearError = progress.errorMessage == null,
                        )
                        if (info.state.isFinished) {
                            ignoredFinishedWorkIds.add(info.id)
                            removeKnownWorkId(clientId, info.id)
                            observerJobs.remove(clientId)
                            return@launch
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    val message = throwable.message ?: WorkObservationError
                    setJob(
                        clientId = clientId,
                        progress = OfflineWorkProgress(
                            clientId = clientId,
                            workId = workId.toString(),
                            status = OfflineWorkStatus.Failed,
                            errorMessage = message,
                        ),
                        error = message,
                    )
                }
                delay(WorkObservationPollIntervalMillis)
            }
        }
    }

    private suspend fun awaitWorkInfosByTag(tag: String): List<WorkInfo> = withContext(Dispatchers.IO) {
        workManager.getWorkInfosByTag(tag).get()
    }

    private suspend fun awaitWorkInfoById(workId: UUID): WorkInfo? = withContext(Dispatchers.IO) {
        workManager.getWorkInfoById(workId).get()
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
        if (knownWorkIds[clientId] == workId) {
            knownWorkIds.remove(clientId)
        }
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
                    progress.errorMessage != null -> progress.errorMessage
                    clearError -> null
                    else -> current.lastErrorMessage
                },
            )
        }
    }

    private companion object {
        const val WorkBackoffDelaySeconds = 10L
        const val WorkObservationPollIntervalMillis = 1_000L
        const val QueuedMessage = "Offline download queued with WorkManager."
        const val CancelledMessage = "Offline download cancelled."
        const val QueueCancelledMessage = "Offline download queue cancelled."
        const val PersistedObservationError = "Could not observe offline download queue."
        const val WorkObservationError = "Could not observe offline download."
    }
}

private val WorkInfo.State.isFinished: Boolean
    get() = this == WorkInfo.State.SUCCEEDED || this == WorkInfo.State.FAILED || this == WorkInfo.State.CANCELLED
