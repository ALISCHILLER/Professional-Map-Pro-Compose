package com.msa.professionalmap.core.service.domain

import com.msa.professionalmap.core.model.GeoPoint

enum class NavigationServiceStatus {
    Idle,
    Starting,
    Active,
    Paused,
    Stopping,
}

data class NavigationServiceConfig(
    val notificationChannelId: String = "navigation_service",
    val notificationChannelName: String = "Navigation",
    val notificationId: Int = 4041,
    val useWakeLock: Boolean = true,
    val autoStopAfterInactiveMillis: Long = 30L * 60L * 1000L,
) {
    init {
        require(notificationId > 0) { "notificationId must be positive." }
        require(autoStopAfterInactiveMillis > 0L) { "autoStopAfterInactiveMillis must be positive." }
    }
}

data class NavigationServiceSnapshot(
    val status: NavigationServiceStatus = NavigationServiceStatus.Idle,
    val destinationTitle: String = "Destination",
    val remainingDistanceText: String = "--",
    val remainingDurationText: String = "--",
    val nextInstructionText: String? = null,
    val destination: GeoPoint? = null,
    val languageTag: String = "en",
    val lastUpdatedAtMillis: Long = 0L,
) {
    init {
        require(languageTag.isNotBlank()) { "languageTag must not be blank." }
    }
}

interface NavigationServiceController {
    fun start(snapshot: NavigationServiceSnapshot)
    fun update(snapshot: NavigationServiceSnapshot)
    fun pause()
    fun resume(snapshot: NavigationServiceSnapshot)
    fun stop()
}
