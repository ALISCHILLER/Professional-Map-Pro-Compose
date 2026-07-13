package com.msa.professionalmap.core.service.data
import android.content.Context
import com.msa.professionalmap.core.location.AndroidFusedLocationRepository
import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.location.LocationPermissionLevel
import com.msa.professionalmap.core.location.LocationRepository
import com.msa.professionalmap.core.location.LocationStatus
import com.msa.professionalmap.core.progress.data.DefaultRouteProgressRepository
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteLocationSample
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig
import com.msa.professionalmap.core.service.data.NavigationProgressPresentation.progressOrNull
import com.msa.professionalmap.core.service.domain.NavigationRuntimeErrorCode
import com.msa.professionalmap.core.service.domain.NavigationRuntimeState
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import com.msa.professionalmap.core.service.domain.NavigationSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
internal class NavigationRuntimeEngine(
    context: Context,
    private val scope: CoroutineScope,
    private val sessionStore: EncryptedNavigationSessionStore,
    private val wakeLockManager: WakeLockManager,
    private val detectPermissionLevel: () -> LocationPermissionLevel,
    private val onStateChanged: (NavigationRuntimeState) -> Unit,
    private val onProgressChanged: (ProgressState, DeviceLocation) -> Unit,
    private val onCompleted: () -> Unit,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val progressConfig = RouteProgressConfig()
    private var activeSession: NavigationSession? = null; private var locationRepository: LocationRepository? = null
    private val routeClient = NavigationRouteClient()
    private val progressRepository = DefaultRouteProgressRepository(progressConfig)
    private val reroutePolicy = NavigationReroutePolicy(progressConfig)
    private var locationJob: Job? = null; private var rerouteJob: Job? = null
    fun startOrRestore(): NavigationSession? {
        val session = sessionStore.read()
        if (session == null) {
            fail(NavigationRuntimeErrorCode.SessionUnavailable)
            return null
        }
        if (detectPermissionLevel() == LocationPermissionLevel.None) {
            fail(NavigationRuntimeErrorCode.MissingLocationPermission, session)
            return null
        }
        return try {
            activeSession = session
            routeClient.configure(session)
            publish(
                NavigationRuntimeState(
                    status = NavigationServiceStatus.Active,
                    session = session,
                    snapshot = NavigationProgressPresentation.baseSnapshot(session, NavigationServiceStatus.Active),
                    remainingRoutePoints = session.route.points,
                )
            )
            startLocationTracking(session)
            session
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            fail(NavigationRuntimeErrorCode.UnexpectedFailure, session)
            null
        }
    }
    fun pause(): Boolean {
        val session = activeSession ?: sessionStore.read()
        if (session == null) {
            fail(NavigationRuntimeErrorCode.SessionUnavailable)
            return false
        }
        rerouteJob?.cancel()
        rerouteJob = null
        reroutePolicy.reset()
        progressRepository.reset()
        locationJob?.cancel()
        locationJob = null
        locationRepository?.stop()
        wakeLockManager.release()
        publish(NavigationRuntimeRegistry.state.value.copy(
                status = NavigationServiceStatus.Paused,
                session = session,
                snapshot = NavigationRuntimeRegistry.state.value.snapshot.copy(
                    status = NavigationServiceStatus.Paused,
                    nextInstructionText = NavigationNotificationText.pausedInstruction(session.languageTag),
                    lastUpdatedAtMillis = System.currentTimeMillis(),
                ),
            ))
        return true
    }
    fun resume() {
        val session = activeSession ?: sessionStore.read()
        if (session == null) {
            fail(NavigationRuntimeErrorCode.SessionUnavailable)
            return
        }
        try {
            activeSession = session
            routeClient.configure(session)
            publish(
                NavigationRuntimeRegistry.state.value.copy(
                    status = NavigationServiceStatus.Active,
                    session = session,
                    snapshot = NavigationRuntimeRegistry.state.value.snapshot.copy(
                        status = NavigationServiceStatus.Active,
                        nextInstructionText = NavigationNotificationText.fallbackInstruction(session.languageTag),
                        lastUpdatedAtMillis = System.currentTimeMillis(),
                    ),
                    errorCode = null,
                )
            )
            startLocationTracking(session)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            fail(NavigationRuntimeErrorCode.UnexpectedFailure, session)
        }
    }
    fun stop(clearPersistedSession: Boolean) {
        rerouteJob?.cancel()
        locationJob?.cancel()
        locationRepository?.close()
        locationRepository = null
        routeClient.close()
        wakeLockManager.release()
        if (clearPersistedSession) sessionStore.clear()
        activeSession = null
        reroutePolicy.reset()
        progressRepository.reset()
    }
    override fun close() = stop(clearPersistedSession = false)
    private fun startLocationTracking(session: NavigationSession) {
        wakeLockManager.acquire()
        locationJob?.cancel()
        locationRepository?.close()
        val repository = AndroidFusedLocationRepository(appContext)
        locationRepository = repository
        repository.setPermissionLevel(detectPermissionLevel())
        locationJob = scope.launch {
            repository.trackingState.collectLatest { tracking ->
                val currentLocation = tracking.location
                when {
                    currentLocation != null -> onLocation(currentLocation)
                    tracking.status is LocationStatus.PermissionRequired -> {
                        fail(NavigationRuntimeErrorCode.MissingLocationPermission, session)
                    }
                    tracking.status is LocationStatus.ProviderDisabled || tracking.status is LocationStatus.Error -> {
                        publish(NavigationRuntimeRegistry.state.value.copy(
                            errorCode = NavigationRuntimeErrorCode.LocationProviderUnavailable,
                        ))
                    }
                }
            }
        }
        repository.start(session.locationConfig)
    }
    private fun onLocation(location: DeviceLocation) {
        val session = activeSession ?: return
        if (NavigationRuntimeRegistry.state.value.status == NavigationServiceStatus.Paused) return
        wakeLockManager.renew()
        val progressState = progressRepository.calculateProgress(
            route = session.route,
            sample = RouteLocationSample(
                position = location.position,
                accuracyMeters = location.accuracyMeters?.toDouble(),
                bearingDegrees = location.bearingDegrees?.toDouble(),
                speedMetersPerSecond = location.speedMetersPerSecond?.toDouble(),
            ),
            timestampMillis = location.timestampMillis,
        )
        val progress = progressState.progressOrNull()
        val split = NavigationProgressPresentation.splitRoute(session.route, progress)
        val status = if (progressState is ProgressState.Arrived) {
            NavigationServiceStatus.Completed
        } else {
            NavigationServiceStatus.Active
        }
        publish(
            NavigationRuntimeState(
                status = status,
                session = session,
                snapshot = NavigationProgressPresentation.snapshotFor(session, progressState).copy(status = status),
                currentLocation = location,
                progressState = progressState,
                completedRoutePoints = split.first,
                remainingRoutePoints = split.second,
                snappedLocation = progress?.matchedLocation?.snappedLocation,
            )
        )
        onProgressChanged(progressState, location)
        when (progressState) {
            is ProgressState.Arrived -> complete(session)
            is ProgressState.OffRoute -> scheduleReroute(location)
            else -> {
                reroutePolicy.onRouteRecovered()
                rerouteJob?.cancel()
            }
        }
    }
    private fun scheduleReroute(location: DeviceLocation) {
        val delayMillis = reroutePolicy.delayBeforeAttempt(System.currentTimeMillis()) ?: return
        if (rerouteJob?.isActive == true) return
        rerouteJob = scope.launch {
            delay(delayMillis)
            val runtime = NavigationRuntimeRegistry.state.value
            if (runtime.status == NavigationServiceStatus.Active && runtime.progressState is ProgressState.OffRoute) {
                reroutePolicy.markAttempt(System.currentTimeMillis())
                reroute(runtime.currentLocation ?: location)
            }
        }
    }
    private suspend fun reroute(location: DeviceLocation) {
        val session = activeSession ?: return
        publish(NavigationRuntimeRegistry.state.value.copy(
            status = NavigationServiceStatus.Rerouting,
            progressState = ProgressState.Rerouting(
                NavigationRuntimeRegistry.state.value.progressState.progressOrNull()
            ),
            snapshot = NavigationRuntimeRegistry.state.value.snapshot.copy(
                status = NavigationServiceStatus.Rerouting,
                nextInstructionText = NavigationNotificationText.reroutingInstruction(session.languageTag),
            ),
        ))
        try {
            val route = routeClient.calculateReroute(session, location)
            val updated = session.copy(route = route)
            sessionStore.save(updated)
            activeSession = updated
            reroutePolicy.onRerouteSucceeded()
            progressRepository.reset()
            publish(NavigationRuntimeRegistry.state.value.copy(
                status = NavigationServiceStatus.Active,
                session = updated,
                progressState = ProgressState.Idle,
                completedRoutePoints = emptyList(),
                remainingRoutePoints = route.points,
                snappedLocation = null,
                errorCode = null,
            ))
            onLocation(location)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            publish(NavigationRuntimeRegistry.state.value.copy(
                status = NavigationServiceStatus.Active,
                errorCode = NavigationRuntimeErrorCode.RoutingFailed,
                snapshot = NavigationRuntimeRegistry.state.value.snapshot.copy(
                    status = NavigationServiceStatus.Active,
                    nextInstructionText = NavigationNotificationText.rerouteFailedInstruction(session.languageTag),
                ),
            ))
        }
    }
    private fun complete(session: NavigationSession) {
        sessionStore.clear()
        rerouteJob?.cancel()
        rerouteJob = null
        locationJob?.cancel()
        locationJob = null
        locationRepository?.stop()
        wakeLockManager.release()
        publish(NavigationRuntimeRegistry.state.value.copy(
            status = NavigationServiceStatus.Completed,
            snapshot = NavigationRuntimeRegistry.state.value.snapshot.copy(
                status = NavigationServiceStatus.Completed,
                remainingDistanceText = NavigationProgressPresentation.formatDistance(0.0, session.languageTag),
                remainingDurationText = NavigationProgressPresentation.formatDuration(0.0, session.languageTag),
                nextInstructionText = NavigationNotificationText.arrivedInstruction(session.languageTag),
            ),
        ))
        onCompleted()
    }
    private fun fail(code: NavigationRuntimeErrorCode, session: NavigationSession? = activeSession) {
        val snapshot = NavigationProgressPresentation.baseSnapshot(session, NavigationServiceStatus.Failed).copy(
            nextInstructionText = NavigationNotificationText.failureInstruction(session?.languageTag ?: "en"),
        )
        publish(NavigationRuntimeState(
            status = NavigationServiceStatus.Failed,
            session = session,
            snapshot = snapshot,
            errorCode = code,
        ))
        rerouteJob?.cancel()
        rerouteJob = null
        locationJob?.cancel()
        locationJob = null
        locationRepository?.close()
        locationRepository = null
        routeClient.close()
        wakeLockManager.release()
    }
    private fun publish(state: NavigationRuntimeState) {
        NavigationRuntimeRegistry.set(state)
        onStateChanged(state)
    }
}
