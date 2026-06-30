package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.model.GeoPoint
import org.junit.Assert.assertTrue
import org.junit.Test

class PrepareOfflineRoutePackUseCaseTest {
    private val style = MapStyleConfig(id = "reference", title = "Reference", url = "https://example.com/style.json")
    private val route = listOf(
        GeoPoint(latitude = 35.0, longitude = 51.0),
        GeoPoint(latitude = 35.1, longitude = 51.1),
    )

    @Test
    fun `returns missing style when style is null`() {
        val result = PrepareOfflineRoutePackUseCase()(style = null, routePoints = route, fallbackRoutePoints = emptyList())

        assertTrue(result is OfflineRoutePackResult.MissingStyle)
    }

    @Test
    fun `creates pack request when style and route are valid`() {
        val result = PrepareOfflineRoutePackUseCase()(style = style, routePoints = route, fallbackRoutePoints = emptyList())

        assertTrue(result is OfflineRoutePackResult.Ready)
    }
}
