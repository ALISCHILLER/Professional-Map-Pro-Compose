package com.msa.professionalmap.core.service.data

import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.location.LocationConfig
import com.msa.professionalmap.core.location.LocationPriority
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.model.RouteInstruction
import com.msa.professionalmap.core.model.RouteLeg
import com.msa.professionalmap.core.model.RouteNavigationPolicy
import com.msa.professionalmap.core.model.TravelMode
import com.msa.professionalmap.core.service.domain.NavigationRoutingConfig
import com.msa.professionalmap.core.service.domain.NavigationSession
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/** JSON codec kept separate from encrypted storage and Android Keystore concerns. */
internal object NavigationSessionJsonCodec {
    fun encode(session: NavigationSession): ByteArray =
        session.toJson().toString().toByteArray(StandardCharsets.UTF_8)

    fun decode(bytes: ByteArray): NavigationSession =
        JSONObject(String(bytes, StandardCharsets.UTF_8)).toNavigationSession()

    private fun NavigationSession.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("route", route.toJson())
        .put("destinationTitle", destinationTitle)
        .put("languageTag", languageTag)
        .put("guidanceConfig", guidanceConfig.toJson())
        .put("travelMode", travelMode.name)
        .put("routingConfig", routingConfig.toJson())
        .put("locationConfig", locationConfig.toJson())
        .put("startedAtMillis", startedAtMillis)

    private fun JSONObject.toNavigationSession(): NavigationSession = NavigationSession(
        id = getString("id"),
        route = getJSONObject("route").toRouteAlternative(),
        destinationTitle = getString("destinationTitle"),
        languageTag = getString("languageTag"),
        guidanceConfig = optJSONObject("guidanceConfig")?.toGuidanceConfig() ?: GuidanceConfig(),
        travelMode = enumValueOrDefault(optString("travelMode"), TravelMode.Driving),
        routingConfig = getJSONObject("routingConfig").toNavigationRoutingConfig(),
        locationConfig = getJSONObject("locationConfig").toLocationConfig(),
        startedAtMillis = optLong("startedAtMillis", System.currentTimeMillis()),
    )

    private fun RouteAlternative.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("summary", summary)
        .put("points", points.toJson())
        .put("distanceMeters", distanceMeters)
        .put("durationSeconds", durationSeconds)
        .put("legs", JSONArray().apply { legs.forEach { put(it.toJson()) } })
        .put("provider", provider)
        .put("navigationPolicy", navigationPolicy.name)

    private fun JSONObject.toRouteAlternative(): RouteAlternative = RouteAlternative(
        id = getString("id"),
        title = optString("title", "Route"),
        summary = optString("summary", ""),
        points = getJSONArray("points").toGeoPoints(),
        distanceMeters = getDouble("distanceMeters"),
        durationSeconds = getDouble("durationSeconds"),
        legs = optJSONArray("legs")?.toRouteLegs().orEmpty(),
        provider = optString("provider", "routing"),
        navigationPolicy = enumValueOrDefault(
            optString("navigationPolicy"),
            RouteNavigationPolicy.Navigable,
        ),
    )

    private fun RouteLeg.toJson(): JSONObject = JSONObject()
        .put("summary", summary)
        .put("distanceMeters", distanceMeters)
        .put("durationSeconds", durationSeconds)
        .put("steps", JSONArray().apply { steps.forEach { put(it.toJson()) } })

    private fun JSONArray.toRouteLegs(): List<RouteLeg> = buildList {
        for (index in 0 until length()) {
            val value = optJSONObject(index) ?: continue
            add(
                RouteLeg(
                    summary = value.optString("summary", ""),
                    distanceMeters = value.optDouble("distanceMeters", 0.0),
                    durationSeconds = value.optDouble("durationSeconds", 0.0),
                    steps = value.optJSONArray("steps")?.toInstructions().orEmpty(),
                )
            )
        }
    }

    private fun RouteInstruction.toJson(): JSONObject = JSONObject()
        .put("instruction", instruction)
        .put("roadName", roadName)
        .put("maneuverType", maneuverType)
        .put("maneuverModifier", maneuverModifier)
        .put("location", location?.toJson())
        .put("distanceMeters", distanceMeters)
        .put("durationSeconds", durationSeconds)
        .put("geometry", geometry.toJson())

