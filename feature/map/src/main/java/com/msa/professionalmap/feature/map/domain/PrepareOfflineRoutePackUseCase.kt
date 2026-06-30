package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.offline.domain.OfflineRegionPlanner
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest

/**
 * Converts UI route/style state into a validated offline download request.
 *
 * ViewModels should not own bounds, zoom and region identity policies. This use
 * case keeps that business decision testable and reusable.
 */
class PrepareOfflineRoutePackUseCase(
    private val planner: OfflineRegionPlanner = OfflineRegionPlanner(),
    private val config: MapFeatureConfig = MapFeatureConfig(),
) {
    operator fun invoke(
        style: MapStyleConfig?,
        routePoints: List<GeoPoint>,
        fallbackRoutePoints: List<GeoPoint>,
    ): OfflineRoutePackResult {
        val resolvedStyle = style ?: return OfflineRoutePackResult.MissingStyle
        val points = routePoints.takeIf { it.size >= 2 } ?: fallbackRoutePoints
        if (points.size < 2) return OfflineRoutePackResult.MissingRoute
        return OfflineRoutePackResult.Ready(
            request = planner.createRoutePackRequest(
                routePoints = points,
                styleId = resolvedStyle.id,
                styleTitle = resolvedStyle.title,
                styleUrl = resolvedStyle.url,
                pixelRatio = config.offlinePixelRatio,
                includeIdeographs = config.offlineIncludeIdeographs,
            )
        )
    }
}

sealed interface OfflineRoutePackResult {
    data object MissingStyle : OfflineRoutePackResult
    data object MissingRoute : OfflineRoutePackResult
    data class Ready(val request: OfflineRegionRequest) : OfflineRoutePackResult
}
