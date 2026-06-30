package com.msa.professionalmap.core.location

import android.content.Context
import android.location.LocationManager
import android.provider.Settings

internal class AndroidLocationProviderStateReader(
    context: Context,
) : LocationProviderStateReader {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun readProviderState(): LocationProvidersState = LocationProvidersState(
        gpsEnabled = isProviderEnabled(LocationManager.GPS_PROVIDER),
        networkEnabled = isProviderEnabled(LocationManager.NETWORK_PROVIDER),
        airplaneModeOn = isAirplaneModeEnabled(),
    )

    private fun isProviderEnabled(provider: String): Boolean = runCatching {
        locationManager.isProviderEnabled(provider)
    }.getOrDefault(false)

    private fun isAirplaneModeEnabled(): Boolean = runCatching {
        Settings.Global.getInt(appContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    }.getOrDefault(false)
}
