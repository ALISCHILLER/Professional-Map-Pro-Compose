package com.msa.professionalmap.feature.map.domain

import com.msa.professionalmap.core.location.DeviceLocation
import com.msa.professionalmap.core.progress.domain.ProgressState

/**
 * Prevents false arrival events caused by GPS jumps or high-speed pass-bys near the destination.
 *
 * The progress engine can report `Arrived` as soon as the distance threshold is met. Production
 * navigation needs a second confirmation gate: the user should be close to the destination, moving
 * slowly, and remain there for a short period. This stateful guard owns that policy so the
 * ViewModel does not have to manage timing details directly.
 */
class ArrivalConfirmationGuard(
    private val config: MapFeatureConfig,
) {
    private var firstCandidateTimestampMillis: Long? = null

    fun confirm(state: ProgressState, location: DeviceLocation): ProgressState {
        if (state !is ProgressState.Arrived) {
            reset()
            return state
        }
        val speed = location.speedMetersPerSecond?.toDouble() ?: 0.0
        if (speed > config.arrivalSpeedThresholdMetersPerSecond) {
            reset()
            return ProgressState.Navigating(state.progress)
        }
        val firstSeen = firstCandidateTimestampMillis ?: location.timestampMillis.also {
            firstCandidateTimestampMillis = it
        }
        return if (location.timestampMillis - firstSeen >= config.arrivalConfirmationMillis) {
            state
        } else {
            ProgressState.Navigating(state.progress)
        }
    }

    fun reset() {
        firstCandidateTimestampMillis = null
    }
}
