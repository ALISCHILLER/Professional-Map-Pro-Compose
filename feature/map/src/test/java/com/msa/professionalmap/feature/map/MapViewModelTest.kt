package com.msa.professionalmap.feature.map

import com.msa.professionalmap.feature.map.presentation.MapLoadState
import com.msa.professionalmap.feature.map.presentation.MapViewModel
import com.msa.professionalmap.feature.map.presentation.MapViewModelFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewModelTest {
    @Test
    fun initialState_containsRouteMetricsAndStyles() {
        val viewModel = MapViewModelFactory(
            dependencies = testMapFeatureDependencies(),
        ).create(MapViewModel::class.java)

        val state = viewModel.uiState.value

        assertTrue(state.loadState is MapLoadState.Ready)
        assertTrue(state.styles.isNotEmpty())
        assertTrue(state.routePoints.size >= 2)
        assertTrue(state.metrics != null)
    }
}
