package com.msa.professionalmap.feature.map.i18n

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import java.util.Locale

internal fun MapStrings.mapStyleTitle(style: MapStyleConfig): String = mapStyleTitle(style.title)

internal fun MapStrings.mapStyleTitle(title: String): String = when (title.lowercase(Locale.US)) {
    "liberty" -> if (language == GuidanceLanguage.Persian) "لیبرتی" else "Liberty"
    "bright" -> if (language == GuidanceLanguage.Persian) "روشن" else "Bright"
    "positron" -> if (language == GuidanceLanguage.Persian) "پوزیترون" else "Positron"
    "dark" -> if (language == GuidanceLanguage.Persian) "تاریک" else "Dark"
    else -> title
}

internal fun MapStrings.routeTitle(title: String): String {
    val normalized = title.trim()
    return when {
        normalized.equals("Recommended", ignoreCase = true) ->
            if (language == GuidanceLanguage.Persian) "پیشنهادی" else "Recommended"

        normalized.equals("Active route", ignoreCase = true) ->
            if (language == GuidanceLanguage.Persian) "مسیر فعال" else "Active route"

        normalized.equals("Straight-line fallback preview", ignoreCase = true) ->
            if (language == GuidanceLanguage.Persian) "پیش‌نمایش مسیر مستقیم" else "Straight-line fallback preview"

        normalized.startsWith("Alternative ", ignoreCase = true) -> {
            val suffix = normalized.substringAfter(' ').trim()
            if (language == GuidanceLanguage.Persian) "مسیر جایگزین ${suffix.localizedDigits()}" else "Alternative $suffix"
        }

        else -> title
    }
}
