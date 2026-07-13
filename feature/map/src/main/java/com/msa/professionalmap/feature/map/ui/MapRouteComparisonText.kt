package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.feature.map.i18n.MapStrings

internal fun MapStrings.routeComparisonLabel(
    route: RouteAlternative,
    fastestDurationSeconds: Double,
): String {
    val fastest = route.durationSeconds <= fastestDurationSeconds + FastestToleranceSeconds
    return when (language) {
        GuidanceLanguage.English -> buildString {
            append(routeAlternative(route.title, route.distanceKm))
            append(" · ").append(route.durationMinutes.toInt()).append(" min")
            if (fastest) append(" · Fastest")
        }

        GuidanceLanguage.Persian -> buildString {
            append(routeAlternative(route.title, route.distanceKm))
            append(" · ").append(localizeNumberText(route.durationMinutes.toInt().toString())).append(" دقیقه")
            if (fastest) append(" · سریع‌ترین")
        }
    }
}

private const val FastestToleranceSeconds = 1.0
