package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class AppAppearanceModelsTest {
    @Test
    fun languageTagsResolveToSupportedApplicationLanguages() {
        assertEquals(AppLanguage.Persian, AppLanguage.fromLanguageTag("fa-IR"))
        assertEquals(AppLanguage.English, AppLanguage.fromLanguageTag("en-US"))
        assertEquals(AppLanguage.English, AppLanguage.fromLanguageTag("de-DE"))
    }

    @Test
    fun applicationLanguageMapsToMatchingVoiceGuidanceLanguage() {
        assertEquals(GuidanceLanguage.English, AppLanguage.English.toGuidanceLanguage())
        assertEquals(GuidanceLanguage.Persian, AppLanguage.Persian.toGuidanceLanguage())
    }

    @Test
    fun unknownStoredThemeFallsBackToSystem() {
        assertEquals(AppThemeMode.Dark, AppThemeMode.fromStorageValue("dark"))
        assertEquals(AppThemeMode.System, AppThemeMode.fromStorageValue("unsupported"))
        assertEquals(AppThemeMode.System, AppThemeMode.fromStorageValue(null))
    }
}
