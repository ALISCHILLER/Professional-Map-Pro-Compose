package com.msa.professionalmap.core.location

import android.location.Location
import android.os.Build
import com.msa.professionalmap.core.model.GeoPoint

@Suppress("DEPRECATION")
internal fun Location.toDeviceLocation(): DeviceLocation = DeviceLocation(
    position = GeoPoint(latitude = latitude, longitude = longitude),
    accuracyMeters = if (hasAccuracy()) accuracy else null,
    altitudeMeters = if (hasAltitude()) altitude else null,
    bearingDegrees = if (hasBearing()) bearing else null,
    speedMetersPerSecond = if (hasSpeed()) speed else null,
    timestampMillis = time,
    provider = provider,
    isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        isFromMockProvider
    },
)
