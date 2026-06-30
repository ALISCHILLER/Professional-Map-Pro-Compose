package com.msa.professionalmap.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.msa.professionalmap.core.location.AndroidLocationRequestMapper.toLocationRequest
import com.msa.professionalmap.core.model.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AndroidFusedLocationRepository internal constructor(
    context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val permissionReader: LocationPermissionReader,
    private val providerStateReader: LocationProviderStateReader,
    private val scope: CoroutineScope,
) : LocationRepository {

    constructor(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context),
    ) : this(
        context = context,
        fusedLocationClient = fusedLocationClient,
        permissionReader = AndroidLocationPermissionChecker(context),
        providerStateReader = AndroidLocationProviderStateReader(context),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    )

    private val providerChangeMonitor = AndroidProviderChangeMonitor(
        context = context,
        scope = scope,
        onProviderChanged = ::refreshProviderState,
    )

    private val _trackingState = MutableStateFlow(LocationTrackingState(status = LocationStatus.Checking))
    override val trackingState: StateFlow<LocationTrackingState> = _trackingState

    private var callback: LocationCallback? = null

    init {
        providerChangeMonitor.start()
        setPermissionLevel(permissionReader.detectPermissionLevel())
        scope.launch { refreshProviderState() }
    }

    override fun setPermissionLevel(permissionLevel: LocationPermissionLevel) {
        if (permissionLevel == LocationPermissionLevel.None) {
            stopInternal(updateState = false)
        }

        _trackingState.update { state ->
            val canKeepTracking = permissionLevel != LocationPermissionLevel.None && state.isTracking

            val status = when {
                permissionLevel == LocationPermissionLevel.None -> LocationStatus.PermissionRequired
                !state.providers.isUsable -> LocationStatus.ProviderDisabled
                canKeepTracking -> LocationStatus.Active
                else -> LocationStatus.Idle
            }

            state.copy(
                permissionLevel = permissionLevel,
                status = status,
                isTracking = canKeepTracking,
                lastErrorMessage = null,
            )
        }
    }

    override suspend fun refreshProviderState(): LocationProvidersState {
        val providers = providerStateReader.readProviderState()

        _trackingState.update { state ->
            state.copy(
                providers = providers,
                status = when {
                    state.permissionLevel == LocationPermissionLevel.None -> LocationStatus.PermissionRequired
                    !providers.isUsable -> LocationStatus.ProviderDisabled
                    state.isTracking -> LocationStatus.Active
                    state.status == LocationStatus.ProviderDisabled -> LocationStatus.Idle
                    else -> state.status
                },
            )
        }

        return providers
    }

    @SuppressLint("MissingPermission")
    override suspend fun refreshLastKnownLocation() {
        val permissionLevel = permissionReader.detectPermissionLevel()
        setPermissionLevel(permissionLevel)

        if (permissionLevel == LocationPermissionLevel.None) return

        try {
            fusedLocationClient.lastLocation.await()?.let(::onLocationReceived)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            updateError("Could not read last known location", throwable)
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(config: LocationConfig) {
        val permissionLevel = permissionReader.detectPermissionLevel()
        setPermissionLevel(permissionLevel)

        if (permissionLevel == LocationPermissionLevel.None) return

        scope.launch {
            val providers = refreshProviderState()
            if (!providers.isUsable) return@launch

            stopInternal(updateState = false)

            _trackingState.update {
                it.copy(
                    status = LocationStatus.Starting,
                    isTracking = true,
                    lastErrorMessage = null,
                )
            }

            val newCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let(::onLocationReceived)
                }
            }

            callback = newCallback
            requestLocationUpdates(config, newCallback)
        }
    }

    override fun stop() {
        stopInternal(updateState = true)
    }

    override fun close() {
        stopInternal(updateState = false)
        providerChangeMonitor.close()
        scope.cancel()
    }

    private fun requestLocationUpdates(config: LocationConfig, newCallback: LocationCallback) {
        runCatching {
            fusedLocationClient
                .requestLocationUpdates(
                    config.toLocationRequest(),
                    newCallback,
                    Looper.getMainLooper(),
                )
                .addOnFailureListener { throwable ->
                    onStartFailure(newCallback, throwable)
                }
        }.onFailure { throwable ->
            onStartFailure(newCallback, throwable)
        }
    }

    private fun stopInternal(updateState: Boolean) {
        callback?.let { fusedLocationClient.removeLocationUpdates(it) }
        callback = null

        if (updateState) {
            _trackingState.update {
                it.copy(
                    status = LocationStatus.Stopped,
                    isTracking = false,
                )
            }
        }
    }

    private fun onLocationReceived(location: Location) {
        val deviceLocation = location.toDeviceLocation()

        _trackingState.update { state ->
            state.copy(
                status = LocationStatus.Active,
                location = deviceLocation,
                isTracking = true,
                lastErrorMessage = null,
            )
        }
    }

    private fun onStartFailure(failedCallback: LocationCallback, throwable: Throwable) {
        if (callback === failedCallback) {
            fusedLocationClient.removeLocationUpdates(failedCallback)
            callback = null
        }

        updateError("Could not start location updates", throwable)
    }

    private fun updateError(prefix: String, throwable: Throwable) {
        val message = throwable.message?.let { "$prefix: $it" } ?: prefix

        _trackingState.update {
            it.copy(
                status = LocationStatus.Error(message),
                isTracking = false,
                lastErrorMessage = message,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun Location.toDeviceLocation(): DeviceLocation = DeviceLocation(
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
}