    private fun JSONArray.toInstructions(): List<RouteInstruction> = buildList {
        for (index in 0 until length()) {
            val value = optJSONObject(index) ?: continue
            add(
                RouteInstruction(
                    instruction = value.optString("instruction", "Continue"),
                    roadName = value.optNullableString("roadName"),
                    maneuverType = value.optNullableString("maneuverType"),
                    maneuverModifier = value.optNullableString("maneuverModifier"),
                    location = value.optJSONObject("location")?.toGeoPoint(),
                    distanceMeters = value.optDouble("distanceMeters", 0.0),
                    durationSeconds = value.optDouble("durationSeconds", 0.0),
                    geometry = value.optJSONArray("geometry")?.toGeoPoints().orEmpty(),
                )
            )
        }
    }

    private fun GuidanceConfig.toJson(): JSONObject = JSONObject()
        .put("enabled", enabled)
        .put("muted", muted)
        .put("language", language.name)
        .put("volume", volume.toDouble())
        .put("minAnnouncementIntervalMillis", minAnnouncementIntervalMillis)
        .put("offRouteRepeatIntervalMillis", offRouteRepeatIntervalMillis)
        .put("announceArrival", announceArrival)

    private fun JSONObject.toGuidanceConfig(): GuidanceConfig = GuidanceConfig(
        enabled = optBoolean("enabled", true),
        muted = optBoolean("muted", false),
        language = enumValueOrDefault(optString("language"), GuidanceLanguage.English),
        volume = optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f),
        minAnnouncementIntervalMillis = optLong("minAnnouncementIntervalMillis", 5_000L).coerceAtLeast(0L),
        offRouteRepeatIntervalMillis = optLong("offRouteRepeatIntervalMillis", 15_000L).coerceAtLeast(0L),
        announceArrival = optBoolean("announceArrival", true),
    )

    private fun NavigationRoutingConfig.toJson(): JSONObject = JSONObject()
        .put("baseUrl", baseUrl)
        .put("userAgent", userAgent)
        .put("allowCleartextTraffic", allowCleartextTraffic)

    private fun JSONObject.toNavigationRoutingConfig(): NavigationRoutingConfig = NavigationRoutingConfig(
        baseUrl = getString("baseUrl"),
        userAgent = getString("userAgent"),
        allowCleartextTraffic = optBoolean("allowCleartextTraffic", false),
    )

    private fun LocationConfig.toJson(): JSONObject = JSONObject()
        .put("intervalMillis", intervalMillis)
        .put("minUpdateIntervalMillis", minUpdateIntervalMillis)
        .put("minDistanceMeters", minDistanceMeters.toDouble())
        .put("priority", priority.name)
        .put("waitForAccurateLocation", waitForAccurateLocation)

    private fun JSONObject.toLocationConfig(): LocationConfig = LocationConfig(
        intervalMillis = optLong("intervalMillis", 2_000L),
        minUpdateIntervalMillis = optLong("minUpdateIntervalMillis", 1_000L),
        minDistanceMeters = optDouble("minDistanceMeters", 2.0).toFloat(),
        priority = enumValueOrDefault(optString("priority"), LocationPriority.HighAccuracy),
        waitForAccurateLocation = optBoolean("waitForAccurateLocation", false),
    )

    private fun List<GeoPoint>.toJson(): JSONArray = JSONArray().apply {
        forEach { put(it.toJson()) }
    }

    private fun GeoPoint.toJson(): JSONObject = JSONObject()
        .put("latitude", latitude)
        .put("longitude", longitude)

    private fun JSONArray.toGeoPoints(): List<GeoPoint> = buildList {
        for (index in 0 until length()) optJSONObject(index)?.toGeoPoint()?.let(::add)
    }

    private fun JSONObject.toGeoPoint(): GeoPoint = GeoPoint(
        latitude = getDouble("latitude"),
        longitude = getDouble("longitude"),
    )

    private fun JSONObject.optNullableString(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default
}
