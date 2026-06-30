package com.msa.professionalmap.core.mapdata

import org.junit.Assert.assertTrue
import org.junit.Test

class StaticMapRepositoryTest {
    private val repository: MapCatalogRepository = StaticMapRepository()

    @Test
    fun `catalog exposes production reference data`() {
        assertTrue(repository.styles().isNotEmpty())
        assertTrue(repository.referenceRoutePoints().size >= 2)
        assertTrue(repository.pois().isNotEmpty())
    }
}
