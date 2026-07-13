package com.msa.professionalmap.feature.map.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.feature.map.domain.AppLanguage
import com.msa.professionalmap.feature.map.domain.AppThemeMode
import com.msa.professionalmap.feature.map.presentation.MapViewModel

/**
 * Aggregates map-screen event callbacks so composables can stay readable and avoid
 * repeated large parameter signatures across mobile, expanded and bottom-sheet layouts.
 */
@Immutable
internal data class MapActionHandlers(
    val onStyleSelected: (MapStyleConfig) -> Unit,
    val onMapClick: (GeoPoint) -> Unit,
    val onPoiSelected: (String) -> Unit,
    val onMapLoadFailed: () -> Unit,
    val onClearSelection: () -> Unit,
    val onIncreaseSimplification: () -> Unit,
    val onDecreaseSimplification: () -> Unit,
    val onRequestLocationPermission: () -> Unit,
    val onStartLocation: () -> Unit,
    val onStopLocation: () -> Unit,
    val onToggleFollowUser: () -> Unit,
    val onCalculateRoute: () -> Unit,
    val onSelectRouteAlternative: (String) -> Unit,
    val onResetReferenceRoute: () -> Unit,
    val onStartNavigation: () -> Unit,
    val onPauseNavigation: () -> Unit,
    val onResumeNavigation: () -> Unit,
    val onStopNavigation: () -> Unit,
    val onToggleGuidanceMuted: () -> Unit,
    val onGuidanceLanguageSelected: (GuidanceLanguage) -> Unit,
    val onIncreaseGuidanceVolume: () -> Unit,
    val onDecreaseGuidanceVolume: () -> Unit,
    val onTestGuidance: () -> Unit,
    val onRefreshOffline: () -> Unit,
    val onDownloadOfflineRoute: () -> Unit,
    val onPauseOffline: (String) -> Unit,
    val onResumeOffline: (String) -> Unit,
    val onDeleteOffline: (String) -> Unit,
    val onClearAmbientCache: () -> Unit,
    val onPackOfflineDatabase: () -> Unit,
    val onAppLanguageSelected: (AppLanguage) -> Unit,
    val onThemeModeSelected: (AppThemeMode) -> Unit,
)

@Composable
internal fun rememberMapActionHandlers(
    viewModel: MapViewModel,
    onRequestLocationPermission: () -> Unit,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    onThemeModeSelected: (AppThemeMode) -> Unit,
): MapActionHandlers = remember(
    viewModel,
    onRequestLocationPermission,
    onAppLanguageSelected,
    onThemeModeSelected,
) {
    MapActionHandlers(
        onStyleSelected = viewModel::selectStyle,
        onMapClick = viewModel::onMapClicked,
        onPoiSelected = viewModel::onPoiSelected,
        onMapLoadFailed = viewModel::onMapLoadFailed,
        onClearSelection = viewModel::clearSelectedPoint,
        onIncreaseSimplification = viewModel::increaseSimplification,
        onDecreaseSimplification = viewModel::decreaseSimplification,
        onRequestLocationPermission = onRequestLocationPermission,
        onStartLocation = viewModel::startLocationTracking,
        onStopLocation = viewModel::stopLocationTracking,
        onToggleFollowUser = viewModel::toggleFollowUserLocation,
        onCalculateRoute = { viewModel.calculateRouteToSelectedPoint(useCurrentLocation = true) },
        onSelectRouteAlternative = viewModel::selectRouteAlternative,
        onResetReferenceRoute = viewModel::resetReferenceRoute,
        onStartNavigation = viewModel::startNavigation,
        onPauseNavigation = viewModel::pauseNavigation,
        onResumeNavigation = viewModel::resumeNavigation,
        onStopNavigation = viewModel::stopNavigation,
        onToggleGuidanceMuted = viewModel::toggleVoiceGuidanceMuted,
        onGuidanceLanguageSelected = viewModel::setGuidanceLanguage,
        onIncreaseGuidanceVolume = viewModel::increaseGuidanceVolume,
        onDecreaseGuidanceVolume = viewModel::decreaseGuidanceVolume,
        onTestGuidance = viewModel::testVoiceGuidance,
        onRefreshOffline = viewModel::refreshOfflineRegions,
        onDownloadOfflineRoute = viewModel::downloadCurrentRouteOffline,
        onPauseOffline = viewModel::pauseOfflineRegion,
        onResumeOffline = viewModel::resumeOfflineRegion,
        onDeleteOffline = viewModel::deleteOfflineRegion,
        onClearAmbientCache = viewModel::clearAmbientCache,
        onPackOfflineDatabase = viewModel::packOfflineDatabase,
        onAppLanguageSelected = { language ->
            viewModel.setGuidanceLanguage(language.toGuidanceLanguage())
            onAppLanguageSelected(language)
        },
        onThemeModeSelected = onThemeModeSelected,
    )
}
