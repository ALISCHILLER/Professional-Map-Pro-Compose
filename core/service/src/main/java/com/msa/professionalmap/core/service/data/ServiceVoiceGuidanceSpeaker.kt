package com.msa.professionalmap.core.service.data

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.msa.professionalmap.core.guidance.data.DefaultVoiceGuidanceRepository
import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.TtsEngine
import com.msa.professionalmap.core.progress.domain.ProgressState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

/**
 * Owns navigation announcements inside the foreground service so spoken guidance keeps working
 * when the Activity or map ViewModel is recreated or no longer visible.
 */
internal class ServiceVoiceGuidanceSpeaker(
    context: Context,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private val repository = DefaultVoiceGuidanceRepository()
    private val ttsEngine: TtsEngine = ServiceTtsEngine(context)
    private var config: GuidanceConfig = GuidanceConfig()
    private var speakingJob: Job? = null

    fun updateConfig(next: GuidanceConfig) {
        config = next
        repository.reset(next)
        if (!next.enabled || next.muted) {
            speakingJob?.cancel()
            speakingJob = null
            ttsEngine.stop()
        }
    }

    fun stopSpeaking() {
        speakingJob?.cancel()
        speakingJob = null
        ttsEngine.stop()
    }

    fun onProgress(
        progressState: ProgressState,
        speedMetersPerSecond: Double?,
        timestampMillis: Long,
    ) {
        val current = config
        if (!current.enabled || current.muted) return
        val event = repository.evaluate(
            progressState = progressState,
            speedMetersPerSecond = speedMetersPerSecond,
            config = current,
            nowMillis = timestampMillis,
        ).eventToSpeak ?: return

        speakingJob?.cancel()
        speakingJob = scope.launch {
            ttsEngine.speak(event.text, event.language, current.volume)
        }
    }

    override fun close() {
        speakingJob?.cancel()
        speakingJob = null
        ttsEngine.close()
    }
}

/** Android TTS adapter scoped to the foreground navigation service. */
private class ServiceTtsEngine(context: Context) : TtsEngine {
    private val initResult = CompletableDeferred<Int>()
    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (!initResult.isCompleted) initResult.complete(status)
    }

    override val isSpeaking: Boolean
        get() = tts.isSpeaking

    override suspend fun speak(
        text: String,
        language: GuidanceLanguage,
        volume: Float,
    ): Result<Unit> {
        if (text.isBlank()) return Result.success(Unit)
        val initStatus = withTimeoutOrNull(TtsInitTimeoutMillis) { initResult.await() }
            ?: return Result.failure(IllegalStateException("TTS initialization timeout"))
        if (initStatus != TextToSpeech.SUCCESS) {
            return Result.failure(IllegalStateException("TTS initialization failed"))
        }

        return runCatching {
            val requestedLocale = Locale.forLanguageTag(language.bcp47Tag)
            val languageResult = tts.setLanguage(requestedLocale)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts.setLanguage(Locale.US)
            }
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            }
            val result = tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                params,
                "navigation-${UUID.randomUUID()}",
            )
            check(result != TextToSpeech.ERROR) { "TTS rejected announcement" }
        }
    }

    override fun stop() {
        tts.stop()
    }

    override fun close() {
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val TtsInitTimeoutMillis = 3_000L
    }
}
