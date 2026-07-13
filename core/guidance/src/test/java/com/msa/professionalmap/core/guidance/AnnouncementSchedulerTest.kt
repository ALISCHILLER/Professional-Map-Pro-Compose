package com.msa.professionalmap.core.guidance

import com.msa.professionalmap.core.guidance.data.DefaultAnnouncementScheduler
import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.model.GeoPoint
import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.NextInstruction
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AnnouncementSchedulerTest {
    private val scheduler = DefaultAnnouncementScheduler()

    @Test fun announcesBucketOnlyOnce() {
        val config = GuidanceConfig(minAnnouncementIntervalMillis = 0L)
        val first = scheduler.nextAnnouncement(progress(), speedMetersPerSecond = 12.0, config = config, nowMillis = 10_000L)
        val second = scheduler.nextAnnouncement(progress(), speedMetersPerSecond = 12.0, config = config, nowMillis = 11_000L)
        assertNotNull(first)
        assertNull(second)
    }

    private fun progress(): ProgressState = ProgressState.Navigating(
        RouteProgress(
            routeId = "route-1",
            matchedLocation = MatchedRouteLocation(
                originalLocation = GeoPoint(35.0, 51.0),
                snappedLocation = GeoPoint(35.0, 51.0),
                segmentIndex = 0,
                segmentFraction = 0.0,
                distanceFromRouteMeters = 2.0,
                distanceFromStartMeters = 100.0,
            ),
            totalDistanceMeters = 1000.0,
            completedDistanceMeters = 100.0,
            remainingDistanceMeters = 900.0,
            remainingDurationSeconds = 120.0,
            progressFraction = 0.1,
            etaEpochMillis = null,
            nextInstruction = NextInstruction(
                instruction = "Turn right",
                distanceMeters = 180.0,
                roadName = null,
                maneuverType = "turn",
                maneuverModifier = "right",
                sourceInstruction = null,
            ),
            timestampMillis = 10_000L,
        )
    )
}
