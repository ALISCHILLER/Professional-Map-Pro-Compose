package com.msa.professionalmap.core.progress.data

import com.msa.professionalmap.core.progress.domain.MatchedRouteLocation
import com.msa.professionalmap.core.progress.domain.OffRouteDetector
import com.msa.professionalmap.core.progress.domain.RouteProgressConfig

class DistanceOffRouteDetector : OffRouteDetector {
    override fun isOffRoute(match: MatchedRouteLocation, config: RouteProgressConfig): Boolean {
        return match.distanceFromRouteMeters > config.offRouteThresholdMeters
    }
}
