package com.msa.professionalmap.core.guidance.data

import com.msa.professionalmap.core.guidance.domain.AnnouncementScheduler
import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.guidance.domain.GuidanceEvaluation
import com.msa.professionalmap.core.guidance.domain.GuidanceState
import com.msa.professionalmap.core.guidance.domain.VoiceGuidanceRepository
import com.msa.professionalmap.core.progress.domain.ProgressState

class DefaultVoiceGuidanceRepository(
    private val scheduler: AnnouncementScheduler = DefaultAnnouncementScheduler(),
    private val maxRecentAnnouncements: Int = 8,
) : VoiceGuidanceRepository {
    private var state: GuidanceState = GuidanceState()

    override fun evaluate(
        progressState: ProgressState,
        speedMetersPerSecond: Double?,
        config: GuidanceConfig,
        nowMillis: Long,
    ): GuidanceEvaluation {
        val configuredState = state.copy(
            enabled = config.enabled,
            muted = config.muted,
            language = config.language,
            volume = config.volume,
        )
        if (!config.enabled) {
            state = configuredState
            return GuidanceEvaluation(state = state, eventToSpeak = null)
        }

        val event = scheduler.nextAnnouncement(
            progressState = progressState,
            speedMetersPerSecond = speedMetersPerSecond,
            config = config,
            nowMillis = nowMillis,
        )
        state = if (event == null) {
            configuredState
        } else {
            configuredState.copy(
                lastAnnouncement = event,
                recentAnnouncements = (listOf(event) + configuredState.recentAnnouncements).take(maxRecentAnnouncements),
                lastErrorMessage = null,
            )
        }
        return GuidanceEvaluation(
            state = state,
            eventToSpeak = if (config.muted) null else event,
        )
    }

    override fun reset(config: GuidanceConfig): GuidanceState {
        scheduler.reset()
        state = GuidanceState(
            enabled = config.enabled,
            muted = config.muted,
            language = config.language,
            volume = config.volume,
        )
        return state
    }

    fun withError(message: String): GuidanceState {
        state = state.copy(lastErrorMessage = message)
        return state
    }
}
