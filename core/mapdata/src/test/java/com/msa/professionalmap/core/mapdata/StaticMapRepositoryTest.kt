package com.msa.professionalmap.core.mapdata

import org.junit.Assert.assertEquals
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

    @Test
    fun `catalog exposes a dedicated secure dark style`() {
        val dark = repository.styles().single { it.id == "dark" }

        assertEquals("https", dark.url.substringBefore(":"))
        assertTrue(dark.isDark)
        assertEquals(MapStyleConfig.Dark, dark)
    }

}
