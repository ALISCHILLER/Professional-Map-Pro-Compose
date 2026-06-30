package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage

/**
 * Small persistence boundary for the map feature language.
 *
 * The UI uses one language for visible copy and voice guidance. Keeping this behind an
 * interface preserves Dependency Inversion: the ViewModel depends on an abstraction while the
 * Android-specific implementation can use SharedPreferences, DataStore, or another storage
 * mechanism without changing presentation logic.
 */
interface LanguagePreferenceStore {
    fun loadLanguage(): GuidanceLanguage?
    fun saveLanguage(language: GuidanceLanguage)
}
