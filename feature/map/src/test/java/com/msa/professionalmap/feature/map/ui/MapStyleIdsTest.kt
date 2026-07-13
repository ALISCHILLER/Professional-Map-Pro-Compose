package com.msa.professionalmap.feature.map.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStyleIdsTest {
    @Test
    fun mapLibreSourceAndLayerIdsRemainUnique() {
        val ids = listOf(
            MapStyleIds.PoiSource,
            MapStyleIds.PoiShadowLayer,
            MapStyleIds.PoiSymbolLayer,
            MapStyleIds.PoiLabelLayer,
            MapStyleIds.PoiSelectedSource,
            MapStyleIds.PoiSelectedHaloLayer,
            MapStyleIds.PoiSelectedCoreLayer,
            MapStyleIds.PoiSelectedSymbolLayer,
            MapStyleIds.PoiSelectedLabelLayer,
            MapStyleIds.ClusterShadowLayer,
            MapStyleIds.ClusterLayerSmall,
            MapStyleIds.ClusterLayerMedium,
            MapStyleIds.ClusterLayerLarge,
            MapStyleIds.ClusterCountLayer,
            MapStyleIds.RouteAlternativesSource,
            MapStyleIds.RouteHitLayer,
            MapStyleIds.AlternativeRouteLayer,
            MapStyleIds.SelectedRouteCasingLayer,
            MapStyleIds.SelectedRouteLayer,
            MapStyleIds.CompletedRouteSource,
            MapStyleIds.CompletedRouteLayer,
            MapStyleIds.RemainingRouteSource,
            MapStyleIds.RemainingRouteCasingLayer,
            MapStyleIds.RemainingRouteLayer,
            MapStyleIds.SimplifiedRouteSource,
            MapStyleIds.SimplifiedRouteLayer,
            MapStyleIds.EndpointSource,
            MapStyleIds.EndpointShadowLayer,
            MapStyleIds.EndpointCircleLayer,
            MapStyleIds.EndpointDestinationLayer,
            MapStyleIds.EndpointLabelLayer,
            MapStyleIds.ManeuverSource,
            MapStyleIds.ManeuverCircleLayer,
            MapStyleIds.ManeuverLabelLayer,
            MapStyleIds.SelectedPointSource,
            MapStyleIds.SelectedPointHaloLayer,
            MapStyleIds.SelectedPointLayer,
            MapStyleIds.ProjectedPointSource,
            MapStyleIds.ProjectedPointLayer,
            MapStyleIds.InstructionPointSource,
            MapStyleIds.InstructionPointLayer,
            MapStyleIds.UserLocationSource,
            MapStyleIds.UserLocationHaloLayer,
            MapStyleIds.UserLocationLayer,
            MapStyleIds.UserHeadingSource,
            MapStyleIds.UserHeadingLayer,
            MapStyleIds.SnappedLocationSource,
            MapStyleIds.SnappedLocationLayer,
        )

        assertEquals(ids.size, ids.toSet().size)
        assertTrue(MapStyleIds.RouteHitLayer.contains("hit-target"))
    }
}
