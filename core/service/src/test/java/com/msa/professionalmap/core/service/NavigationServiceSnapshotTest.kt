package com.msa.professionalmap.core.service

import com.msa.professionalmap.core.service.data.NavigationNotificationText
import com.msa.professionalmap.core.service.data.NavigationProgressPresentation
import com.msa.professionalmap.core.service.domain.NavigationServiceConfig
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationServiceSnapshotTest {
    @Test
    fun defaultSnapshotStartsIdle() {
        val snapshot = NavigationServiceSnapshot()
        assertEquals("Destination", snapshot.destinationTitle)
        assertEquals("--", snapshot.remainingDistanceText)
        assertEquals("en", snapshot.languageTag)
    }

    @Test(expected = IllegalArgumentException::class)
    fun configRejectsInvalidNotificationId() {
        NavigationServiceConfig(notificationId = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun snapshotRejectsBlankLanguageTag() {
        NavigationServiceSnapshot(languageTag = " ")
    }

    @Test
    fun notificationCopyUsesPersianForPersianSnapshot() {
        val snapshot = NavigationServiceSnapshot(
            status = NavigationServiceStatus.Active,
            destinationTitle = "تهران",
            languageTag = "fa-IR",
        )

        assertTrue(NavigationNotificationText.contentTitle(snapshot).contains("تهران"))
        assertEquals("توقف موقت", NavigationNotificationText.pauseAction(snapshot.languageTag))
        assertEquals("ادامه", NavigationNotificationText.resumeAction(snapshot.languageTag))
    }


    @Test
    fun persianProgressTextUsesPersianDigits() {
        assertEquals("۱۲.۵ کیلومتر", NavigationProgressPresentation.formatDistance(12_500.0, "fa-IR"))
        assertEquals("۱ ساعت و ۵ دقیقه", NavigationProgressPresentation.formatDuration(3_900.0, "fa-IR"))
    }

    @Test
    fun publicNotificationCopyHidesRouteDetails() {
        val snapshot = NavigationServiceSnapshot(
            status = NavigationServiceStatus.Active,
            destinationTitle = "Sensitive Destination",
            remainingDistanceText = "12 km",
            remainingDurationText = "20 min",
            languageTag = "en",
        )

        assertEquals("Navigation running", NavigationNotificationText.publicContentTitle(snapshot.languageTag))
        assertEquals(
            "Open the app to view route details.",
            NavigationNotificationText.publicContentText(snapshot.languageTag),
        )
    }
}
