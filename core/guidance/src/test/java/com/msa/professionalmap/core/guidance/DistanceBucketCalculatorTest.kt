package com.msa.professionalmap.core.guidance

import com.msa.professionalmap.core.guidance.data.DynamicDistanceBucketCalculator
import com.msa.professionalmap.core.guidance.domain.RoadContext
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceBucketCalculatorTest {
    private val calculator = DynamicDistanceBucketCalculator()

    @Test fun highwaySpeedUsesHighwayBuckets() {
        assertEquals(RoadContext.Highway, calculator.roadContext(28.0))
        assertEquals(1000.0, calculator.bucketFor(900.0, 28.0)?.meters ?: 0.0, 0.0)
    }

    @Test fun citySpeedUsesCityBuckets() {
        assertEquals(RoadContext.City, calculator.roadContext(12.0))
        assertEquals(200.0, calculator.bucketFor(170.0, 12.0)?.meters ?: 0.0, 0.0)
    }

    @Test fun walkingSpeedUsesWalkingBuckets() {
        assertEquals(RoadContext.Walking, calculator.roadContext(1.4))
        assertEquals(50.0, calculator.bucketFor(45.0, 1.4)?.meters ?: 0.0, 0.0)
    }
}
