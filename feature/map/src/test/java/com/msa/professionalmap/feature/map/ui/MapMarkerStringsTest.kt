package com.msa.professionalmap.feature.map.ui

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.MapPoi
import com.msa.professionalmap.core.model.PoiCategory
import com.msa.professionalmap.feature.map.i18n.MapStrings
import org.junit.Assert.assertEquals
import org.junit.Test

class MapMarkerStringsTest {
    private val poi = MapPoi(
        id = "warehouse-east",
        title = "East Warehouse",
        subtitle = "Logistics and fulfillment",
        position = GeoPoint(35.72, 51.46),
        category = PoiCategory.Warehouse,
    )

    @Test
    fun localizesKnownReferencePoiWithoutChangingDomainObject() {
        val localized = poi.localizedForMap(MapStrings.forLanguage(GuidanceLanguage.Persian))

        assertEquals("انبار شرق", localized.title)
        assertEquals("لجستیک و آماده‌سازی", localized.subtitle)
        assertEquals("East Warehouse", poi.title)
    }

    @Test
    fun localizesCategoryLabel() {
        val strings = MapStrings.forLanguage(GuidanceLanguage.Persian)
        assertEquals("انبار", PoiCategory.Warehouse.localizedLabel(strings))
    }
}
