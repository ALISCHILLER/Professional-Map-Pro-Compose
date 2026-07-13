package com.msa.professionalmap.core.offline.domain

import com.msa.professionalmap.core.model.GeoBounds
import com.msa.professionalmap.core.model.GeoPoint
import kotlin.math.abs
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.math.max

/**
 * Builds storage-conscious offline region requests from a route.
 *
 * This keeps route-to-offline policy out of the ViewModel and makes it testable.
 */
class OfflineRegionPlanner(
    private val defaultPaddingDegrees: Double = 0.01,
    private val defaultBufferRatio: Double = 0.10,
) {
    fun createRoutePackRequest(
        routePoints: List<GeoPoint>,
        styleId: String,
        styleTitle: String,
        styleUrl: String,
        pixelRatio: Float = 2.0f,
        includeIdeographs: Boolean = false,
    ): OfflineRegionRequest {
        require(routePoints.size >= 2) { "At least two route points are required." }
        val bounds = GeoBounds.from(routePoints, paddingDegrees = defaultPaddingDegrees)
            .expandedByRatio(defaultBufferRatio)
        val zoom = chooseZoomRange(routePoints, bounds)
        val clientId = buildClientId(styleId, routePoints)
        return OfflineRegionRequest(
            clientId = clientId,
            title = "$styleTitle route pack",
            styleUrl = styleUrl,
            bounds = bounds,
            minZoom = zoom.first.toDouble(),
            maxZoom = zoom.last.toDouble(),
            pixelRatio = pixelRatio,
            includeIdeographs = includeIdeographs,
        )
    }

    fun chooseZoomRange(routePoints: List<GeoPoint>, bounds: GeoBounds): IntRange {
        val spanLat = abs(bounds.northEast.latitude - bounds.southWest.latitude)
        val spanLon = abs(bounds.northEast.longitude - bounds.southWest.longitude)
        val maxSpan = max(spanLat, spanLon)
        val pointCount = routePoints.size
        return when {
            maxSpan > 2.5 -> 5..12
            maxSpan > 1.0 -> 7..14
            maxSpan > 0.25 -> 9..16
            pointCount > 600 -> 10..17
            else -> 10..18
        }
    }

    private fun buildClientId(styleId: String, routePoints: List<GeoPoint>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(styleId.encodeToByteArray())
        digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(routePoints.size).array())
        routePoints.forEach { point ->
            digest.update(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(point.latitude.toBits()).array())
            digest.update(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(point.longitude.toBits()).array())
        }
        val fingerprint = digest.digest().take(FingerprintBytes).joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
        return "route-${styleId.sanitizeIdPart()}-$fingerprint"
    }

    private fun String.sanitizeIdPart(): String = lowercase()
        .map { if (it.isLetterOrDigit()) it else '-' }
        .joinToString("")
        .trim('-')
        .ifBlank { "style" }

    private companion object {
        const val FingerprintBytes = 8
    }
}
