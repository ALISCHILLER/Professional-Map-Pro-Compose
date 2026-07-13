package com.msa.professionalmap.core.guidance.domain

/**
 * Stable BCP-47 tags for language-aware system surfaces such as notifications,
 * foreground services, offline worker progress and service handoff state.
 */
fun GuidanceLanguage.toLanguageTag(): String = when (this) {
    GuidanceLanguage.English -> "en"
    GuidanceLanguage.Persian -> "fa"
}
