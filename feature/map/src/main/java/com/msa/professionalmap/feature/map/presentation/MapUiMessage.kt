package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.model.GeoPoint

/**
 * Typed, localization-friendly feedback emitted by the map feature.
 *
 * Earlier revisions stored transient user feedback as raw English strings. That made the
 * ViewModel harder to test, coupled UI behavior to copy text, and forced Persian support to
 * rely on fragile string matching. `MapUiMessage` keeps business events semantic while letting
 * the presentation/localization layer decide how each message should be rendered.
 */
sealed interface MapUiMessage {
    data object SelectionCleared : MapUiMessage
    data object LocationPermissionRequired : MapUiMessage
    data object StartingGps : MapUiMessage
    data object GpsStopped : MapUiMessage
    data object FollowEnabled : MapUiMessage
    data object FollowDisabled : MapUiMessage
    data object SelectDestinationFirst : MapUiMessage
    data object NoOriginAvailable : MapUiMessage
    data object ReferenceRouteRestored : MapUiMessage
    data object RouteBeforeNavigation : MapUiMessage
    data object StartGpsBeforeNavigation : MapUiMessage
    data object NavigationStarted : MapUiMessage
    data object NavigationStopped : MapUiMessage
    data object NavigationPaused : MapUiMessage
    data object NavigationUnavailable : MapUiMessage
    data object ArrivedAtDestination : MapUiMessage
    data object SelectStyleFirst : MapUiMessage
    data object OfflineRouteBoundsUnavailable : MapUiMessage
    data object ProviderRouteFailedFallbackShown : MapUiMessage
    data object VoiceTestPrompt : MapUiMessage
    data object VoiceTestPlayed : MapUiMessage
    data object VoiceMuted : MapUiMessage
    data object VoiceUnmuted : MapUiMessage
    data object Rerouting : MapUiMessage
    data object CalculatingRoute : MapUiMessage
    data object CheckingLocationServices : MapUiMessage
    data object ProviderDisabled : MapUiMessage
    data object NativeGeoEngineReady : MapUiMessage

    data class StyleChanged(val title: String) : MapUiMessage
    data class PointSelected(val point: GeoPoint, val distanceFromRouteStartKm: Double?) : MapUiMessage
    data class PoiSelected(val title: String) : MapUiMessage
    data class RouteReady(val provider: String, val distanceKm: Double, val durationMinutes: Double) : MapUiMessage
    data class RouteAlternativeSelected(val title: String, val distanceKm: Double, val durationMinutes: Double) : MapUiMessage
    data class NavigationRemaining(val remainingKm: Double) : MapUiMessage
    data class OffRoute(val distanceMeters: Double) : MapUiMessage
    data class GuidanceLanguageChanged(val language: GuidanceLanguage) : MapUiMessage
    data class GuidanceVolumeChanged(val percent: Int) : MapUiMessage
    data class OfflineDownloadQueued(val workId: String) : MapUiMessage
    data class OfflineRegionStatus(val detail: String) : MapUiMessage
    data class OfflineWorkerStatus(val detail: String) : MapUiMessage
    data class LocationRuntimeError(val detail: String?) : MapUiMessage
    data class LoadFailed(val detail: String?) : MapUiMessage
    data class ExternalError(val area: String, val detail: String?) : MapUiMessage
    data class SimplificationChanged(val toleranceMeters: Int) : MapUiMessage
}
