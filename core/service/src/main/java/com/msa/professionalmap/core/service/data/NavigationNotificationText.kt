package com.msa.professionalmap.core.service.data

import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus

/** Process-safe, privacy-safe notification copy for the foreground navigation service. */
internal object NavigationNotificationText {
    fun channelDescription(languageTag: String): String = if (languageTag.isPersian()) {
        "وضعیت زنده ناوبری قدم‌به‌قدم."
    } else {
        "Foreground turn-by-turn navigation status."
    }

    fun contentTitle(snapshot: NavigationServiceSnapshot): String = if (snapshot.languageTag.isPersian()) {
        when (snapshot.status) {
            NavigationServiceStatus.Paused -> "ناوبری موقتاً متوقف است"
            NavigationServiceStatus.Rerouting -> "در حال محاسبه مسیر جدید"
            NavigationServiceStatus.Stopping -> "در حال توقف ناوبری"
            NavigationServiceStatus.Completed -> "به مقصد رسیدید"
            NavigationServiceStatus.Failed -> "ناوبری در دسترس نیست"
            NavigationServiceStatus.Idle -> "ناوبری آماده است"
            NavigationServiceStatus.Starting -> "در حال شروع ناوبری"
            NavigationServiceStatus.Active -> "در حال ناوبری به ${snapshot.destinationTitle}"
        }
    } else {
        when (snapshot.status) {
            NavigationServiceStatus.Paused -> "Navigation paused"
            NavigationServiceStatus.Rerouting -> "Finding a new route"
            NavigationServiceStatus.Stopping -> "Stopping navigation"
            NavigationServiceStatus.Completed -> "You have arrived"
            NavigationServiceStatus.Failed -> "Navigation unavailable"
            NavigationServiceStatus.Idle -> "Navigation ready"
            NavigationServiceStatus.Starting -> "Starting navigation"
            NavigationServiceStatus.Active -> "Navigating to ${snapshot.destinationTitle}"
        }
    }

    fun contentText(snapshot: NavigationServiceSnapshot): String =
        "${snapshot.remainingDistanceText} • ${snapshot.remainingDurationText}"

    fun fallbackInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "مسیر را ادامه دهید."
    } else {
        "Continue on the route."
    }

    fun pausedInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "ناوبری موقتاً متوقف شد."
    } else {
        "Navigation paused."
    }

    fun reroutingInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "در حال پیدا کردن مسیر جدید…"
    } else {
        "Finding a new route…"
    }

    fun rerouteFailedInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "مسیر جدید پیدا نشد؛ مسیر فعلی را با احتیاط ادامه دهید."
    } else {
        "A new route could not be found. Continue carefully."
    }

    fun offRouteInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "از مسیر خارج شده‌اید."
    } else {
        "You are off route."
    }

    fun arrivedInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "به مقصد رسیدید."
    } else {
        "You have arrived."
    }

    fun failureInstruction(languageTag: String): String = if (languageTag.isPersian()) {
        "ناوبری متوقف شد. برنامه را باز کنید و دوباره تلاش کنید."
    } else {
        "Navigation stopped. Open the app and try again."
    }

    fun defaultDestination(languageTag: String): String = if (languageTag.isPersian()) "مقصد" else "Destination"

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
