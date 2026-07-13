package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.mapdata.StaticMapRepository
import com.msa.professionalmap.core.progress.data.DefaultRouteProgressRepository
import com.msa.professionalmap.feature.map.TestRoutingRepository
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewModelRuntimeTest {
    @Test
    fun create_returnsExplicitRuntimePolicyGraph() {
        val config = MapFeatureConfig(routeRequestDebounceMillis = 0L)
        val runtime = MapViewModelRuntime.create(
            routingRepository = TestRoutingRepository(),
            progressRepository = DefaultRouteProgressRepository(),
            geoEngine = KotlinGeoEngine,
            mapCatalogRepository = StaticMapRepository(),
            featureConfig = config,
        )

        assertSame(config, runtime.featureConfig)
        assertTrue(runtime.progressConfig.rerouteDebounceMillis >= 0L)
        assertTrue(runtime.updateNavigationProgress is UpdateNavigationProgressUseCase)
    }
}
