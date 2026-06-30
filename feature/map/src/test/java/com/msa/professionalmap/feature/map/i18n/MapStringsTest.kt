package com.msa.professionalmap.feature.map.i18n

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.GuidanceState
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.offline.domain.OfflineManagerState
import com.msa.professionalmap.core.offline.domain.OfflineWorkProgress
import com.msa.professionalmap.core.offline.domain.OfflineWorkStatus
import com.msa.professionalmap.feature.map.presentation.MapUiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStringsTest {
    @Test
    fun persianStringsUseRtlAndPersianDigits() {
        val strings = MapStrings.forLanguage(GuidanceLanguage.Persian)

        assertTrue(strings.isRtl)
        assertEquals("دقیق", strings.permissionLabel(LocationPermissionLevel.Precise))
        assertEquals("۱۲ کیلومتر", strings.distanceText(12_000.0))
        assertEquals("۸۵٪", strings.percentText(0.85))
    }

    @Test
    fun englishStringsStayLtrAndHumanReadable() {
        val strings = MapStrings.forLanguage(GuidanceLanguage.English)

        assertEquals(false, strings.isRtl)
        assertEquals("Precise", strings.permissionLabel(LocationPermissionLevel.Precise))
        assertEquals("12.0 km", strings.distanceText(12_000.0))
        assertEquals("85%", strings.percentText(0.85))
    }

    @Test
    fun externalSubsystemStatusIsLocalizedThroughExplicitHelpers() {
        val strings = MapStrings.forLanguage(GuidanceLanguage.Persian)

        assertEquals("خطای تبدیل متن به گفتار", strings.guidanceStatusText(GuidanceState(lastErrorMessage = "TTS error")))
        assertEquals("به مقصد رسیدید.", strings.offlineStatusText(OfflineManagerState(lastMessage = "Arrived at destination.")))
        assertEquals(
            "کارگر Running: route-1 ۵۰٪",
            strings.offlineWorkerText(
                OfflineWorkProgress(
                    clientId = "route-1",
                    status = OfflineWorkStatus.Running,
                    progressPercent = 50,
                )
            )
        )
    }
    @Test
    fun typedMessagesAreLocalizedWithoutStringMatching() {
        val fa = MapStrings.forLanguage(GuidanceLanguage.Persian)
        val en = MapStrings.forLanguage(GuidanceLanguage.English)

        assertEquals("ناوبری شروع شد.", fa.messageText(MapUiMessage.NavigationStarted))
        assertEquals("Navigation started.", en.messageText(MapUiMessage.NavigationStarted))
        assertEquals("حجم صدا: ۷۰٪.", fa.messageText(MapUiMessage.GuidanceVolumeChanged(70)))
    }

}
