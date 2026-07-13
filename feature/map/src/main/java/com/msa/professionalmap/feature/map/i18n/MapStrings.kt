package com.msa.professionalmap.feature.map.i18n

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.GuidanceState
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.location.LocationStatus
import com.msa.professionalmap.core.offline.domain.OfflineManagerState
import com.msa.professionalmap.core.offline.domain.OfflineWorkProgress
import com.msa.professionalmap.core.offline.domain.OfflineWorkStatus
import com.msa.professionalmap.feature.map.presentation.RoutingUiState
import java.util.Locale
/**
 * Centralized bilingual copy for the map feature.
 *
 * The project intentionally keeps UI text in one place instead of scattering hard-coded
 * strings across composables. This keeps the feature easy to migrate to Android resources,
 * server-driven copy, or a full localization framework later.
 */
data class MapStrings(
    val language: GuidanceLanguage,
    val appTitle: String,
    val appSubtitle: String,
    val sectionFoundation: String,
    val sectionAppearance: String,
    val appearanceSubtitle: String,
    val appLanguage: String,
    val appLanguageSubtitle: String,
    val theme: String,
    val themeSubtitle: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val menu: String,
    val close: String,
    val mapStyle: String,
    val ready: String,
    val nextAction: String,
    val completed: String,
    val styleTolerancePrefix: String,
    val lessSimplify: String,
    val moreSimplify: String,
    val clear: String,
    val sectionLocation: String,
    val enableGps: String,
    val startGps: String,
    val stopGps: String,
    val follow: String,
    val following: String,
    val currentLocationFallback: String,
    val sectionRouting: String,
    val routeHere: String,
    val resetRoute: String,
    val steps: String,
    val sectionNavigation: String,
    val active: String,
    val idle: String,
    val startNavigation: String,
    val stopNavigation: String,
    val remainingSuffix: String,
    val minutesSuffix: String,
    val nextPrefix: String,
    val sectionGuidance: String,
    val guidanceReady: String,
    val muted: String,
    val voiceOn: String,
    val volume: String,
    val volumeDown: String,
    val volumeUp: String,
    val test: String,
    val sectionOffline: String,
    val offlineDefaultMessage: String,
    val refresh: String,
    val worker: String,
    val savedRegions: String,
    val tiles: String,
    val downloadRoute: String,
    val list: String,
    val clearCache: String,
    val packDb: String,
    val pause: String,
    val resume: String,
    val delete: String,
    val metricRoute: String,
    val metricDirect: String,
    val metricPoints: String,
    val loadingNative: String,
    val mapFailed: String,
    val noPermission: String,
    val approximate: String,
    val precise: String,
    val locationReceiving: String,
    val locationChecking: String,
    val locationReady: String,
    val locationPermissionRequired: String,
    val locationProviderDisabled: String,
    val locationStarting: String,
    val locationStopped: String,
    val routingTapDestination: String,
    val routingReady: String,
    val routingLoading: String,
    val navigationStartHint: String,
    val navigationOnRoute: String,
    val navigationRerouting: String,
    val navigationArrived: String,
    val english: String,
    val persian: String,
) {
    val isRtl: Boolean get() = language == GuidanceLanguage.Persian

    fun permissionLabel(level: LocationPermissionLevel): String = when (level) {
        LocationPermissionLevel.None -> noPermission
        LocationPermissionLevel.Approximate -> approximate
        LocationPermissionLevel.Precise -> precise
    }

    fun locationStatusLabel(status: LocationStatus): String = when (status) {
        LocationStatus.Active -> locationReceiving
        LocationStatus.Checking -> locationChecking
        LocationStatus.Idle -> locationReady
        LocationStatus.PermissionRequired -> locationPermissionRequired
        LocationStatus.ProviderDisabled -> locationProviderDisabled
        LocationStatus.Starting -> locationStarting
        LocationStatus.Stopped -> locationStopped
        is LocationStatus.Error -> status.message
    }

    fun routingLabel(state: RoutingUiState, hasSelectedPoint: Boolean): String = when (state) {
        RoutingUiState.Idle -> if (hasSelectedPoint) routingReady else routingTapDestination
        RoutingUiState.Loading -> routingLoading
        is RoutingUiState.Success -> messageText(state.message) ?: routingReady
        is RoutingUiState.Error -> messageText(state.message) ?: routingTapDestination
    }

    fun guidanceStatusText(state: GuidanceState): String =
        state.lastAnnouncement?.text ?: runtimeMessageText(state.lastErrorMessage) ?: guidanceReady

    fun offlineStatusText(state: OfflineManagerState): String =
        runtimeMessageText(state.lastErrorMessage) ?: runtimeMessageText(state.lastMessage) ?: offlineDefaultMessage

    fun offlineWorkerText(job: OfflineWorkProgress): String {
        val base = "$worker ${offlineWorkStatusLabel(job.status)}: ${job.clientId} ${percentText(job.progressPercent.toDouble() / 100.0)}"
        val error = runtimeMessageText(job.errorMessage)?.let { " · $it" }.orEmpty()
        return base + error
    }

    private fun offlineWorkStatusLabel(status: OfflineWorkStatus): String = when (language) {
        GuidanceLanguage.English -> status.name
        GuidanceLanguage.Persian -> when (status) {
            OfflineWorkStatus.Idle -> "آماده"
            OfflineWorkStatus.Enqueued -> "در صف"
            OfflineWorkStatus.Running -> "در حال اجرا"
            OfflineWorkStatus.Succeeded -> "موفق"
            OfflineWorkStatus.Failed -> "ناموفق"
            OfflineWorkStatus.Cancelled -> "لغوشده"
        }
    }

    fun guidanceLanguageLabel(target: GuidanceLanguage): String = when (target) {
        GuidanceLanguage.English -> english
        GuidanceLanguage.Persian -> persian
    }

    fun offRouteLabel(distanceMeters: Double): String = when (language) {
        GuidanceLanguage.English -> "Off route by ${distanceMeters.formatZero()} m"
        GuidanceLanguage.Persian -> "${distanceMeters.formatZero().localizedDigits()} متر خارج از مسیر"
    }

    fun progressLine(km: Double, minutes: Double, percent: String): String = when (language) {
        GuidanceLanguage.English -> "${km.formatOne()} km left · ${minutes.formatZero()} min · $percent"
        GuidanceLanguage.Persian -> "${km.formatOne().localizedDigits()} کیلومتر مانده · ${minutes.formatZero().localizedDigits()} دقیقه · $percent"
    }

    fun nextInstruction(text: String, distance: String): String = when (language) {
        GuidanceLanguage.English -> "$nextPrefix $text in $distance"
        GuidanceLanguage.Persian -> "$nextPrefix $text تا $distance دیگر"
    }

    fun volumeText(value: Float): String = when (language) {
        GuidanceLanguage.English -> "$volume ${(value * 100).toInt()}%"
        GuidanceLanguage.Persian -> "$volume ${(value * 100).toInt().toString().localizedDigits()}٪"
    }

    fun routeAlternative(title: String, distanceKm: Double): String = when (language) {
        GuidanceLanguage.English -> "${routeTitle(title)} ${distanceKm.formatOne()} km"
        GuidanceLanguage.Persian -> "${routeTitle(title)} ${distanceKm.formatOne().localizedDigits()} کیلومتر"
    }

    fun selectedRouteSummary(title: String, distanceKm: Double, durationMinutes: Double, stepCount: Int): String = when (language) {
        GuidanceLanguage.English -> "${routeTitle(title)}: ${distanceKm.formatOne()} km · ${durationMinutes.formatZero()} min · $stepCount $steps"
        GuidanceLanguage.Persian -> "${routeTitle(title)}: ${distanceKm.formatOne().localizedDigits()} کیلومتر · ${durationMinutes.formatZero().localizedDigits()} دقیقه · ${stepCount.toString().localizedDigits()} $steps"
    }

    fun tolerance(valueMeters: Int): String = when (language) {
        GuidanceLanguage.English -> "$styleTolerancePrefix ${valueMeters}m"
        GuidanceLanguage.Persian -> "$styleTolerancePrefix ${valueMeters.toString().localizedDigits()} متر"
    }

    fun routeMetricKm(value: Double): String = when (language) {
        GuidanceLanguage.English -> String.format(Locale.US, "%.2f km", value)
        GuidanceLanguage.Persian -> String.format(Locale.US, "%.2f", value).localizedDigits() + " کیلومتر"
    }

    fun distanceText(meters: Double): String = when {
        meters >= 1000.0 -> when (language) {
            GuidanceLanguage.English -> "${(meters / 1000.0).formatOne()} km"
            GuidanceLanguage.Persian -> {
                val kilometers = meters / 1000.0
                val formatted = if (kilometers % 1.0 == 0.0) kilometers.formatZero() else kilometers.formatOne()
                "${formatted.localizedDigits()} کیلومتر"
            }
        }
        else -> when (language) {
            GuidanceLanguage.English -> "${meters.formatZero()} m"
            GuidanceLanguage.Persian -> "${meters.formatZero().localizedDigits()} متر"
        }
    }

    fun percentText(fraction: Double): String {
        val value = ((fraction * 100.0).toInt().coerceIn(0, 100)).toString()
        return when (language) {
            GuidanceLanguage.English -> "$value%"
            GuidanceLanguage.Persian -> value.localizedDigits() + "٪"
        }
    }

    fun bytesText(bytes: Long): String {
        val raw = when {
            bytes >= 1024L * 1024L -> "${bytes / 1024L / 1024L} MB"
            bytes >= 1024L -> "${bytes / 1024L} KB"
            else -> "$bytes B"
        }
        return if (language == GuidanceLanguage.Persian) raw.localizedDigits() else raw
    }

    fun localizeNumberText(text: String): String = if (language == GuidanceLanguage.Persian) text.localizedDigits() else text

    companion object {
        fun forLanguage(language: GuidanceLanguage): MapStrings = MapStringCatalog.forLanguage(language)
    }
}
