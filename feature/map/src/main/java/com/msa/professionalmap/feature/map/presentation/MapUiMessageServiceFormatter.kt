package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import java.util.Locale

/**
 * Formats semantic map feedback for Android services and notifications.
 *
 * UI localization is handled by `MapStrings`, but foreground services and service-safe seams
 * need short, process-safe text that can be generated without Compose or Android resources.
 * Keeping this formatter separate prevents `MapViewModel` from accumulating copy rules while
 * still allowing the notification text to follow the active English/Persian guidance language.
 */
internal object MapUiMessageServiceFormatter {
    fun format(message: MapUiMessage, language: GuidanceLanguage = GuidanceLanguage.English): String = when (language) {
        GuidanceLanguage.English -> formatEnglish(message)
        GuidanceLanguage.Persian -> formatPersian(message)
    }

    private fun formatEnglish(message: MapUiMessage): String = when (message) {
        MapUiMessage.SelectionCleared -> "Selection cleared"
        MapUiMessage.LocationPermissionRequired -> "Location permission is required for GPS features."
        MapUiMessage.StartingGps -> "Starting GPS tracking..."
        MapUiMessage.GpsStopped -> "GPS tracking stopped."
        MapUiMessage.FollowEnabled -> "Follow mode enabled."
        MapUiMessage.FollowDisabled -> "Follow mode disabled."
        MapUiMessage.SelectDestinationFirst -> "Select a destination on the map first."
        MapUiMessage.NoOriginAvailable -> "No origin point is available."
        MapUiMessage.ReferenceRouteRestored -> "Reference route restored."
        MapUiMessage.RouteBeforeNavigation -> "Calculate a route before starting navigation."
        MapUiMessage.StartGpsBeforeNavigation -> "Start GPS before navigation."
        MapUiMessage.NavigationStarted -> "Navigation started."
        MapUiMessage.NavigationStopped -> "Navigation stopped."
        MapUiMessage.ArrivedAtDestination -> "Arrived at destination."
        MapUiMessage.SelectStyleFirst -> "Select a map style first."
        MapUiMessage.OfflineRouteBoundsUnavailable -> "Route bounds are not available for offline download."
        MapUiMessage.ProviderRouteFailedFallbackShown -> "Could not calculate provider route. Fallback route is shown."
        MapUiMessage.VoiceTestPrompt -> "Voice guidance is ready."
        MapUiMessage.VoiceTestPlayed -> "Voice guidance test played."
        MapUiMessage.VoiceMuted -> "Voice guidance muted."
        MapUiMessage.VoiceUnmuted -> "Voice guidance unmuted."
        MapUiMessage.Rerouting -> "Rerouting..."
        MapUiMessage.CalculatingRoute -> "Calculating route..."
        MapUiMessage.CheckingLocationServices -> "Checking location services..."
        MapUiMessage.ProviderDisabled -> "GPS/network location provider is disabled."
        MapUiMessage.NativeGeoEngineReady -> "Native geo engine is ready."
        is MapUiMessage.StyleChanged -> "Style changed to ${message.title}"
        is MapUiMessage.PointSelected -> "Destination selected."
        is MapUiMessage.RouteReady -> "Route ready: ${message.distanceKm.oneDecimal()} km, ${message.durationMinutes.zeroDecimal()} min."
        is MapUiMessage.RouteAlternativeSelected -> "Selected ${message.title}: ${message.distanceKm.oneDecimal()} km, ${message.durationMinutes.zeroDecimal()} min."
        is MapUiMessage.NavigationRemaining -> "Navigation: ${message.remainingKm.oneDecimal()} km remaining."
        is MapUiMessage.OffRoute -> "Off route by ${message.distanceMeters.zeroDecimal()} m."
        is MapUiMessage.GuidanceLanguageChanged -> "Voice guidance language changed."
        is MapUiMessage.GuidanceVolumeChanged -> "Voice volume: ${message.percent}%."
        is MapUiMessage.OfflineDownloadQueued -> "Offline route download queued: ${message.workId}"
        is MapUiMessage.OfflineRegionStatus -> "Offline maps: ${message.detail}"
        is MapUiMessage.OfflineWorkerStatus -> "Offline download: ${message.detail}"
        is MapUiMessage.LocationRuntimeError -> message.detail ?: "Location error"
        is MapUiMessage.LoadFailed -> message.detail ?: "Startup error"
        is MapUiMessage.ExternalError -> message.detail ?: "${message.area} error"
        is MapUiMessage.SimplificationChanged -> "Simplification tolerance: ${message.toleranceMeters} m"
    }

