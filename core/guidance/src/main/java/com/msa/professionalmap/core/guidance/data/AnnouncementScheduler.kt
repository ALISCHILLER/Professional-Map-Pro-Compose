package com.msa.professionalmap.core.guidance.data

import com.msa.professionalmap.core.guidance.domain.AnnouncementEvent
import com.msa.professionalmap.core.guidance.domain.AnnouncementPriority
import com.msa.professionalmap.core.guidance.domain.AnnouncementScheduler
import com.msa.professionalmap.core.guidance.domain.DistanceBucketCalculator
import com.msa.professionalmap.core.guidance.domain.GuidanceConfig
import com.msa.professionalmap.core.guidance.domain.InstructionFormatter
import com.msa.professionalmap.core.progress.domain.NextInstruction
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress

class DefaultAnnouncementScheduler(
    private val bucketCalculator: DistanceBucketCalculator = DynamicDistanceBucketCalculator(),
    private val formatter: InstructionFormatter = DefaultInstructionFormatter(),
) : AnnouncementScheduler {
    private val announcedKeys = linkedSetOf<String>()
    private var lastAnnouncementTimestampMillis: Long? = null
    private var lastAnnouncementPriority: AnnouncementPriority? = null
    private var lastOffRouteAnnouncementMillis: Long? = null

    override fun nextAnnouncement(
        progressState: ProgressState,
        speedMetersPerSecond: Double?,
        config: GuidanceConfig,
        nowMillis: Long,
    ): AnnouncementEvent? {
        if (!config.enabled) return null

        return when (progressState) {
            ProgressState.Idle -> null
            is ProgressState.Rerouting -> null
            is ProgressState.Arrived -> arrivalAnnouncement(progressState.progress, config, nowMillis)
            is ProgressState.OffRoute -> offRouteAnnouncement(progressState.distanceFromRouteMeters, progressState.progress, config, nowMillis)
                ?: instructionAnnouncement(progressState.progress, speedMetersPerSecond, config, nowMillis)
            is ProgressState.Navigating -> instructionAnnouncement(progressState.progress, speedMetersPerSecond, config, nowMillis)
        }?.also { event ->
            lastAnnouncementTimestampMillis = nowMillis
            lastAnnouncementPriority = event.priority
            announcedKeys += event.id
            trimKeys()
        }
    }

    override fun reset() {
        announcedKeys.clear()
        lastAnnouncementTimestampMillis = null
        lastAnnouncementPriority = null
        lastOffRouteAnnouncementMillis = null
    }

    private fun instructionAnnouncement(
        progress: RouteProgress,
        speedMetersPerSecond: Double?,
        config: GuidanceConfig,
        nowMillis: Long,
    ): AnnouncementEvent? {
        val instruction = progress.nextInstruction ?: return null
        val bucket = bucketCalculator.bucketFor(instruction.distanceMeters, speedMetersPerSecond) ?: return null
        val key = "${progress.routeId}|${instruction.stableKey()}|${bucket.meters.toInt()}"
        if (key in announcedKeys) return null
        if (!passesInterval(bucket.priority, config, nowMillis)) return null

        return AnnouncementEvent(
            id = key,
            text = formatter.formatInstruction(instruction, bucket, config.language),
            language = config.language,
            priority = bucket.priority,
            routeId = progress.routeId,
            instructionDistanceMeters = instruction.distanceMeters,
            bucketMeters = bucket.meters,
            maneuverType = instruction.maneuverType,
            maneuverModifier = instruction.maneuverModifier,
            roadName = instruction.roadName,
            createdAtMillis = nowMillis,
        )
    }

    private fun arrivalAnnouncement(
        progress: RouteProgress,
        config: GuidanceConfig,
        nowMillis: Long,
    ): AnnouncementEvent? {
        if (!config.announceArrival) return null
        val key = "${progress.routeId}|arrival"
        if (key in announcedKeys) return null
        if (!passesInterval(AnnouncementPriority.High, config, nowMillis)) return null
        return AnnouncementEvent(
            id = key,
            text = formatter.formatArrival(progress, config.language),
            language = config.language,
            priority = AnnouncementPriority.High,
            routeId = progress.routeId,
            instructionDistanceMeters = 0.0,
            bucketMeters = null,
            maneuverType = "arrive",
            maneuverModifier = null,
            roadName = null,
            createdAtMillis = nowMillis,
        )
    }

    private fun offRouteAnnouncement(
        distanceFromRouteMeters: Double,
        progress: RouteProgress,
        config: GuidanceConfig,
        nowMillis: Long,
    ): AnnouncementEvent? {
        val last = lastOffRouteAnnouncementMillis
        if (last != null && nowMillis - last < config.offRouteRepeatIntervalMillis) return null
        if (!passesInterval(AnnouncementPriority.High, config, nowMillis)) return null
        lastOffRouteAnnouncementMillis = nowMillis
        return AnnouncementEvent(
            id = "${progress.routeId}|off-route|$nowMillis",
            text = formatter.formatOffRoute(distanceFromRouteMeters, config.language),
            language = config.language,
            priority = AnnouncementPriority.High,
            routeId = progress.routeId,
            instructionDistanceMeters = progress.remainingDistanceMeters,
            bucketMeters = null,
            maneuverType = "off-route",
            maneuverModifier = null,
            roadName = null,
            createdAtMillis = nowMillis,
        )
    }

    private fun passesInterval(priority: AnnouncementPriority, config: GuidanceConfig, nowMillis: Long): Boolean {
        val last = lastAnnouncementTimestampMillis ?: return true
        val elapsed = nowMillis - last
        if (elapsed >= config.minAnnouncementIntervalMillis) return true
        val previous = lastAnnouncementPriority ?: return false
        return priority == AnnouncementPriority.High && previous != AnnouncementPriority.High
    }

    private fun NextInstruction.stableKey(): String {
        return listOf(
            maneuverType.orEmpty(),
            maneuverModifier.orEmpty(),
            roadName.orEmpty(),
            instruction,
        ).joinToString("|") { it.trim().lowercase() }
    }

    private fun trimKeys() {
        while (announcedKeys.size > MAX_ANNOUNCED_KEYS) {
            val first = announcedKeys.firstOrNull() ?: return
            announcedKeys.remove(first)
        }
    }

    companion object {
        private const val MAX_ANNOUNCED_KEYS = 256
    }
}
