package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.feature.map.i18n.MapStrings
import com.msa.professionalmap.feature.map.i18n.formatCoordinate
import com.msa.professionalmap.feature.map.presentation.MapUiState

internal fun MapUiState.currentLocationText(strings: MapStrings): String = currentLocation?.let { location ->
    strings.localizeNumberText("${location.position.latitude.formatCoordinate()}, ${location.position.longitude.formatCoordinate()}") +
        location.accuracyMeters?.let { " · ±${it.toInt()}m" }.orEmpty()
} ?: strings.currentLocationFallback

internal fun MapUiState.routeDestinationText(strings: MapStrings): String = selectedPoint?.let { point ->
    strings.routeHere + ": " + strings.localizeNumberText(
        "${point.latitude.formatCoordinate()}, ${point.longitude.formatCoordinate()}"
    )
} ?: strings.routingTapDestination

internal fun MapUiState.navigationHint(strings: MapStrings): String = when {
    navigationActive -> strings.navigationOnRoute
    currentLocation == null -> strings.locationPermissionRequired
    selectedRouteAlternative == null && routePoints.size < 2 -> strings.routingTapDestination
    else -> strings.navigationStartHint
}
