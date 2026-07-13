package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.toLanguageTag
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteMetrics
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import java.util.Locale

/**
 * Builds the compact navigation state used by the foreground service and future
 * route preview screens.
 */
class BuildNavigationSnapshotUseCase {
    operator fun invoke(
        destination: GeoPoint?,
        routePoints: List<GeoPoint>,
        metrics: RouteMetrics?,
        progressState: ProgressState,
        selectedRoute: RouteAlternative?,
        lastMessage: String?,
        instructionOverride: String? = null,
        language: GuidanceLanguage = GuidanceLanguage.English,
        status: NavigationServiceStatus = NavigationServiceStatus.Active,
        nowMillis: Long = System.currentTimeMillis(),
    ): NavigationServiceSnapshot {
        val progress = progressState.routeProgressOrNull()
        val resolvedDestination = destination ?: routePoints.lastOrNull()
        return NavigationServiceSnapshot(
            status = status,
            destinationTitle = if (language == GuidanceLanguage.Persian) "مقصد" else "Destination",
            remainingDistanceText = progress?.let { distanceText(it.remainingDistanceKm, language) }
                ?: selectedRoute?.let { distanceText(it.distanceKm, language) }
                ?: metrics?.let { distanceText(it.totalDistanceKm, language) }
                ?: "--",
            remainingDurationText = progress?.let { durationText(it.remainingMinutes, language) } ?: "--",
            nextInstructionText = instructionOverride ?: progress?.nextInstruction?.instruction ?: lastMessage,
            destination = resolvedDestination,
            languageTag = language.toLanguageTag(),
            lastUpdatedAtMillis = nowMillis,
        )
    }

    private fun ProgressState.routeProgressOrNull(): RouteProgress? = when (this) {
        is ProgressState.Arrived -> progress
        is ProgressState.Navigating -> progress
        is ProgressState.OffRoute -> progress
        is ProgressState.Rerouting -> lastKnownProgress
        ProgressState.Idle -> null
    }

    private fun distanceText(km: Double, language: GuidanceLanguage): String = when (language) {
        GuidanceLanguage.English -> "${km.formatOne()} km"
        GuidanceLanguage.Persian -> "${km.formatOne().localizedFor(language)} کیلومتر"
    }

    private fun durationText(minutes: Double, language: GuidanceLanguage): String = when (language) {
        GuidanceLanguage.English -> "${minutes.formatZero()} min"
        GuidanceLanguage.Persian -> "${minutes.formatZero().localizedFor(language)} دقیقه"
    }

    private fun Double.formatOne(): String = String.format(Locale.US, "%.1f", this)
    private fun Double.formatZero(): String = String.format(Locale.US, "%.0f", this)

    private fun String.localizedFor(language: GuidanceLanguage): String {
        if (language != GuidanceLanguage.Persian) return this
        return map { char ->
            when (char) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> char
            }
        }.joinToString("")
    }
}
