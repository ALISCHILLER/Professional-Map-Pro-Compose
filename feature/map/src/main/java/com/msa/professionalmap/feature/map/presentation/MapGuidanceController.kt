package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.TtsEngine
import com.msa.professionalmap.core.guidance.domain.VoiceGuidanceRepository
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.feature.map.domain.LanguagePreferenceStore
import com.msa.professionalmap.feature.map.domain.MapFeatureTelemetry
import com.msa.professionalmap.feature.map.domain.TelemetryArea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns voice-guidance presentation behavior for a single map screen instance.
 *
 * Keeping language, volume, mute, TTS test and progress-announcement handling
 * outside MapViewModel keeps the ViewModel focused on user intent orchestration.
 */
internal class MapGuidanceController(
    private val state: MutableStateFlow<MapUiState>,
    private val scope: CoroutineScope,
    private val guidanceRepository: VoiceGuidanceRepository,
    private val ttsEngine: TtsEngine,
    private val languagePreferenceStore: LanguagePreferenceStore,
    private val telemetry: MapFeatureTelemetry,
) {
    fun restoreLanguagePreference() {
        val stored = languagePreferenceStore.loadLanguage() ?: return
        state.update { current ->
            val nextConfig = current.guidanceConfig.copy(language = stored)
            current.copy(
                guidanceConfig = nextConfig,
                guidanceState = guidanceRepository.reset(nextConfig),
            )
        }
    }

    fun resetForCurrentConfig() {
        state.update { current ->
            current.copy(guidanceState = guidanceRepository.reset(current.guidanceConfig))
        }
    }

    fun toggleMuted() {
        state.update { current ->
            val nextConfig = current.guidanceConfig.copy(muted = !current.guidanceConfig.muted)
            current.copy(
                guidanceConfig = nextConfig,
                guidanceState = current.guidanceState.copy(
                    enabled = nextConfig.enabled,
                    muted = nextConfig.muted,
                    language = nextConfig.language,
                    volume = nextConfig.volume,
                ),
                lastAction = if (nextConfig.muted) MapUiMessage.VoiceMuted else MapUiMessage.VoiceUnmuted,
            )
        }
    }

    fun setLanguage(language: GuidanceLanguage) {
        languagePreferenceStore.saveLanguage(language)
        state.update { current ->
            val nextConfig = current.guidanceConfig.copy(language = language)
            current.copy(
                guidanceConfig = nextConfig,
                guidanceState = guidanceRepository.reset(nextConfig),
                lastAction = MapUiMessage.GuidanceLanguageChanged(language),
            )
        }
    }

    fun increaseVolume() {
        updateVolume((state.value.guidanceConfig.volume + VOLUME_STEP).coerceAtMost(1.0f))
    }

    fun decreaseVolume() {
        updateVolume((state.value.guidanceConfig.volume - VOLUME_STEP).coerceAtLeast(0.0f))
    }

    fun testVoiceGuidance() {
        val config = state.value.guidanceConfig
        val text = MapUiMessageServiceFormatter.format(MapUiMessage.VoiceTestPrompt, config.language)
        scope.launch {
            ttsEngine.speak(text, config.language, config.volume)
                .onSuccess {
                    state.update { it.copy(lastAction = MapUiMessage.VoiceTestPlayed) }
                }
                .onFailure { throwable ->
                    telemetry.voiceGuidanceError(TelemetryArea.VoiceGuidanceTest, throwable)
                    state.update {
                        it.copy(
                            guidanceState = it.guidanceState.copy(lastErrorMessage = throwable.message ?: TTS_ERROR_MESSAGE),
                            lastAction = MapUiMessage.ExternalError("voice_guidance_test", throwable.message),
                        )
                    }
                }
        }
    }

    fun handleProgress(
        progressState: ProgressState,
        speedMetersPerSecond: Double?,
        timestampMillis: Long,
    ) {
        val config = state.value.guidanceConfig
        val evaluation = guidanceRepository.evaluate(
            progressState = progressState,
            speedMetersPerSecond = speedMetersPerSecond,
            config = config,
            nowMillis = timestampMillis,
        )
        state.update { it.copy(guidanceState = evaluation.state) }
        val event = evaluation.eventToSpeak ?: return
        scope.launch {
            ttsEngine.speak(event.text, event.language, config.volume)
                .onFailure { throwable ->
                    telemetry.voiceGuidanceError(TelemetryArea.VoiceGuidance, throwable)
                    state.update { current ->
                        current.copy(
                            guidanceState = current.guidanceState.copy(lastErrorMessage = throwable.message ?: TTS_ERROR_MESSAGE),
                            lastAction = MapUiMessage.ExternalError("voice_guidance", throwable.message),
                        )
                    }
                }
        }
    }

    fun close() {
        ttsEngine.close()
    }

    private fun updateVolume(volume: Float) {
        state.update { current ->
            val nextConfig = current.guidanceConfig.copy(volume = volume.coerceIn(0.0f, 1.0f))
            current.copy(
                guidanceConfig = nextConfig,
                guidanceState = current.guidanceState.copy(
                    enabled = nextConfig.enabled,
                    muted = nextConfig.muted,
                    language = nextConfig.language,
                    volume = nextConfig.volume,
                ),
                lastAction = MapUiMessage.GuidanceVolumeChanged((nextConfig.volume * 100).toInt()),
            )
        }
    }

    private companion object {
        const val VOLUME_STEP = 0.1f
        const val TTS_ERROR_MESSAGE = "TTS error"
    }
}
