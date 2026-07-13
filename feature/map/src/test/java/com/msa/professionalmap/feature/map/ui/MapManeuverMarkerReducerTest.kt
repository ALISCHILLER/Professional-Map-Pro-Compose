package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteInstruction
import com.msa.professionalmap.core.model.RouteLeg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapManeuverMarkerReducerTest {
    @Test
    fun removesEndpointAndCloselySpacedManeuvers() {
        val origin = GeoPoint(35.7000, 51.4000)
        val destination = GeoPoint(35.7100, 51.4100)
        val points = listOf(
            origin,
            GeoPoint(35.7001, 51.4001),
            GeoPoint(35.7040, 51.4040),
            GeoPoint(35.7041, 51.4041),
            destination,
        )
        val route = RouteAlternative(
            id = "route",
            title = "Route",
            summary = "",
            points = listOf(origin, destination),
            distanceMeters = 1_500.0,
            durationSeconds = 300.0,
            provider = "test",
            legs = listOf(
                RouteLeg(
                    summary = "",
                    distanceMeters = 1_500.0,
                    durationSeconds = 300.0,
                    steps = points.map { point ->
                        RouteInstruction("Continue", null, null, null, point, 100.0, 20.0)
                    },
                )
            ),
        )

        val reduced = MapManeuverMarkerReducer.reduce(route)

        assertEquals(1, reduced.size)
        assertTrue(reduced.single().latitude in 35.7039..35.7042)
    }
}
