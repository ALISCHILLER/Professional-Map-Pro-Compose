package com.msa.professionalmap.core.service.data

import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.model.RouteAlternative
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress
import com.msa.professionalmap.core.service.domain.NavigationServiceSnapshot
import com.msa.professionalmap.core.service.domain.NavigationServiceStatus
import com.msa.professionalmap.core.service.domain.NavigationSession
import java.util.Locale

internal object NavigationProgressPresentation {
    fun snapshotFor(session: NavigationSession, state: ProgressState): NavigationServiceSnapshot {
        val progress = state.progressOrNull()
        val instruction = when (state) {
            is ProgressState.Arrived -> NavigationNotificationText.arrivedInstruction(session.languageTag)
            is ProgressState.OffRoute -> NavigationNotificationText.offRouteInstruction(session.languageTag)
            is ProgressState.Rerouting -> NavigationNotificationText.reroutingInstruction(session.languageTag)
            else -> progress?.nextInstruction?.instruction
                ?: NavigationNotificationText.fallbackInstruction(session.languageTag)
        }
        return NavigationServiceSnapshot(
            status = NavigationServiceStatus.Active,
            destinationTitle = session.destinationTitle,
            remainingDistanceText = formatDistance(
                progress?.remainingDistanceMeters ?: session.route.distanceMeters,
                session.languageTag,
            ),
            remainingDurationText = formatDuration(
                progress?.remainingDurationSeconds ?: session.route.durationSeconds,
                session.languageTag,
            ),
            nextInstructionText = instruction,
            destination = session.destination,
            languageTag = session.languageTag,
            lastUpdatedAtMillis = System.currentTimeMillis(),
        )
    }

    fun baseSnapshot(
        session: NavigationSession?,
        status: NavigationServiceStatus,
    ): NavigationServiceSnapshot = NavigationServiceSnapshot(
        status = status,
        destinationTitle = session?.destinationTitle ?: NavigationNotificationText.defaultDestination("en"),
        remainingDistanceText = session?.let { formatDistance(it.route.distanceMeters, it.languageTag) } ?: "--",
        remainingDurationText = session?.let { formatDuration(it.route.durationSeconds, it.languageTag) } ?: "--",
        nextInstructionText = NavigationNotificationText.fallbackInstruction(session?.languageTag ?: "en"),
        destination = session?.destination,
        languageTag = session?.languageTag ?: "en",
        lastUpdatedAtMillis = System.currentTimeMillis(),
    )

    fun splitRoute(
        route: RouteAlternative,
        progress: RouteProgress?,
    ): Pair<List<GeoPoint>, List<GeoPoint>> {
        val match = progress?.matchedLocation ?: return emptyList<GeoPoint>() to route.points
        val segmentIndex = match.segmentIndex.coerceIn(0, route.points.lastIndex - 1)
        val snapped = match.snappedLocation
        val completed = buildList {
            addAll(route.points.take(segmentIndex + 1))
            if (lastOrNull() != snapped) add(snapped)
        }
        val remaining = buildList {
            add(snapped)
            route.points.drop(segmentIndex + 1).forEach { point ->
                if (lastOrNull() != point) add(point)
            }
        }
        return completed to remaining
    }

    fun formatDistance(distanceMeters: Double, languageTag: String): String {
        val persian = languageTag.trim().lowercase().startsWith("fa")
        return if (distanceMeters >= 1_000.0) {
            val value = String.format(Locale.US, "%.1f", distanceMeters / 1_000.0)
            if (persian) "${value.toPersianDigits()} کیلومتر" else "$value km"
        } else {
            val value = distanceMeters.coerceAtLeast(0.0).toInt().toString()
            if (persian) "${value.toPersianDigits()} متر" else "$value m"
        }
    }

    fun formatDuration(durationSeconds: Double, languageTag: String): String {
        val persian = languageTag.trim().lowercase().startsWith("fa")
        val totalMinutes = (durationSeconds.coerceAtLeast(0.0) / 60.0).toInt()
        return if (totalMinutes >= 60) {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (persian) {
                "${hours.toString().toPersianDigits()} ساعت و ${minutes.toString().toPersianDigits()} دقیقه"
            } else {
                "$hours h $minutes min"
            }
        } else if (persian) {
            "${totalMinutes.toString().toPersianDigits()} دقیقه"
        } else {
            "$totalMinutes min"
        }
    }


    private fun String.toPersianDigits(): String = buildString(length) {
        this@toPersianDigits.forEach { character ->
            append(if (character in '0'..'9') PersianDigits[character - '0'] else character)
        }
    }

    private const val PersianDigits = "۰۱۲۳۴۵۶۷۸۹"
    fun ProgressState.progressOrNull(): RouteProgress? = when (this) {
        is ProgressState.Navigating -> progress
        is ProgressState.OffRoute -> progress
        is ProgressState.Rerouting -> lastKnownProgress
        is ProgressState.Arrived -> progress
        ProgressState.Idle -> null
    }
}
