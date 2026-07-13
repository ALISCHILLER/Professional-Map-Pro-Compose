package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.location.LocationConfig
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.location.LocationProvidersState
import com.msa.professionalmap.core.location.LocationRepository
import com.msa.professionalmap.core.location.LocationTrackingState
import com.msa.professionalmap.feature.map.domain.MapFeatureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapLocationControllerTest {
    @Test
    fun `revoking location permission stops tracking and independent navigation`() {
        val repository = FakeLocationRepository()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val state = MutableStateFlow(
            MapUiState(
                navigationActive = true,
                followUserLocation = true,
            )
        )
        var stopMessage: MapUiMessage? = null
        val controller = MapLocationController(
            state = state,
            scope = scope,
            locationRepository = repository,
            featureConfig = MapFeatureConfig(),
            stopNavigation = { stopMessage = it },
        )

        controller.onPermissionChanged(LocationPermissionLevel.None, autoStart = false)

        assertTrue(repository.stopCalled)
        assertEquals(LocationPermissionLevel.None, repository.permissionLevel)
        assertEquals(MapUiMessage.LocationPermissionRequired, stopMessage)
        scope.cancel()
    }

    private class FakeLocationRepository : LocationRepository {
        private val mutableTrackingState = MutableStateFlow(LocationTrackingState())
        override val trackingState: StateFlow<LocationTrackingState> = mutableTrackingState

        var permissionLevel: LocationPermissionLevel? = null
        var stopCalled: Boolean = false

        override fun setPermissionLevel(permissionLevel: LocationPermissionLevel) {
            this.permissionLevel = permissionLevel
        }

        override suspend fun refreshProviderState(): LocationProvidersState = LocationProvidersState.Unknown

        override suspend fun refreshLastKnownLocation() = Unit

        override fun start(config: LocationConfig) = Unit

        override fun stop() {
            stopCalled = true
        }
    }
}
