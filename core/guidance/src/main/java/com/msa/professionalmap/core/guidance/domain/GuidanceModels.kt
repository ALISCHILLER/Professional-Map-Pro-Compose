package com.msa.professionalmap.core.guidance.domain

import com.msa.professionalmap.core.progress.domain.NextInstruction
import com.msa.professionalmap.core.progress.domain.ProgressState
import com.msa.professionalmap.core.progress.domain.RouteProgress

enum class GuidanceLanguage(val bcp47Tag: String) {
    English("en-US"),
    Persian("fa-IR"),
}

enum class AnnouncementPriority {
    Low,
    Medium,
    High,
}

enum class RoadContext {
    Highway,
    City,
    Walking,
}

data class GuidanceConfig(
    val enabled: Boolean = true,
    val muted: Boolean = false,
    val language: GuidanceLanguage = GuidanceLanguage.English,
    val volume: Float = 1.0f,
    val minAnnouncementIntervalMillis: Long = 5_000L,
    val offRouteRepeatIntervalMillis: Long = 15_000L,
    val announceArrival: Boolean = true,
) {
    init {
        require(volume in 0f..1f) { "volume must be in [0, 1]." }
        require(minAnnouncementIntervalMillis >= 0L) { "minAnnouncementIntervalMillis must be non-negative." }
        require(offRouteRepeatIntervalMillis >= 0L) { "offRouteRepeatIntervalMillis must be non-negative." }
    }
}

data class DistanceBucket(
    val meters: Double,
    val roadContext: RoadContext,
    val priority: AnnouncementPriority,
) {
    init {
        require(meters > 0.0) { "Bucket distance must be positive." }
    }
}

data class AnnouncementEvent(
    val id: String,
    val text: String,
    val language: GuidanceLanguage,
    val priority: AnnouncementPriority,
    val routeId: String?,
    val instructionDistanceMeters: Double?,
    val bucketMeters: Double?,
    val maneuverType: String?,
    val maneuverModifier: String?,
    val roadName: String?,
    val createdAtMillis: Long,
) {
    init {
        require(text.isNotBlank()) { "Announcement text must not be blank." }
        require(instructionDistanceMeters == null || instructionDistanceMeters >= 0.0) {
            "instructionDistanceMeters must be null or non-negative."
        }
        require(bucketMeters == null || bucketMeters > 0.0) { "bucketMeters must be null or positive." }
    }
}

data class GuidanceState(
    val enabled: Boolean = true,
    val muted: Boolean = false,
    val language: GuidanceLanguage = GuidanceLanguage.English,
    val volume: Float = 1.0f,
    val lastAnnouncement: AnnouncementEvent? = null,
    val recentAnnouncements: List<AnnouncementEvent> = emptyList(),
    val lastErrorMessage: String? = null,
) {
    val isSpeakable: Boolean get() = enabled && !muted
}

data class GuidanceEvaluation(
    val state: GuidanceState,
    val eventToSpeak: AnnouncementEvent?,
)

interface VoiceGuidanceRepository {
    fun evaluate(
        progressState: ProgressState,
        speedMetersPerSecond: Double?,
        config: GuidanceConfig,
        nowMillis: Long,
    ): GuidanceEvaluation

    fun reset(config: GuidanceConfig = GuidanceConfig()): GuidanceState
}

interface DistanceBucketCalculator {
    fun roadContext(speedMetersPerSecond: Double?): RoadContext
    fun bucketsFor(speedMetersPerSecond: Double?): List<DistanceBucket>
    fun bucketFor(distanceMeters: Double, speedMetersPerSecond: Double?): DistanceBucket?
}

interface InstructionFormatter {
    fun formatInstruction(
        instruction: NextInstruction,
        bucket: DistanceBucket,
        language: GuidanceLanguage,
    ): String

    fun formatArrival(progress: RouteProgress, language: GuidanceLanguage): String
    fun formatOffRoute(distanceFromRouteMeters: Double, language: GuidanceLanguage): String
    fun formatTest(language: GuidanceLanguage): String
}

interface AnnouncementScheduler {
    fun nextAnnouncement(
        progressState: ProgressState,
        speedMetersPerSecond: Double?,
        config: GuidanceConfig,
        nowMillis: Long,
    ): AnnouncementEvent?

    fun reset()
}

interface TtsEngine : AutoCloseable {
    val isSpeaking: Boolean
    suspend fun speak(
        text: String,
        language: GuidanceLanguage,
        volume: Float,
    ): Result<Unit>

    fun stop()
    override fun close() = stop()
}
