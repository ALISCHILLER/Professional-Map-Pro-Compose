package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.GeoEngine
import com.msa.professionalmap.core.mapdata.MapCatalogRepository
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.core.model.RouteMetrics

/**
 * Builds the first map scene shown by the feature.
 *
 * Keeping this bootstrap logic outside the ViewModel makes the startup contract testable and
 * keeps presentation orchestration away from catalog validation, route simplification and
 * derived geometry calculations.
 */
class LoadInitialMapSceneUseCase(
    private val repository: MapCatalogRepository,
    private val geoEngine: GeoEngine,
) {
    operator fun invoke(simplificationToleranceMeters: Double): InitialMapScene {
        val styles = repository.styles()
        val referenceRoute = repository.referenceRoutePoints()
        require(referenceRoute.size >= MinimumReferenceRoutePoints) {
            "Reference route must contain at least $MinimumReferenceRoutePoints points."
        }

        val simplified = geoEngine.simplifyRoute(referenceRoute, simplificationToleranceMeters)
        val bearing = geoEngine.initialBearingDegrees(referenceRoute.first(), referenceRoute.last())
        val projected = geoEngine.destinationPoint(referenceRoute.first(), bearing, ProjectionDistanceMeters)

        return InitialMapScene(
            styles = styles,
            selectedStyle = styles.firstOrNull(),
            referenceRoutePoints = referenceRoute,
            simplifiedRoutePoints = simplified,
            pois = repository.pois(),
            projectedPoint = projected,
            metrics = geoEngine.routeMetrics(referenceRoute, simplified),
        )
    }

    private companion object {
        const val MinimumReferenceRoutePoints = 2
        const val ProjectionDistanceMeters = 2_500.0
    }
}

data class InitialMapScene(
    val styles: List<MapStyleConfig>,
    val selectedStyle: MapStyleConfig?,
    val referenceRoutePoints: List<GeoPoint>,
    val simplifiedRoutePoints: List<GeoPoint>,
    val pois: List<MapPoi>,
    val projectedPoint: GeoPoint,
    val metrics: RouteMetrics,
)
