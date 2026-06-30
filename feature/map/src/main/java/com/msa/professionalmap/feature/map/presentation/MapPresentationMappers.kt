package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.location.LocationStatus
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress

internal fun LocationStatus.toUiMessage(): MapUiMessage? = when (this) {
    LocationStatus.Active -> null
    LocationStatus.Checking -> MapUiMessage.CheckingLocationServices
    LocationStatus.Idle -> null
    LocationStatus.PermissionRequired -> MapUiMessage.LocationPermissionRequired
    LocationStatus.ProviderDisabled -> MapUiMessage.ProviderDisabled
    LocationStatus.Starting -> MapUiMessage.StartingGps
    LocationStatus.Stopped -> MapUiMessage.GpsStopped
    is LocationStatus.Error -> MapUiMessage.LocationRuntimeError(message)
}

internal fun ProgressState.routeProgressOrNull(): RouteProgress? = when (this) {
    is ProgressState.Arrived -> progress
    is ProgressState.Navigating -> progress
    is ProgressState.OffRoute -> progress
    is ProgressState.Rerouting -> lastKnownProgress
    ProgressState.Idle -> null
}

internal fun ProgressState.toUiMessage(): MapUiMessage? = when (this) {
    ProgressState.Idle -> null
    is ProgressState.Navigating -> MapUiMessage.NavigationRemaining(progress.remainingDistanceKm)
    is ProgressState.OffRoute -> MapUiMessage.OffRoute(distanceFromRouteMeters)
    is ProgressState.Rerouting -> MapUiMessage.Rerouting
    is ProgressState.Arrived -> MapUiMessage.ArrivedAtDestination
}

internal fun ProgressState.toServiceText(language: GuidanceLanguage): String? = when (this) {
    ProgressState.Idle -> null
    is ProgressState.Navigating -> MapUiMessageServiceFormatter.format(
        MapUiMessage.NavigationRemaining(progress.remainingDistanceKm),
        language,
    )
    is ProgressState.OffRoute -> MapUiMessageServiceFormatter.format(
        MapUiMessage.OffRoute(distanceFromRouteMeters),
        language,
    )
    is ProgressState.Rerouting -> MapUiMessageServiceFormatter.format(MapUiMessage.Rerouting, language)
    is ProgressState.Arrived -> MapUiMessageServiceFormatter.format(MapUiMessage.ArrivedAtDestination, language)
}

internal fun MapUiState.serviceMessage(): String? = lastAction?.let { message ->
    MapUiMessageServiceFormatter.format(message, guidanceConfig.language)
}
