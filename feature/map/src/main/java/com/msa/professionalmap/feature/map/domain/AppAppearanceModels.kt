package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage

/**
 * Languages exposed by the in-app language picker.
 *
 * UI language is intentionally separated from voice guidance, while conversion helpers make it
 * possible to keep both in sync when the user changes the application language.
 */
enum class AppLanguage(
    val languageTag: String,
) {
    English("en"),
    Persian("fa");

    fun toGuidanceLanguage(): GuidanceLanguage = when (this) {
        English -> GuidanceLanguage.English
        Persian -> GuidanceLanguage.Persian
    }

    companion object {
        fun fromLanguageTag(languageTag: String?): AppLanguage = when {
            languageTag.equals("fa", ignoreCase = true) ||
                languageTag?.startsWith("fa-", ignoreCase = true) == true -> Persian

            else -> English
        }
    }
}

/** Theme choice persisted for the whole Android application. */
enum class AppThemeMode {
    System,
    Light,
    Dark;

    companion object {
        fun fromStorageValue(value: String?): AppThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: System
    }
}
