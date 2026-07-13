package com.msa.professionalmap.core.routing

import com.msa.professionalmap.core.model.RouteSnapOptions
import com.msa.professionalmap.core.model.RoutingRequest
import java.util.Locale
import kotlin.math.roundToInt

/** Encodes optional road-snapping hints without leaking OSRM syntax into domain models. */
internal object OsrmRouteQueryEncoder {
    fun bearings(request: RoutingRequest): String? {
        val bearing = request.snapOptions.normalizedOriginBearing ?: return null
        val values = MutableList(request.allWaypoints.size) { "" }
        values[0] = "${bearing.roundToInt()},${request.snapOptions.originBearingToleranceDegrees}"
        return values.joinToString(";")
    }

    fun radiuses(request: RoutingRequest): String? {
        val options = request.snapOptions
        if (options.originRadiusMeters == null && options.destinationRadiusMeters == null) return null
        val values = MutableList(request.allWaypoints.size) { "" }
        options.originRadiusMeters?.let { values[0] = it.metersText() }
        options.destinationRadiusMeters?.let { values[values.lastIndex] = it.metersText() }
        return values.joinToString(";")
    }

    private fun Double.metersText(): String = String.format(Locale.US, "%.0f", this)
}
