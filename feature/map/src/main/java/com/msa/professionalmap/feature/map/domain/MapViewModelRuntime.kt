package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.GeoEngine
import com.msa.professionalmap.core.mapdata.MapCatalogRepository
import com.msa.professionalmap.core.progress.domain.ReroutingStrategy
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig
import com.msa.professionalmap.core.progress.domain.RouteProgressRepository
import com.msa.professionalmap.core.routing.RoutingRepository

/**
 * Stateful runtime policies owned by one MapViewModel instance.
 *
 * The ViewModel coordinates UI events; this object owns feature policies, stateful
 * guards and application use cases. Keeping the runtime graph explicit prevents
 * hidden construction in presentation code and makes tests/composition roots decide
 * which concrete behavior is used.
 */
class MapViewModelRuntime(
    val featureConfig: MapFeatureConfig,
    val progressConfig: RouteProgressConfig,
    val reroutingStrategy: ReroutingStrategy,
    val activeRouteResolver: ActiveRouteResolver,
    val routeOriginResolver: RouteOriginResolver,
    val updateNavigationProgress: UpdateNavigationProgressUseCase,
    val useCases: MapUseCases,
) {
    companion object {
        fun create(
            routingRepository: RoutingRepository,
            progressRepository: RouteProgressRepository,
            geoEngine: GeoEngine,
            mapCatalogRepository: MapCatalogRepository,
            featureConfig: MapFeatureConfig = MapFeatureConfig(),
            progressConfig: RouteProgressConfig = RouteProgressConfig(),
        ): MapViewModelRuntime {
            val activeRouteResolver = ActiveRouteResolver(geoEngine)
            val routeOriginResolver = RouteOriginResolver()
            val routeSplitter = RouteProgressRouteSplitter()
            val arrivalGuard = ArrivalConfirmationGuard(featureConfig)
            val progressUpdateThrottle = ProgressUpdateThrottle(featureConfig)
            return MapViewModelRuntime(
                featureConfig = featureConfig,
                progressConfig = progressConfig,
                reroutingStrategy = ReroutingStrategy(progressConfig.rerouteDebounceMillis),
                activeRouteResolver = activeRouteResolver,
                routeOriginResolver = routeOriginResolver,
                updateNavigationProgress = UpdateNavigationProgressUseCase(
                    progressRepository = progressRepository,
                    activeRouteResolver = activeRouteResolver,
                    routeSplitter = routeSplitter,
                    arrivalGuard = arrivalGuard,
                    progressUpdateThrottle = progressUpdateThrottle,
                ),
                useCases = MapUseCases.create(
                    routingRepository = routingRepository,
                    geoEngine = geoEngine,
                    mapCatalogRepository = mapCatalogRepository,
                    activeRouteResolver = activeRouteResolver,
                    routeOriginResolver = routeOriginResolver,
                    config = featureConfig,
                ),
            )
        }
    }
}
