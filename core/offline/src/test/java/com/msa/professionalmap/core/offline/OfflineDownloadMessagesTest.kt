package com.msa.professionalmap.core.offline

import com.msa.professionalmap.core.offline.data.OfflineDownloadMessages
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineDownloadMessagesTest {
    @Test
    fun englishMessagesUseAsciiPercent() {
        val messages = OfflineDownloadMessages.from("en")

        assertEquals("Downloading offline map", messages.notificationTitle())
        assertEquals("Region · 45%", messages.notificationBody("Region", 45))
        assertEquals("Downloading 45%", messages.progress(45))
        assertEquals("Offline region ready.", messages.ready())
    }

    @Test
    fun persianMessagesUseLocalizedDigits() {
        val messages = OfflineDownloadMessages.from("fa-IR")

        assertEquals("در حال دانلود نقشه آفلاین", messages.notificationTitle())
        assertEquals("ناحیه · ۴۵٪", messages.notificationBody("ناحیه", 45))
        assertEquals("در حال دانلود ۴۵٪", messages.progress(45))
        assertEquals("نقشه آفلاین آماده است.", messages.ready())
    }

    @Test
    fun percentagesAreClamped() {
        val messages = OfflineDownloadMessages.from("en")

        assertEquals("Region · 100%", messages.notificationBody("Region", 250))
        assertEquals("Downloading 0%", messages.progress(-10))
    }
}
