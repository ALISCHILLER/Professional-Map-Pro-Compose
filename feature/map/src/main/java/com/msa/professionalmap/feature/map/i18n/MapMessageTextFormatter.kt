package com.msa.professionalmap.feature.map.i18n

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.feature.map.presentation.MapUiMessage
import java.util.Locale

/**
 * Translates typed map UI messages to localized, user-facing text.
 *
 * Keeping message rendering outside MapStrings keeps the string catalog as data
 * and makes feedback-copy behavior independently testable.
 */
internal fun MapStrings.messageText(message: MapUiMessage?): String? = when (message) {
    null -> null
    MapUiMessage.SelectionCleared -> if (language == GuidanceLanguage.English) "Selection cleared" else "انتخاب پاک شد"
    MapUiMessage.LocationPermissionRequired -> if (language == GuidanceLanguage.English) "Location permission is required for GPS features." else "برای امکانات GPS مجوز موقعیت لازم است."
    MapUiMessage.StartingGps -> if (language == GuidanceLanguage.English) "Starting GPS tracking..." else "در حال شروع ردیابی GPS..."
    MapUiMessage.GpsStopped -> if (language == GuidanceLanguage.English) "GPS tracking stopped." else "ردیابی GPS متوقف شد."
    MapUiMessage.FollowEnabled -> if (language == GuidanceLanguage.English) "Follow mode enabled." else "حالت دنبال کردن فعال شد."
    MapUiMessage.FollowDisabled -> if (language == GuidanceLanguage.English) "Follow mode disabled." else "حالت دنبال کردن غیرفعال شد."
    MapUiMessage.SelectDestinationFirst -> if (language == GuidanceLanguage.English) "Select a destination on the map first." else "ابتدا مقصد را روی نقشه انتخاب کنید."
    MapUiMessage.NoOriginAvailable -> if (language == GuidanceLanguage.English) "No origin point is available." else "نقطه شروع در دسترس نیست."
    MapUiMessage.ReferenceRouteRestored -> if (language == GuidanceLanguage.English) "Reference route restored." else "مسیر مرجع بازیابی شد."
    MapUiMessage.RouteBeforeNavigation -> if (language == GuidanceLanguage.English) "Calculate a route before starting navigation." else "قبل از شروع ناوبری، مسیر را محاسبه کنید."
    MapUiMessage.StartGpsBeforeNavigation -> if (language == GuidanceLanguage.English) "Start GPS before navigation." else "قبل از ناوبری GPS را شروع کنید."
    MapUiMessage.NavigationStarted -> if (language == GuidanceLanguage.English) "Navigation started." else "ناوبری شروع شد."
    MapUiMessage.NavigationStopped -> if (language == GuidanceLanguage.English) "Navigation stopped." else "ناوبری متوقف شد."
    MapUiMessage.ArrivedAtDestination -> if (language == GuidanceLanguage.English) "Arrived at destination." else "به مقصد رسیدید."
    MapUiMessage.SelectStyleFirst -> if (language == GuidanceLanguage.English) "Select a map style first." else "ابتدا یک سبک نقشه انتخاب کنید."
    MapUiMessage.OfflineRouteBoundsUnavailable -> if (language == GuidanceLanguage.English) "Route bounds are not available for offline download." else "محدوده مسیر برای دانلود آفلاین در دسترس نیست."
    MapUiMessage.ProviderRouteFailedFallbackShown -> if (language == GuidanceLanguage.English) "Could not calculate provider route. Fallback route is shown." else "محاسبه مسیر از سرویس‌دهنده انجام نشد؛ مسیر جایگزین نمایش داده شد."
    MapUiMessage.VoiceTestPrompt -> if (language == GuidanceLanguage.English) "Voice guidance is ready." else "راهنمای صوتی آماده است."
    MapUiMessage.VoiceTestPlayed -> if (language == GuidanceLanguage.English) "Voice guidance test played." else "تست راهنمای صوتی پخش شد."
    MapUiMessage.VoiceMuted -> if (language == GuidanceLanguage.English) "Voice guidance muted." else "راهنمای صوتی بی‌صدا شد."
    MapUiMessage.VoiceUnmuted -> if (language == GuidanceLanguage.English) "Voice guidance unmuted." else "راهنمای صوتی دوباره فعال شد."
    MapUiMessage.Rerouting -> if (language == GuidanceLanguage.English) "Rerouting..." else "در حال مسیر‌یابی مجدد..."
    MapUiMessage.CalculatingRoute -> if (language == GuidanceLanguage.English) "Calculating route..." else "در حال محاسبه مسیر..."
    MapUiMessage.CheckingLocationServices -> if (language == GuidanceLanguage.English) "Checking location services..." else "در حال بررسی سرویس‌های موقعیت..."
    MapUiMessage.ProviderDisabled -> if (language == GuidanceLanguage.English) "GPS/network location provider is disabled." else "GPS یا سرویس موقعیت شبکه غیرفعال است."
    MapUiMessage.NativeGeoEngineReady -> if (language == GuidanceLanguage.English) "Native geo engine is ready." else "موتور بومی ژئو آماده است."
    is MapUiMessage.StyleChanged -> if (language == GuidanceLanguage.English) "Style changed to ${message.title}" else "سبک نقشه تغییر کرد: ${message.title}"
    is MapUiMessage.PointSelected -> when {
        message.distanceFromRouteStartKm == null -> if (language == GuidanceLanguage.English) {
            "Selected ${String.format(Locale.US, "%.5f", message.point.latitude)}, ${String.format(Locale.US, "%.5f", message.point.longitude)}"
        } else {
            "نقطه انتخاب شد: ${String.format(Locale.US, "%.5f", message.point.latitude).localizedDigits()}، ${String.format(Locale.US, "%.5f", message.point.longitude).localizedDigits()}"
        }
        language == GuidanceLanguage.English -> "Selected point is ${message.distanceFromRouteStartKm.formatOne()} km from route start. Tap Route here."
        else -> "نقطه انتخاب‌شده ${message.distanceFromRouteStartKm.formatOne().localizedDigits()} کیلومتر از شروع مسیر فاصله دارد. روی «مسیر تا اینجا» بزنید."
    }
    is MapUiMessage.RouteReady -> if (language == GuidanceLanguage.English) {
        "Route ready via ${message.provider}: ${message.distanceKm.formatOne()} km, ${message.durationMinutes.formatZero()} min."
    } else {
        "مسیر با ${message.provider} آماده شد: ${message.distanceKm.formatOne().localizedDigits()} کیلومتر، ${message.durationMinutes.formatZero().localizedDigits()} دقیقه."
    }
    is MapUiMessage.RouteAlternativeSelected -> if (language == GuidanceLanguage.English) {
        "Selected ${message.title}: ${message.distanceKm.formatOne()} km, ${message.durationMinutes.formatZero()} min."
    } else {
        "${message.title} انتخاب شد: ${message.distanceKm.formatOne().localizedDigits()} کیلومتر، ${message.durationMinutes.formatZero().localizedDigits()} دقیقه."
    }
    is MapUiMessage.NavigationRemaining -> if (language == GuidanceLanguage.English) {
        "Navigation: ${message.remainingKm.formatOne()} km remaining."
    } else {
        "ناوبری: ${message.remainingKm.formatOne().localizedDigits()} کیلومتر باقی مانده."
    }
    is MapUiMessage.OffRoute -> offRouteLabel(message.distanceMeters) + if (language == GuidanceLanguage.English) ". Rerouting if it continues." else "؛ اگر ادامه پیدا کند مسیر دوباره محاسبه می‌شود."
    is MapUiMessage.GuidanceLanguageChanged -> if (language == GuidanceLanguage.English) {
        "Voice guidance language: ${guidanceLanguageLabel(message.language)}."
    } else {
        "زبان راهنمای صوتی: ${guidanceLanguageLabel(message.language)}."
    }
    is MapUiMessage.GuidanceVolumeChanged -> if (language == GuidanceLanguage.English) {
        "Voice volume: ${message.percent}%."
    } else {
        "حجم صدا: ${message.percent.toString().localizedDigits()}٪."
    }
    is MapUiMessage.OfflineDownloadQueued -> if (language == GuidanceLanguage.English) {
        "Offline route download queued: ${message.workId}"
    } else {
        "دانلود آفلاین مسیر در صف قرار گرفت: ${message.workId.localizedDigits()}"
    }
    is MapUiMessage.OfflineRegionStatus -> if (language == GuidanceLanguage.English) {
        "Offline maps: ${message.detail}"
    } else {
        "نقشه آفلاین: ${message.detail.localizedDigits()}"
    }
    is MapUiMessage.OfflineWorkerStatus -> if (language == GuidanceLanguage.English) {
        "Offline download: ${message.detail}"
    } else {
        "دانلود آفلاین: ${message.detail.localizedDigits()}"
    }
    is MapUiMessage.LocationRuntimeError -> externalErrorText("location", message.detail)
    is MapUiMessage.LoadFailed -> externalErrorText("startup", message.detail)
    is MapUiMessage.SimplificationChanged -> if (language == GuidanceLanguage.English) {
        "Simplification tolerance: ${message.toleranceMeters} m"
    } else {
        "تلورانس ساده‌سازی: ${message.toleranceMeters.toString().localizedDigits()} متر"
    }
    is MapUiMessage.ExternalError -> externalErrorText(message.area, message.detail)
}

