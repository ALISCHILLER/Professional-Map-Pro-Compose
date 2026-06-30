package com.msa.professionalmap.core.service.data

import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus

/**
 * Process-safe notification copy for the foreground navigation service.
 *
 * The service module intentionally does not depend on app resources or the guidance module;
 * snapshots carry a BCP-47 language tag and this formatter keeps the service copy localized
 * without leaking UI/i18n concerns into Android service lifecycle code.
 */
internal object NavigationNotificationText {
    fun channelDescription(languageTag: String): String = if (languageTag.isPersian()) {
        "وضعیت زنده ناوبری قدم‌به‌قدم."
    } else {
        "Foreground turn-by-turn navigation status."
    }

    fun contentTitle(snapshot: NavigationServiceSnapshot): String = if (snapshot.languageTag.isPersian()) {
        when (snapshot.status) {
            NavigationServiceStatus.Paused -> "ناوبری متوقف موقت شد"
            NavigationServiceStatus.Stopping -> "ناوبری در حال توقف است"
            NavigationServiceStatus.Idle -> "ناوبری آماده است"
            NavigationServiceStatus.Starting,
            NavigationServiceStatus.Active -> "در حال ناوبری به ${snapshot.destinationTitle}"
        }
    } else {
        when (snapshot.status) {
            NavigationServiceStatus.Paused -> "Navigation paused"
            NavigationServiceStatus.Stopping -> "Stopping navigation"
            NavigationServiceStatus.Idle -> "Navigation ready"
            NavigationServiceStatus.Starting,
            NavigationServiceStatus.Active -> "Navigating to ${snapshot.destinationTitle}"
        }
    }

    fun contentText(snapshot: NavigationServiceSnapshot): String =
        "${snapshot.remainingDistanceText} • ${snapshot.remainingDurationText}"

    fun fallbackInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "ناوبری فعال است."
    } else {
        "Navigation is active."
    }

    fun pausedInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "ناوبری متوقف موقت شد."
    } else {
        "Navigation paused."
    }

    fun stoppedInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "ناوبری متوقف شد."
    } else {
        "Navigation stopped."
    }

    fun publicContentTitle(languageTag: String): String = if (languageTag.isPersian()) {
        "ناوبری فعال است"
    } else {
        "Navigation running"
    }

    fun publicContentText(languageTag: String): String = if (languageTag.isPersian()) {
        "برای دیدن جزئیات مسیر، برنامه را باز کنید."
    } else {
        "Open the app to view route details."
    }

    fun pauseAction(languageTag: String): String = if (languageTag.isPersian()) "توقف موقت" else "Pause"

    fun resumeAction(languageTag: String): String = if (languageTag.isPersian()) "ادامه" else "Resume"

    fun stopAction(languageTag: String): String = if (languageTag.isPersian()) "توقف" else "Stop"

    private fun String.isPersian(): Boolean = trim().lowercase().startsWith("fa")
}
