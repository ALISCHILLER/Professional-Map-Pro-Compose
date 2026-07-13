package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.geo.KotlinGeoEngine
import com.msa.professionalmap.core.mapdata.MapCatalogRepository
import com.msa.professionalmap.core.mapdata.MapStyleConfig
import com.msa.professionalmap.core.mapdata.StaticMapRepository
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadInitialMapSceneUseCaseTest {
    @Test
    fun invoke_buildsValidatedInitialScene() {
        val useCase = LoadInitialMapSceneUseCase(
            repository = StaticMapRepository(),
            geoEngine = KotlinGeoEngine,
        )

        val scene = useCase(simplificationToleranceMeters = 18.0)

        assertTrue(scene.styles.isNotEmpty())
        assertEquals(scene.styles.first(), scene.selectedStyle)
        assertTrue(scene.referenceRoutePoints.size >= 2)
        assertTrue(scene.simplifiedRoutePoints.size >= 2)
        assertTrue(scene.metrics.totalDistanceMeters > 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invoke_rejectsInvalidReferenceRoute() {
        val useCase = LoadInitialMapSceneUseCase(
            repository = InvalidCatalogRepository,
            geoEngine = KotlinGeoEngine,
        )

        useCase(simplificationToleranceMeters = 18.0)
    }

    private object InvalidCatalogRepository : MapCatalogRepository {
        override fun styles(): List<MapStyleConfig> = MapStyleConfig.DefaultStyles
        override fun referenceRoutePoints(): List<GeoPoint> = listOf(GeoPoint(35.0, 51.0))
        override fun pois(): List<MapPoi> = emptyList()
    }
}
