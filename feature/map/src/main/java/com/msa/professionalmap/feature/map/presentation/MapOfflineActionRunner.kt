package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.feature.map.domain.MapFeatureTelemetry
import com.msa.professionalmap.feature.map.domain.TelemetryArea
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Runs offline fire-and-forget presentation commands with consistent telemetry and UI errors.
 */
internal class MapOfflineActionRunner(
    private val state: MutableStateFlow<MapUiState>,
    private val scope: CoroutineScope,
    private val telemetry: MapFeatureTelemetry,
) {
    fun launch(
        telemetryArea: TelemetryArea,
        uiArea: String,
        action: suspend () -> Unit,
    ) {
        scope.launch {
            try {
                action()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                telemetry.record(telemetryArea, throwable)
                state.update {
                    it.copy(lastAction = MapUiMessage.ExternalError(uiArea, throwable.message))
                }
            }
        }
    }
}
