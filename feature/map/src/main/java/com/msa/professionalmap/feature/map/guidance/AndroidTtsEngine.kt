package com.msa.professionalmap.feature.map.guidance

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.TtsEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

/**
 * Android-specific TTS adapter. The guidance decision engine remains pure Kotlin in core:guidance;
 * this class is only an infrastructure detail of the Android feature layer.
 */
class AndroidTtsEngine(context: Context) : TtsEngine {
    private val appContext = context.applicationContext
    private val initResult = CompletableDeferred<Int>()
    private val tts = TextToSpeech(appContext) { status -> initResult.complete(status) }

    override val isSpeaking: Boolean
        get() = tts.isSpeaking

    override suspend fun speak(
        text: String,
        language: GuidanceLanguage,
        volume: Float,
    ): Result<Unit> {
        if (text.isBlank()) return Result.success(Unit)
        val status = withTimeoutOrNull(TTS_INIT_TIMEOUT_MILLIS) { initResult.await() }
            ?: return Result.failure(IllegalStateException("Text-to-Speech initialization timed out."))
        if (status != TextToSpeech.SUCCESS) {
            return Result.failure(IllegalStateException("Text-to-Speech initialization failed."))
        }

        return runCatching {
            val locale = Locale.forLanguageTag(language.bcp47Tag)
            val languageResult = tts.setLanguage(locale)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US)
            }

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            }
            val result = tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                params,
                "guidance-${UUID.randomUUID()}",
            )
            if (result == TextToSpeech.ERROR) {
                throw IllegalStateException("Text-to-Speech rejected the announcement.")
            }
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
        const val TTS_INIT_TIMEOUT_MILLIS = 3_000L
    }
}
