package com.msa.professionalmap.core.location

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

internal object AndroidLocationRequestMapper {
    fun LocationConfig.toLocationRequest(): LocationRequest = LocationRequest.Builder(priority.toGmsPriority(), intervalMillis)
        .setMinUpdateIntervalMillis(minUpdateIntervalMillis)
        .setMinUpdateDistanceMeters(minDistanceMeters)
        .setWaitForAccurateLocation(waitForAccurateLocation)
        .build()

    private fun LocationPriority.toGmsPriority(): Int = when (this) {
        LocationPriority.HighAccuracy -> Priority.PRIORITY_HIGH_ACCURACY
        LocationPriority.Balanced -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
        LocationPriority.LowPower -> Priority.PRIORITY_LOW_POWER
        LocationPriority.Passive -> Priority.PRIORITY_PASSIVE
    }
}
