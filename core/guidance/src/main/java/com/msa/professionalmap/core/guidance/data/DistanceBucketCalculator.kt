package com.msa.professionalmap.core.guidance.data

import com.msa.professionalmap.core.guidance.domain.AnnouncementPriority
import com.msa.professionalmap.core.guidance.domain.DistanceBucket
import com.msa.professionalmap.core.guidance.domain.DistanceBucketCalculator
import com.msa.professionalmap.core.guidance.domain.RoadContext

class DynamicDistanceBucketCalculator : DistanceBucketCalculator {
    override fun roadContext(speedMetersPerSecond: Double?): RoadContext {
        val speed = speedMetersPerSecond?.takeIf { it.isFinite() && it >= 0.0 } ?: return RoadContext.City
        return when {
            speed >= HIGHWAY_SPEED_METERS_PER_SECOND -> RoadContext.Highway
            speed <= WALKING_SPEED_METERS_PER_SECOND -> RoadContext.Walking
            else -> RoadContext.City
        }
    }

    override fun bucketsFor(speedMetersPerSecond: Double?): List<DistanceBucket> {
        return when (roadContext(speedMetersPerSecond)) {
            RoadContext.Highway -> listOf(
                DistanceBucket(2000.0, RoadContext.Highway, AnnouncementPriority.Low),
                DistanceBucket(1000.0, RoadContext.Highway, AnnouncementPriority.Medium),
                DistanceBucket(500.0, RoadContext.Highway, AnnouncementPriority.Medium),
                DistanceBucket(200.0, RoadContext.Highway, AnnouncementPriority.High),
            )
            RoadContext.City -> listOf(
                DistanceBucket(500.0, RoadContext.City, AnnouncementPriority.Low),
                DistanceBucket(200.0, RoadContext.City, AnnouncementPriority.Medium),
                DistanceBucket(100.0, RoadContext.City, AnnouncementPriority.Medium),
                DistanceBucket(50.0, RoadContext.City, AnnouncementPriority.High),
            )
            RoadContext.Walking -> listOf(
                DistanceBucket(200.0, RoadContext.Walking, AnnouncementPriority.Low),
                DistanceBucket(100.0, RoadContext.Walking, AnnouncementPriority.Medium),
                DistanceBucket(50.0, RoadContext.Walking, AnnouncementPriority.Medium),
                DistanceBucket(10.0, RoadContext.Walking, AnnouncementPriority.High),
            )
        }
    }

    override fun bucketFor(distanceMeters: Double, speedMetersPerSecond: Double?): DistanceBucket? {
        if (!distanceMeters.isFinite() || distanceMeters < 0.0) return null
        return bucketsFor(speedMetersPerSecond)
            .sortedBy { it.meters }
            .firstOrNull { distanceMeters <= it.meters }
    }

    companion object {
        private const val HIGHWAY_SPEED_METERS_PER_SECOND = 22.0 // about 79 km/h
        private const val WALKING_SPEED_METERS_PER_SECOND = 2.5 // about 9 km/h
    }
}
