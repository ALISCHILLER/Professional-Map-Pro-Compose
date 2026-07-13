package com.msa.professionalmap.feature.map.presentation

import com.msa.professionalmap.core.location.LocationConfig
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.location.LocationRepository
import com.msa.professionalmap.feature.map.domain.MapFeatureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns permission, provider and live-location orchestration for the map screen.
 *
 * Keeping this flow outside `MapViewModel` prevents the main orchestrator from becoming a
 * location state machine and makes GPS behavior easier to test in isolation.
 */
internal class MapLocationController(
    private val state: MutableStateFlow<MapUiState>,
    private val scope: CoroutineScope,
    private val locationRepository: LocationRepository,
    private val featureConfig: MapFeatureConfig,
    private val stopNavigation: (MapUiMessage) -> Unit,
) {
    fun observe() {
        scope.launch {
            locationRepository.trackingState.collectLatest { trackingState ->
                state.update { current ->
                    current.copy(
                        locationTrackingState = trackingState,
                        currentLocation = trackingState.location,
                        lastAction = trackingState.status.toUiMessage() ?: current.lastAction,
                    )
                }
            }
        }
    }

    fun onPermissionChanged(permissionLevel: LocationPermissionLevel, autoStart: Boolean) {
        if (permissionLevel == LocationPermissionLevel.None) {
            locationRepository.stop()
            locationRepository.setPermissionLevel(permissionLevel)
            stopNavigation(MapUiMessage.LocationPermissionRequired)
            return
        }

        locationRepository.setPermissionLevel(permissionLevel)
        scope.launch {
            locationRepository.refreshProviderState()
            locationRepository.refreshLastKnownLocation()
            if (autoStart) startTracking()
        }
    }

    fun startTracking() {
        locationRepository.start(
            LocationConfig(
                intervalMillis = featureConfig.locationUpdateIntervalMillis,
                minUpdateIntervalMillis = featureConfig.locationMinUpdateIntervalMillis,
                minDistanceMeters = featureConfig.locationMinDistanceMeters,
                waitForAccurateLocation = featureConfig.waitForAccurateLocation,
            )
        )
        state.update { it.copy(lastAction = MapUiMessage.StartingGps) }
    }

    fun stopTracking() {
        locationRepository.stop()
        stopNavigation(MapUiMessage.GpsStopped)
    }

    fun toggleFollowUserLocation() {
        state.update { current ->
            val next = !current.followUserLocation
            current.copy(
                followUserLocation = next,
                lastAction = if (next) MapUiMessage.FollowEnabled else MapUiMessage.FollowDisabled,
            )
        }
    }

    fun close() {
        locationRepository.close()
    }
}