private fun MapStrings.externalErrorText(area: String, detail: String?): String {
    val cleanArea = area.replace('_', ' ').trim().ifBlank { "runtime" }
    return when (language) {
        GuidanceLanguage.English -> detail?.let { "$cleanArea error: $it" } ?: "$cleanArea error."
        GuidanceLanguage.Persian -> detail?.let { "خطای $cleanArea: ${it.localizedDigits()}" } ?: "خطای $cleanArea."
    }
}

internal fun MapStrings.runtimeMessageText(message: String?): String? {
    if (message == null || language == GuidanceLanguage.English) return message
    val exact = when (message) {
        "Selection cleared" -> "انتخاب پاک شد"
        "Location permission is required for GPS features." -> "برای امکانات GPS مجوز موقعیت لازم است."
        "Starting GPS tracking..." -> "در حال شروع ردیابی GPS..."
        "Select a destination on the map first." -> "ابتدا مقصد را روی نقشه انتخاب کنید."
        "No origin point is available." -> "نقطه شروع در دسترس نیست."
        "Reference route restored." -> "مسیر مرجع بازیابی شد."
        "Calculate a route before starting navigation." -> "قبل از شروع ناوبری، مسیر را محاسبه کنید."
        "Start GPS before navigation." -> "قبل از ناوبری GPS را شروع کنید."
        "Navigation started." -> "ناوبری شروع شد."
        "Voice guidance test played." -> "تست راهنمای صوتی پخش شد."
        "Voice guidance muted." -> "راهنمای صوتی بی‌صدا شد."
        "Voice guidance unmuted." -> "راهنمای صوتی دوباره فعال شد."
        "Navigation stopped." -> "ناوبری متوقف شد."
        "Select a map style first." -> "ابتدا یک سبک نقشه انتخاب کنید."
        "Route bounds are not available for offline download." -> "محدوده مسیر برای دانلود آفلاین در دسترس نیست."
        "Could not calculate provider route. Fallback route is shown." -> "محاسبه مسیر از سرویس‌دهنده انجام نشد؛ مسیر جایگزین نمایش داده شد."
        "Arrived at destination." -> "به مقصد رسیدید."
        "TTS error" -> "خطای تبدیل متن به گفتار"
        else -> null
    }
    if (exact != null) return exact
    return when {
        message.startsWith("Style changed to ") -> "سبک نقشه تغییر کرد: " + message.removePrefix("Style changed to ")
        message.startsWith("Selected point is ") -> message
            .replace("Selected point is ", "نقطه انتخاب‌شده ")
            .replace(" km from route start. Tap Route here.", " کیلومتر با شروع مسیر فاصله دارد. «مسیر تا اینجا» را بزنید.")
            .localizedDigits()
        message.startsWith("Selected ") -> message.replace("Selected ", "انتخاب شد: ").localizedDigits()
        message.startsWith("Voice guidance language:") -> "زبان راهنمای صوتی تغییر کرد."
        message.startsWith("Offline route download queued:") -> "دانلود آفلاین مسیر در صف قرار گرفت: " + message.substringAfter(":").trim()
        message.startsWith("Voice volume:") -> message.replace("Voice volume:", "حجم صدا:").localizedDigits()
        message.startsWith("Native geo engine ready.") -> "موتور بومی ژئو آماده است."
        message.startsWith("Simplification tolerance:") -> message.replace("Simplification tolerance:", "تلورانس ساده‌سازی:").localizedDigits()
        else -> message.localizedDigits()
    }
}
