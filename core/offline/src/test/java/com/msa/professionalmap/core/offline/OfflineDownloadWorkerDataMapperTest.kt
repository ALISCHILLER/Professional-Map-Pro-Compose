package com.msa.professionalmap.core.offline

import com.msa.professionalmap.core.model.GeoBounds
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.offline.data.OfflineDownloadWorker
import com.msa.professionalmap.core.offline.data.OfflineDownloadWorkerDataMapper
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineDownloadWorkerDataMapperTest {
    @Test
    fun roundTripsOfflineRegionRequestThroughWorkData() {
        val request = OfflineRegionRequest(
            clientId = "tehran-core",
            title = "Tehran Core",
            styleUrl = "https://example.com/style.json",
            bounds = GeoBounds(
                southWest = GeoPoint(35.60, 51.20),
                northEast = GeoPoint(35.82, 51.60),
            ),
            minZoom = 9.0,
            maxZoom = 15.0,
            pixelRatio = 3.0f,
            includeIdeographs = true,
        )

        val restored = OfflineDownloadWorkerDataMapper.run {
            OfflineDownloadWorker.inputData(request, "fa-IR").toOfflineRegionRequest()
        }

        assertEquals(request, restored)
    }

    @Test
    fun progressPercentIsClampedBeforePublishing() {
        val data = OfflineDownloadWorker.progressData(
            clientId = "region-1",
            percent = 125,
            downloadedTiles = 10L,
            totalTiles = 20L,
            message = "Downloading",
        )

        assertEquals(100, data.getInt(OfflineDownloadWorker.KeyProgressPercent, -1))
    }

    @Test
    fun progressTileCountsAreNeverPublishedAsNegative() {
        val data = OfflineDownloadWorker.progressData(
            clientId = "region-1",
            percent = 25,
            downloadedTiles = -10L,
            totalTiles = -20L,
            message = "Downloading",
        )

        assertEquals(0L, data.getLong(OfflineDownloadWorker.KeyDownloadedTiles, -1L))
        assertEquals(0L, data.getLong(OfflineDownloadWorker.KeyTotalTiles, -1L))
    }
}
