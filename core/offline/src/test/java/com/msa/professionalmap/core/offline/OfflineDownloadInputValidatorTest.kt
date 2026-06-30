package com.msa.professionalmap.core.offline

import com.msa.professionalmap.core.model.GeoBounds
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.offline.data.OfflineDownloadInputValidator
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineDownloadInputValidatorTest {
    @Test
    fun acceptsValidRequest() {
        val request = validRequest()

        assertEquals(request, OfflineDownloadInputValidator.validate(request))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCleartextStyleUrl() {
        OfflineDownloadInputValidator.validate(validRequest(styleUrl = "http://example.com/style.json"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidZoomRange() {
        OfflineDownloadInputValidator.validate(validRequest(minZoom = 18.0, maxZoom = 10.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidBounds() {
        OfflineDownloadInputValidator.validate(
            validRequest(
                bounds = GeoBounds(
                    southWest = GeoPoint(36.0, 52.0),
                    northEast = GeoPoint(35.0, 51.0),
                )
            )
        )
    }

    private fun validRequest(
        styleUrl: String = "https://example.com/style.json",
        minZoom: Double = 10.0,
        maxZoom: Double = 16.0,
        bounds: GeoBounds = GeoBounds(
            southWest = GeoPoint(35.0, 51.0),
            northEast = GeoPoint(36.0, 52.0),
        ),
    ): OfflineRegionRequest = OfflineRegionRequest(
        clientId = "region-1",
        title = "Region",
        styleUrl = styleUrl,
        bounds = bounds,
        minZoom = minZoom,
        maxZoom = maxZoom,
    )
}
