package com.msa.professionalmap.core.service.data

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteOverview
import com.msa.professionalmap.core.model.RouteSnapOptions
import com.msa.professionalmap.core.model.TravelMode
import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.routing.CachedRoutingRepository
import com.msa.professionalmap.core.routing.OsrmRoutingConfig
import com.msa.professionalmap.core.routing.OsrmRoutingRepository
import com.msa.professionalmap.core.routing.RoutingRepository
import com.msa.professionalmap.core.service.domain.NavigationSession
import io.ktor.client.engine.android.Android

/** Owns the routing client lifecycle used by the foreground navigation runtime. */
internal class NavigationRouteClient : AutoCloseable {
    private var repository: RoutingRepository? = null

    fun configure(session: NavigationSession) {
        close()
        val config = OsrmRoutingConfig(
            baseUrl = session.routingConfig.baseUrl,
            userAgent = session.routingConfig.userAgent,
            allowCleartextTraffic = session.routingConfig.allowCleartextTraffic,
        )
        repository = CachedRoutingRepository(
            OsrmRoutingRepository(
                config = config,
                client = OsrmRoutingRepository.createClient(config, Android),
            )
        )
    }

    suspend fun calculateReroute(
        session: NavigationSession,
        location: DeviceLocation,
    ): RouteAlternative {
        val routingRepository = checkNotNull(repository) {
            "Navigation routing client is not configured."
        }
        return routingRepository.calculateRoute(
            RoutingRequest(
                origin = location.position,
                destination = session.destination,
                travelMode = session.travelMode,
                requestAlternatives = false,
                includeSteps = true,
                overview = RouteOverview.Full,
                snapOptions = RouteSnapOptions(
                    originBearingDegrees = location.bearingDegrees?.toDouble(),
                    originBearingToleranceDegrees = bearingTolerance(location),
                    originRadiusMeters = snapRadius(location),
                    destinationRadiusMeters = DestinationSnapRadiusMeters,
                    continueStraight = session.travelMode == TravelMode.Driving,
                ),
            )
        ).primaryRoute
    }

    private fun snapRadius(location: DeviceLocation): Double =
        ((location.accuracyMeters?.toDouble() ?: DefaultAccuracyMeters) * AccuracyRadiusMultiplier)
            .coerceIn(MinimumSnapRadiusMeters, MaximumSnapRadiusMeters)

    private fun bearingTolerance(location: DeviceLocation): Int = when {
        location.bearingDegrees == null -> DefaultBearingToleranceDegrees
        (location.speedMetersPerSecond ?: 0f) >= ReliableBearingSpeedMetersPerSecond -> 35
        else -> DefaultBearingToleranceDegrees
    }

    override fun close() {
        repository?.close()
        repository = null
    }

    private companion object {
        const val DefaultAccuracyMeters = 18.0
        const val AccuracyRadiusMultiplier = 1.8
        const val MinimumSnapRadiusMeters = 20.0
        const val MaximumSnapRadiusMeters = 90.0
        const val DestinationSnapRadiusMeters = 45.0
        const val DefaultBearingToleranceDegrees = 65
        const val ReliableBearingSpeedMetersPerSecond = 2.0f
    }
}
