package com.msa.professionalmap.feature.map

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.feature.map.presentation.MapUiMessage
import com.msa.professionalmap.feature.map.presentation.MapUiMessageServiceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class MapUiMessageServiceFormatterTest {
    @Test
    fun formatsNavigationMessagesForForegroundService() {
        assertEquals("Navigation started.", MapUiMessageServiceFormatter.format(MapUiMessage.NavigationStarted))
        assertEquals("Voice guidance is ready.", MapUiMessageServiceFormatter.format(MapUiMessage.VoiceTestPrompt))
        assertEquals("Voice volume: 80%.", MapUiMessageServiceFormatter.format(MapUiMessage.GuidanceVolumeChanged(80)))
    }

    @Test
    fun formatsPersianNavigationMessagesForForegroundService() {
        assertEquals(
            "ناوبری شروع شد.",
            MapUiMessageServiceFormatter.format(MapUiMessage.NavigationStarted, GuidanceLanguage.Persian),
        )
        assertEquals(
            "راهنمای صوتی آماده است.",
            MapUiMessageServiceFormatter.format(MapUiMessage.VoiceTestPrompt, GuidanceLanguage.Persian),
        )
        assertEquals(
            "حجم صدا: ۸۰٪.",
            MapUiMessageServiceFormatter.format(MapUiMessage.GuidanceVolumeChanged(80), GuidanceLanguage.Persian),
        )
    }

    @Test
    fun formatsExternalErrorsWithoutUiLocalizationDependency() {
        assertEquals(
            "network timeout",
            MapUiMessageServiceFormatter.format(MapUiMessage.ExternalError("routing", "network timeout")),
        )
    }
}
