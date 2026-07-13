package com.msa.professionalmap.core.offline.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.msa.professionalmap.core.model.GeoBounds
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.offline.domain.OfflineRegionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineWorkPayloadCipherTest {
    @Test
    fun workDataContainsCiphertextInsteadOfExactBoundsOrStyleUrl() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val request = OfflineRegionRequest(
            clientId = "route-private-1234",
            title = "Private route",
            styleUrl = "https://tiles.example.invalid/private-style.json?token=secret-value",
            bounds = GeoBounds(
                southWest = GeoPoint(35.6892, 51.3890),
                northEast = GeoPoint(35.7000, 51.4000),
            ),
        )

        val data = OfflineDownloadWorkerDataMapper.inputData(context, request, "fa-IR")
        val encrypted = data.getString(OfflineDownloadWorkerKeys.EncryptedRequest)
        val restored = OfflineDownloadWorkerDataMapper.run {
            data.toOfflineDownloadPayload(context)
        }

        assertNotNull(encrypted)
        assertFalse(encrypted.orEmpty().contains("35.6892"))
        assertFalse(encrypted.orEmpty().contains("51.389"))
        assertFalse(encrypted.orEmpty().contains("secret-value"))
        assertEquals(request, restored.request)
        assertEquals("fa-IR", restored.languageTag)
    }
}
