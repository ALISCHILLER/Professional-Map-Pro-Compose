package com.msa.professionalmap.feature.map.data

import android.content.Context
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.feature.map.domain.LanguagePreferenceStore

/**
 * Android-backed language preference store for the map feature.
 *
 * SharedPreferences is intentionally used here instead of DataStore to keep the starter small
 * and dependency-light. The interface lets production apps swap this implementation for
 * DataStore, encrypted preferences, or account-level settings later.
 */
class SharedPreferencesLanguagePreferenceStore(
    context: Context,
) : LanguagePreferenceStore {

    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadLanguage(): GuidanceLanguage? {
        val raw = preferences.getString(KEY_LANGUAGE, null) ?: return null
        return GuidanceLanguage.entries.firstOrNull { it.name == raw }
    }

    override fun saveLanguage(language: GuidanceLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "professional_map_feature"
        const val KEY_LANGUAGE = "language"
    }
}