    private fun formatPersian(message: MapUiMessage): String = when (message) {
        MapUiMessage.SelectionCleared -> "انتخاب پاک شد"
        MapUiMessage.LocationPermissionRequired -> "مجوز موقعیت لازم است."
        MapUiMessage.StartingGps -> "در حال شروع GPS..."
        MapUiMessage.GpsStopped -> "GPS متوقف شد."
        MapUiMessage.FollowEnabled -> "دنبال کردن فعال شد."
        MapUiMessage.FollowDisabled -> "دنبال کردن غیرفعال شد."
        MapUiMessage.SelectDestinationFirst -> "ابتدا مقصد را انتخاب کنید."
        MapUiMessage.NoOriginAvailable -> "نقطه شروع در دسترس نیست."
        MapUiMessage.ReferenceRouteRestored -> "مسیر مرجع بازیابی شد."
        MapUiMessage.RouteBeforeNavigation -> "ابتدا مسیر را محاسبه کنید."
        MapUiMessage.StartGpsBeforeNavigation -> "قبل از ناوبری GPS را شروع کنید."
        MapUiMessage.NavigationStarted -> "ناوبری شروع شد."
        MapUiMessage.NavigationStopped -> "ناوبری متوقف شد."
        MapUiMessage.ArrivedAtDestination -> "به مقصد رسیدید."
        MapUiMessage.SelectStyleFirst -> "ابتدا سبک نقشه را انتخاب کنید."
        MapUiMessage.OfflineRouteBoundsUnavailable -> "محدوده آفلاین مسیر در دسترس نیست."
        MapUiMessage.ProviderRouteFailedFallbackShown -> "مسیر جایگزین نمایش داده شد."
        MapUiMessage.VoiceTestPrompt -> "راهنمای صوتی آماده است."
        MapUiMessage.VoiceTestPlayed -> "تست صدا پخش شد."
        MapUiMessage.VoiceMuted -> "راهنمای صوتی بی‌صدا شد."
        MapUiMessage.VoiceUnmuted -> "راهنمای صوتی فعال شد."
        MapUiMessage.Rerouting -> "در حال مسیر‌یابی مجدد..."
        MapUiMessage.CalculatingRoute -> "در حال محاسبه مسیر..."
        MapUiMessage.CheckingLocationServices -> "در حال بررسی سرویس موقعیت..."
        MapUiMessage.ProviderDisabled -> "GPS یا موقعیت شبکه غیرفعال است."
        MapUiMessage.NativeGeoEngineReady -> "موتور ژئو آماده است."
        is MapUiMessage.StyleChanged -> "سبک نقشه تغییر کرد: ${message.title}"
        is MapUiMessage.PointSelected -> "مقصد انتخاب شد."
        is MapUiMessage.RouteReady -> "مسیر آماده شد: ${message.distanceKm.oneDecimal().persianDigits()} کیلومتر، ${message.durationMinutes.zeroDecimal().persianDigits()} دقیقه."
        is MapUiMessage.RouteAlternativeSelected -> "${message.title} انتخاب شد: ${message.distanceKm.oneDecimal().persianDigits()} کیلومتر."
        is MapUiMessage.NavigationRemaining -> "${message.remainingKm.oneDecimal().persianDigits()} کیلومتر باقی مانده."
        is MapUiMessage.OffRoute -> "${message.distanceMeters.zeroDecimal().persianDigits()} متر خارج از مسیر."
        is MapUiMessage.GuidanceLanguageChanged -> "زبان راهنمای صوتی تغییر کرد."
        is MapUiMessage.GuidanceVolumeChanged -> "حجم صدا: ${message.percent.toString().persianDigits()}٪."
        is MapUiMessage.OfflineDownloadQueued -> "دانلود آفلاین در صف: ${message.workId.persianDigits()}"
        is MapUiMessage.OfflineRegionStatus -> "نقشه آفلاین: ${message.detail.persianDigits()}"
        is MapUiMessage.OfflineWorkerStatus -> "دانلود آفلاین: ${message.detail.persianDigits()}"
        is MapUiMessage.LocationRuntimeError -> message.detail?.persianDigits() ?: "خطای موقعیت"
        is MapUiMessage.LoadFailed -> message.detail?.persianDigits() ?: "خطای شروع برنامه"
        is MapUiMessage.ExternalError -> message.detail?.persianDigits() ?: "خطای ${message.area}"
        is MapUiMessage.SimplificationChanged -> "تلورانس ساده‌سازی: ${message.toleranceMeters.toString().persianDigits()} متر"
    }

    private fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)
    private fun Double.zeroDecimal(): String = String.format(Locale.US, "%.0f", this)

    private fun String.persianDigits(): String = map { char ->
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
