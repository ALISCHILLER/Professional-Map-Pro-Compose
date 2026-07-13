package com.msa.professionalmap.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal class AndroidLocationPermissionChecker(
    context: Context,
) : LocationPermissionReader {
    private val appContext = context.applicationContext

    override fun detectPermissionLevel(): LocationPermissionLevel {
        val fine = appContext.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = appContext.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        return when {
            fine -> LocationPermissionLevel.Precise
            coarse -> LocationPermissionLevel.Approximate
            else -> LocationPermissionLevel.None
        }
    }

    private fun Context.hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
