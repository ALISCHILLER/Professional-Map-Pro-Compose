package com.msa.professionalmap.core.geo

import com.msa.professionalmap.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinGeoEngineTest {
    private val engine = KotlinGeoEngine

    @Test
    fun distanceBetweenTehranAndKaraj_isReasonable() {
        val tehran = GeoPoint(35.6892, 51.3890)
        val karaj = GeoPoint(35.8327, 50.9915)
        val distanceKm = engine.distanceMeters(tehran, karaj) / 1000.0
        assertTrue(distanceKm in 35.0..45.0)
    }

    @Test
    fun simplifyRoute_keepsFirstAndLastPoints() {
        val points = listOf(
            GeoPoint(35.0, 51.0),
            GeoPoint(35.001, 51.001),
            GeoPoint(35.002, 51.002),
            GeoPoint(35.010, 51.010),
        )
        val simplified = engine.simplifyRoute(points, toleranceMeters = 100.0)
        assertEquals(points.first(), simplified.first())
        assertEquals(points.last(), simplified.last())
    }
}
