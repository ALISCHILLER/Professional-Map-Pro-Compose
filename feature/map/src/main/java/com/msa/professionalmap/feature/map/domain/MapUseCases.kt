package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.GeoEngine
import com.msa.professionalmap.core.mapdata.MapCatalogRepository
import com.msa.professionalmap.core.offline.domain.OfflineRegionPlanner
import com.msa.professionalmap.core.routing.RoutingRepository

/**
 * Small dependency bundle for feature-level application use cases.
 *
 * This is intentionally not a service locator: it is built at composition/root
 * creation time and injected into the ViewModel, keeping dependencies explicit.
 */
data class MapUseCases(
    val calculateRoute: CalculateRouteUseCase,
    val applyRouteCalculationOutcome: ApplyRouteCalculationOutcomeUseCase,
    val resolveRouteRequest: ResolveRouteRequestUseCase,
    val startNavigation: StartNavigationUseCase,
    val prepareOfflineRoutePack: PrepareOfflineRoutePackUseCase,
    val loadInitialMapScene: LoadInitialMapSceneUseCase,
    val buildRoutePresentation: BuildRoutePresentationUseCase,
    val buildNavigationSnapshot: BuildNavigationSnapshotUseCase = BuildNavigationSnapshotUseCase(),
) {
    companion object {
        fun create(
            routingRepository: RoutingRepository,
            geoEngine: GeoEngine,
            mapCatalogRepository: MapCatalogRepository,
            activeRouteResolver: ActiveRouteResolver,
            routeOriginResolver: RouteOriginResolver,
            offlineRegionPlanner: OfflineRegionPlanner = OfflineRegionPlanner(),
            config: MapFeatureConfig = MapFeatureConfig(),
        ): MapUseCases {
            val buildRoutePresentation = BuildRoutePresentationUseCase(geoEngine)
            return MapUseCases(
                calculateRoute = CalculateRouteUseCase(routingRepository, geoEngine),
                applyRouteCalculationOutcome = ApplyRouteCalculationOutcomeUseCase(buildRoutePresentation),
                resolveRouteRequest = ResolveRouteRequestUseCase(routeOriginResolver),
                startNavigation = StartNavigationUseCase(activeRouteResolver),
                prepareOfflineRoutePack = PrepareOfflineRoutePackUseCase(offlineRegionPlanner, config),
                loadInitialMapScene = LoadInitialMapSceneUseCase(mapCatalogRepository, geoEngine),
                buildRoutePresentation = buildRoutePresentation,
            )
        }
    }
}
