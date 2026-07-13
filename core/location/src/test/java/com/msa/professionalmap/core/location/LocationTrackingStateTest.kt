package com.msa.professionalmap.core.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTrackingStateTest {
    @Test
    fun canStart_requiresPermissionAndUsableProvider() {
        val noPermission = LocationTrackingState(
            permissionLevel = LocationPermissionLevel.None,
            providers = LocationProvidersState(gpsEnabled = true, networkEnabled = false),
        )
        assertFalse(noPermission.canStart)

        val ready = LocationTrackingState(
            permissionLevel = LocationPermissionLevel.Precise,
            providers = LocationProvidersState(gpsEnabled = true, networkEnabled = false),
        )
        assertTrue(ready.canStart)
    }

    @Test
    fun lastKnownLocationDoesNotPretendTrackingStarted() {
        val location = DeviceLocation(
            position = com.msa.professionalmap.core.model.GeoPoint(35.7, 51.4),
        )

        val updated = LocationTrackingState(
            status = LocationStatus.Idle,
            permissionLevel = LocationPermissionLevel.Precise,
            providers = LocationProvidersState(gpsEnabled = true, networkEnabled = true),
            isTracking = false,
        ).withLocationUpdate(location, trackingUpdate = false)

        assertFalse(updated.isTracking)
        assertTrue(updated.status is LocationStatus.Idle)
        assertTrue(updated.location === location)
    }

    @Test
    fun liveLocationUpdateMarksTrackingActive() {
        val location = DeviceLocation(
            position = com.msa.professionalmap.core.model.GeoPoint(35.7, 51.4),
        )

        val updated = LocationTrackingState(
            status = LocationStatus.Starting,
            permissionLevel = LocationPermissionLevel.Precise,
            providers = LocationProvidersState(gpsEnabled = true, networkEnabled = true),
            isTracking = true,
        ).withLocationUpdate(location, trackingUpdate = true)

        assertTrue(updated.isTracking)
        assertTrue(updated.status is LocationStatus.Active)
    }
}
