package com.msa.professionalmap.core.model

/**
 * Provider-neutral routing models used by UI, routing providers and future navigation modules.
 */
enum class TravelMode {
    Driving,
    Walking,
    Cycling,
}

enum class RouteOverview {
    Full,
    Simplified,
    None,
}

/**
 * Declares whether a route is safe to use for turn-by-turn navigation.
 *
 * Provider-calculated routes are usually navigable because they are matched to the road network.
 * Locally synthesized previews, such as straight-line fallback routes, must stay preview-only so
 * the UI can show a visual aid without accidentally starting unsafe navigation guidance.
 */
enum class RouteNavigationPolicy {
    Navigable,
    PreviewOnly,
}


data class RouteSnapOptions(
    val originBearingDegrees: Double? = null,
    val originBearingToleranceDegrees: Int = 50,
    val originRadiusMeters: Double? = null,
    val destinationRadiusMeters: Double? = null,
    val continueStraight: Boolean? = null,
) {
    init {
        require(originBearingDegrees == null || originBearingDegrees.isFinite()) {
            "originBearingDegrees must be null or finite."
        }
        require(originBearingToleranceDegrees in 0..180) {
            "originBearingToleranceDegrees must be in 0..180."
        }
        require(originRadiusMeters == null || originRadiusMeters > 0.0) {
            "originRadiusMeters must be null or positive."
        }
        require(destinationRadiusMeters == null || destinationRadiusMeters > 0.0) {
            "destinationRadiusMeters must be null or positive."
        }
    }

    val normalizedOriginBearing: Double?
        get() = originBearingDegrees?.let { ((it % 360.0) + 360.0) % 360.0 }
}

data class RoutingRequest(
    val origin: GeoPoint,
    val destination: GeoPoint,
    val intermediateWaypoints: List<GeoPoint> = emptyList(),
    val travelMode: TravelMode = TravelMode.Driving,
    val requestAlternatives: Boolean = true,
    val includeSteps: Boolean = true,
    val overview: RouteOverview = RouteOverview.Full,
    val snapOptions: RouteSnapOptions = RouteSnapOptions(),
) {
    init {
        require(intermediateWaypoints.size <= 23) {
            "Keep route requests small. Move provider-specific large waypoint limits into routing providers."
        }
    }

    val allWaypoints: List<GeoPoint>
        get() = buildList {
            add(origin)
            addAll(intermediateWaypoints)
            add(destination)
        }
}

data class RoutingResult(
    val routes: List<RouteAlternative>,
    val waypoints: List<RouteWaypoint> = emptyList(),
    val provider: String,
) {
    init {
        require(routes.isNotEmpty()) { "RoutingResult must contain at least one route." }
    }

    val primaryRoute: RouteAlternative get() = routes.first()
}

data class RouteAlternative(
    val id: String,
    val title: String,
    val summary: String,
    val points: List<GeoPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val legs: List<RouteLeg> = emptyList(),
    val provider: String,
    val navigationPolicy: RouteNavigationPolicy = RouteNavigationPolicy.Navigable,
) {
    init {
        require(points.size >= 2) { "A route alternative must contain at least two geometry points." }
        require(distanceMeters >= 0.0) { "distanceMeters must be non-negative." }
        require(durationSeconds >= 0.0) { "durationSeconds must be non-negative." }
    }

    val distanceKm: Double get() = distanceMeters / 1000.0
    val durationMinutes: Double get() = durationSeconds / 60.0
    val isNavigationEligible: Boolean get() = navigationPolicy == RouteNavigationPolicy.Navigable
}

data class RouteLeg(
    val summary: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<RouteInstruction> = emptyList(),
)

data class RouteInstruction(
    val instruction: String,
    val roadName: String?,
    val maneuverType: String?,
    val maneuverModifier: String?,
    val location: GeoPoint?,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val geometry: List<GeoPoint> = emptyList(),
)

data class RouteWaypoint(
    val name: String,
    val snappedPosition: GeoPoint,
    val distanceMeters: Double?,
)
