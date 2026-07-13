package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.guidance.domain.toLanguageTag
import com.msa.professionalmap.core.offline.domain.OfflineDownloadManager
import com.msa.professionalmap.core.offline.domain.OfflineDownloadWorkRequest
import com.msa.professionalmap.core.offline.domain.OfflineMapRepository
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import com.msa.professionalmap.feature.map.domain.MapFeatureConfig
import com.msa.professionalmap.feature.map.domain.MapFeatureTelemetry
import com.msa.professionalmap.feature.map.domain.OfflineRoutePackResult
import com.msa.professionalmap.feature.map.domain.PrepareOfflineRoutePackUseCase
import com.msa.professionalmap.feature.map.domain.TelemetryArea
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns offline-map presentation actions and observations.
 *
 * The class keeps WorkManager enqueue policy, offline repository commands and
 * offline status-to-message mapping out of MapViewModel.
 */
internal class MapOfflineController(
    private val state: MutableStateFlow<MapUiState>,
    private val scope: CoroutineScope,
    private val offlineRepository: OfflineMapRepository,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val telemetry: MapFeatureTelemetry,
    private val featureConfig: MapFeatureConfig,
    private val prepareOfflineRoutePack: PrepareOfflineRoutePackUseCase,
) {
    private val offlineActionRunner = MapOfflineActionRunner(
        state = state,
        scope = scope,
        telemetry = telemetry,
    )
    fun observe() {
        observeRegions()
        observeWork()
    }

    fun refreshRegions() {
        offlineActionRunner.launch(TelemetryArea.OfflineRegionRefresh, "offline_region_refresh") {
            offlineRepository.refreshRegions()
        }
    }

    fun downloadCurrentRoute() {
        val current = state.value
        when (val pack = prepareOfflineRoutePack(
            style = current.selectedStyle,
            routePoints = current.routePoints,
            fallbackRoutePoints = current.referenceRoutePoints,
        )) {
            OfflineRoutePackResult.MissingStyle -> {
                state.update { it.copy(lastAction = MapUiMessage.SelectStyleFirst) }
            }
            OfflineRoutePackResult.MissingRoute -> {
                state.update { it.copy(lastAction = MapUiMessage.OfflineRouteBoundsUnavailable) }
            }
            is OfflineRoutePackResult.Ready -> enqueueDownload(pack.request)
        }
    }

    fun pauseRegion(clientId: String) {
        offlineActionRunner.launch(TelemetryArea.OfflineRegionPause, "offline_region_pause") {
            offlineRepository.pauseRegion(clientId)
        }
    }

    fun resumeRegion(clientId: String) {
        offlineActionRunner.launch(TelemetryArea.OfflineRegionResume, "offline_region_resume") {
            offlineRepository.resumeRegion(clientId)
        }
    }

    fun deleteRegion(clientId: String) {
        offlineActionRunner.launch(TelemetryArea.OfflineRegionDelete, "offline_region_delete") {
            offlineRepository.deleteRegion(clientId)
        }
    }

    fun clearAmbientCache() {
        offlineActionRunner.launch(TelemetryArea.OfflineAmbientCacheClear, "offline_ambient_cache_clear") {
            offlineRepository.clearAmbientCache()
        }
    }

    fun packDatabase() {
        offlineActionRunner.launch(TelemetryArea.OfflineDatabasePack, "offline_database_pack") {
            offlineRepository.packDatabase()
        }
    }

    fun close() {
        offlineRepository.close()
        offlineDownloadManager.close()
    }


    private fun observeRegions() {
        scope.launch {
            offlineRepository.state.collectLatest { offlineState ->
                state.update { current ->
                    current.copy(
                        offlineState = offlineState,
                        lastAction = offlineState.lastErrorMessage?.let { MapUiMessage.ExternalError("offline_region", null) }
                            ?: offlineState.lastMessage?.let { MapUiMessage.OfflineRegionStatus(it) }
                            ?: current.lastAction,
                    )
                }
            }
        }
        refreshRegions()
    }

    private fun observeWork() {
        scope.launch {
            offlineDownloadManager.state.collectLatest { workState ->
                state.update { current ->
                    current.copy(
                        offlineWorkState = workState,
                        lastAction = workState.lastErrorMessage?.let { MapUiMessage.ExternalError("offline_worker", null) }
                            ?: workState.lastMessage?.let { MapUiMessage.OfflineWorkerStatus(it) }
                            ?: current.lastAction,
                    )
                }
            }
        }
    }

    private fun enqueueDownload(request: OfflineRegionRequest) {
        scope.launch {
            val trace = telemetry.startOfflineDownloadTrace()
            try {
                offlineRepository.setAmbientCacheSize(featureConfig.offlineAmbientCacheBytes)
                val workId = offlineDownloadManager.enqueueDownload(
                    OfflineDownloadWorkRequest(
                        request = request,
                        requireUnmeteredNetwork = featureConfig.offlineRequireUnmeteredNetwork,
                        requireBatteryNotLow = featureConfig.offlineRequireBatteryNotLow,
                        requireStorageNotLow = featureConfig.offlineRequireStorageNotLow,
                        notificationLanguageTag = state.value.guidanceConfig.language.toLanguageTag(),
                    )
                )
                telemetry.offlineDownloadQueued(
                    workId = workId,
                    styleAlias = state.value.selectedStyle?.title ?: "custom_style",
                    trace = trace,
                )
                state.update { it.copy(lastAction = MapUiMessage.OfflineDownloadQueued(workId)) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                telemetry.record(TelemetryArea.OfflineDownloadEnqueue, throwable)
                state.update { it.copy(lastAction = MapUiMessage.ExternalError("offline_download_enqueue", null)) }
            } finally {
                trace.stop()
            }
        }
    }
}
