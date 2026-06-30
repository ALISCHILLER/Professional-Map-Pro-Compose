package com.msa.professionalmap.core.location

/**
 * Single source of truth for device location tracking.
 */
data class LocationTrackingState(
    val status: LocationStatus = LocationStatus.Idle,
    val permissionLevel: LocationPermissionLevel = LocationPermissionLevel.None,
    val providers: LocationProvidersState = LocationProvidersState.Unknown,
    val location: DeviceLocation? = null,
    val lastErrorMessage: String? = null,
    val isTracking: Boolean = false,
) {
    val canStart: Boolean
        get() = permissionLevel != LocationPermissionLevel.None && providers.isUsable
}

sealed interface LocationStatus {
    data object Idle : LocationStatus
    data object Checking : LocationStatus
    data object PermissionRequired : LocationStatus
    data object ProviderDisabled : LocationStatus
    data object Starting : LocationStatus
    data object Active : LocationStatus
    data object Stopped : LocationStatus
    data class Error(val message: String) : LocationStatus
}

data class LocationProvidersState(
    val gpsEnabled: Boolean,
    val networkEnabled: Boolean,
    val airplaneModeOn: Boolean = false,
) {
    val isUsable: Boolean get() = gpsEnabled || networkEnabled

    companion object {
        val Unknown: LocationProvidersState = LocationProvidersState(
            gpsEnabled = false,
            networkEnabled = false,
            airplaneModeOn = false,
        )
    }
}
