package com.msa.professionalmap.feature.map.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.msa.professionalmap.feature.map.di.MapFeatureDependencies
import com.msa.professionalmap.feature.map.domain.MapFeatureTelemetry
import com.msa.professionalmap.feature.map.domain.MapViewModelRuntime

/**
 * Production ViewModel factory for the map feature.
 *
 * The factory receives a fully resolved dependency graph instead of constructing fallback
 * implementations by itself. This keeps the presentation layer honest: tests, previews and
 * the application composition root must explicitly choose their dependencies.
 */
class MapViewModelFactory(
    private val dependencies: MapFeatureDependencies,
    private val runtime: MapViewModelRuntime = MapViewModelRuntime.create(
        routingRepository = dependencies.routingRepository,
        progressRepository = dependencies.progressRepository,
        geoEngine = dependencies.geoEngine,
        mapCatalogRepository = dependencies.repository,
        featureConfig = dependencies.config,
    ),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MapViewModel::class.java)) {
            "MapViewModelFactory can only create MapViewModel."
        }
        return MapViewModel(
            locationRepository = dependencies.locationRepository,
            routingRepository = dependencies.routingRepository,
            offlineRepository = dependencies.offlineRepository,
            offlineDownloadManager = dependencies.offlineDownloadManager,
            guidanceRepository = dependencies.guidanceRepository,
            ttsEngine = dependencies.ttsEngine,
            navigationServiceController = dependencies.navigationServiceController,
            telemetry = MapFeatureTelemetry(dependencies.appMonitor),
            languagePreferenceStore = dependencies.languagePreferenceStore,
            runtime = runtime,
        ) as T
    }
}
