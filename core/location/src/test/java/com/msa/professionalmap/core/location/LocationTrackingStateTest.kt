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
}
