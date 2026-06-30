package com.msa.professionalmap.core.location

import kotlinx.coroutines.flow.StateFlow

interface LocationRepository : AutoCloseable {
    val trackingState: StateFlow<LocationTrackingState>

    fun setPermissionLevel(permissionLevel: LocationPermissionLevel)
    suspend fun refreshProviderState(): LocationProvidersState
    suspend fun refreshLastKnownLocation()
    fun start(config: LocationConfig = LocationConfig())
    fun stop()

    override fun close() {
        stop()
    }
}
