package com.msa.professionalmap.core.routing

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteInstruction
import com.msa.professionalmap.core.model.RouteLeg
import com.msa.professionalmap.core.model.RouteOverview
import com.msa.professionalmap.core.model.RouteWaypoint
import com.msa.professionalmap.core.model.RoutingRequest
import com.msa.professionalmap.core.model.RoutingResult
import com.msa.professionalmap.core.model.TravelMode
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * OSRM-backed routing provider.
 *
 * The public contract remains provider-neutral via [RoutingRepository]; OSRM request/response
 * details stay isolated in this adapter so Valhalla, GraphHopper or a self-hosted backend can
 * be added later without touching the UI.
 */
class OsrmRoutingRepository(
    private val config: OsrmRoutingConfig,
    private val client: HttpClient,
) : RoutingRepository {

    override suspend fun calculateRoute(request: RoutingRequest): RoutingResult {
        val response = try {
            client.get(buildRouteUrl(request)) {
                parameter("alternatives", request.requestAlternatives.toString())
                parameter("steps", request.includeSteps.toString())
                parameter("geometries", "geojson")
                parameter("overview", request.overview.toOsrmValue())
                OsrmRouteQueryEncoder.bearings(request)?.let { parameter("bearings", it) }
                OsrmRouteQueryEncoder.radiuses(request)?.let { parameter("radiuses", it) }
                request.snapOptions.continueStraight?.let {
                    parameter("continue_straight", it.toString())
                }
            }.body<OsrmRouteResponse>()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (timeout: HttpRequestTimeoutException) {
            throw RoutingException(RoutingErrorCode.Timeout, PROVIDER_NAME)
        } catch (_: Throwable) {
            // Never propagate the Ktor/provider message. It can contain the full request URL,
            // and OSRM embeds precise coordinates in that URL path.
            throw RoutingException(RoutingErrorCode.NetworkUnavailable, PROVIDER_NAME)
        }

        if (response.code != "Ok") {
            throw RoutingException(RoutingErrorCode.ProviderRejected, PROVIDER_NAME)
        }

        val routes = response.routes.mapIndexedNotNull { index, route -> route.toDomain(index) }
        if (routes.isEmpty()) {
            throw RoutingException(RoutingErrorCode.EmptyResponse, PROVIDER_NAME)
        }

        return RoutingResult(
            provider = PROVIDER_NAME,
            routes = routes,
            waypoints = response.waypoints.mapNotNull { it.toDomain() },
        )
    }

    override fun close() {
        client.close()
    }

    private fun buildRouteUrl(request: RoutingRequest): String {
        val coordinates = request.allWaypoints.joinToString(separator = ";") { point ->
            "${point.longitude},${point.latitude}"
        }
        val profile = request.travelMode.toOsrmProfile()
        return "${config.normalizedBaseUrl}/route/v1/$profile/$coordinates"
    }

    private fun OsrmRoute.toDomain(index: Int): RouteAlternative? {
        val points = geometry?.coordinates.toGeoPoints()
        if (points.size < 2) return null
        val legs = legs.map { it.toDomain() }
        val summary = legs.firstOrNull { it.summary.isNotBlank() }?.summary.orEmpty()
        return RouteAlternative(
            id = "osrm-$index-${stableRouteFingerprint(points)}",
            title = if (index == 0) "Recommended" else "Alternative ${index + 1}",
            summary = summary.ifBlank { "${distance.toKilometersText()} km · ${duration.toMinutesText()} min" },
            points = points,
            distanceMeters = distance,
            durationSeconds = duration,
            legs = legs,
            provider = PROVIDER_NAME,
        )
    }

    private fun OsrmLeg.toDomain(): RouteLeg = RouteLeg(
        summary = summary,
        distanceMeters = distance,
        durationSeconds = duration,
        steps = steps.map { it.toDomain() },
    )

    private fun OsrmStep.toDomain(): RouteInstruction = RouteInstruction(
        instruction = buildInstruction(),
        roadName = name.takeIf { it.isNotBlank() },
        maneuverType = maneuver?.type,
        maneuverModifier = maneuver?.modifier,
        location = maneuver?.location.toGeoPointOrNull(),
        distanceMeters = distance,
        durationSeconds = duration,
        geometry = geometry?.coordinates.toGeoPoints(),
    )

    private fun OsrmStep.buildInstruction(): String {
        val type = maneuver?.type.orEmpty().replace('-', ' ')
        val modifier = maneuver?.modifier.orEmpty().replace('-', ' ')
        val road = name.takeIf { it.isNotBlank() }
        return buildString {
            when {
                type.isBlank() -> append("Continue")
                type == "depart" -> append("Depart")
                type == "arrive" -> append("Arrive")
                modifier.isNotBlank() -> append(type.replaceFirstChar { it.uppercase() }).append(' ').append(modifier)
                else -> append(type.replaceFirstChar { it.uppercase() })
            }
            if (road != null && type != "arrive") append(" onto ").append(road)
        }
    }

    private fun OsrmWaypoint.toDomain(): RouteWaypoint? {
        val position = location.toGeoPointOrNull() ?: return null
        return RouteWaypoint(
            name = name,
            snappedPosition = position,
            distanceMeters = distance,
        )
    }

    private fun List<List<Double>>?.toGeoPoints(): List<GeoPoint> = this.orEmpty().mapNotNull { it.toGeoPointOrNull() }

    private fun List<Double>?.toGeoPointOrNull(): GeoPoint? {
        if (this == null || size < 2) return null
        val lon = this[0]
        val lat = this[1]
        return runCatching { GeoPoint(latitude = lat, longitude = lon) }.getOrNull()
    }

    private fun TravelMode.toOsrmProfile(): String = when (this) {
        TravelMode.Driving -> "driving"
        TravelMode.Walking -> "walking"
        TravelMode.Cycling -> "cycling"
    }

    private fun RouteOverview.toOsrmValue(): String = when (this) {
        RouteOverview.Full -> "full"
        RouteOverview.Simplified -> "simplified"
        RouteOverview.None -> "false"
    }


    private fun stableRouteFingerprint(points: List<GeoPoint>): String {
        var hash = 17L
        points.forEach { point ->
            hash = 31L * hash + point.latitude.toBits()
            hash = 31L * hash + point.longitude.toBits()
        }
        return hash.toULong().toString(16)
    }

    private fun Double.toKilometersText(): String = String.format(Locale.US, "%.1f", this / 1000.0)
    private fun Double.toMinutesText(): String = String.format(Locale.US, "%.0f", this / 60.0)

    companion object {
        private const val PROVIDER_NAME = "osrm"

        fun <T : HttpClientEngineConfig> createClient(
            config: OsrmRoutingConfig,
            engineFactory: HttpClientEngineFactory<T>,
        ): HttpClient = HttpClient(engineFactory) {
            install(UserAgent) { agent = config.userAgent }
            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeoutMillis
                connectTimeoutMillis = config.requestTimeoutMillis
                socketTimeoutMillis = config.requestTimeoutMillis
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    }
                )
            }
        }
    }
}
