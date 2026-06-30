package com.msa.professionalmap.core.guidance.data

import com.msa.professionalmap.core.guidance.domain.DistanceBucket
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.InstructionFormatter
import com.msa.professionalmap.core.progress.domain.NextInstruction
import com.msa.professionalmap.core.progress.domain.RouteProgress
import kotlin.math.roundToInt

class DefaultInstructionFormatter : InstructionFormatter {
    override fun formatInstruction(
        instruction: NextInstruction,
        bucket: DistanceBucket,
        language: GuidanceLanguage,
    ): String {
        return when (language) {
            GuidanceLanguage.English -> formatEnglishInstruction(instruction, bucket)
            GuidanceLanguage.Persian -> formatPersianInstruction(instruction, bucket)
        }
    }

    override fun formatArrival(progress: RouteProgress, language: GuidanceLanguage): String {
        return when (language) {
            GuidanceLanguage.English -> "You have arrived at your destination."
            GuidanceLanguage.Persian -> "به مقصد رسیدید."
        }
    }

    override fun formatOffRoute(distanceFromRouteMeters: Double, language: GuidanceLanguage): String {
        val distance = distanceFromRouteMeters.formatDistance(language)
        return when (language) {
            GuidanceLanguage.English -> "You are off route by $distance. Rerouting if needed."
            GuidanceLanguage.Persian -> "شما حدود $distance از مسیر خارج شده‌اید. در صورت نیاز مسیر جدید محاسبه می‌شود."
        }
    }

    override fun formatTest(language: GuidanceLanguage): String {
        return when (language) {
            GuidanceLanguage.English -> "Voice guidance is ready."
            GuidanceLanguage.Persian -> "راهنمای صوتی آماده است."
        }
    }

    private fun formatEnglishInstruction(instruction: NextInstruction, bucket: DistanceBucket): String {
        val distance = bucket.meters.formatDistance(GuidanceLanguage.English)
        val road = instruction.roadName?.takeIf { it.isNotBlank() }?.let { " onto $it" }.orEmpty()
        val maneuver = englishManeuver(instruction.maneuverType, instruction.maneuverModifier)
        return when {
            instruction.maneuverType == "arrive" -> "In $distance, you will arrive at your destination."
            maneuver.isNotBlank() -> "In $distance, $maneuver$road."
            instruction.instruction.isNotBlank() -> "In $distance, ${instruction.instruction}."
            else -> "Continue for $distance."
        }
    }

    private fun englishManeuver(type: String?, modifier: String?): String {
        val normalizedType = type.orEmpty().lowercase()
        val normalizedModifier = modifier.orEmpty().replace('-', ' ').lowercase()
        return when (normalizedType) {
            "turn" -> if (normalizedModifier.isBlank()) "turn" else "turn $normalizedModifier"
            "new name" -> "continue"
            "depart" -> "depart"
            "arrive" -> "arrive"
            "merge" -> if (normalizedModifier.isBlank()) "merge" else "merge $normalizedModifier"
            "fork" -> if (normalizedModifier.isBlank()) "keep going" else "keep $normalizedModifier"
            "roundabout", "rotary" -> "enter the roundabout"
            "continue" -> "continue"
            "notification" -> "continue"
            else -> if (normalizedType.isBlank()) "" else normalizedType.replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatPersianInstruction(instruction: NextInstruction, bucket: DistanceBucket): String {
        val distance = bucket.meters.formatDistance(GuidanceLanguage.Persian)
        val road = instruction.roadName?.takeIf { it.isNotBlank() }?.let { " به سمت $it" }.orEmpty()
        val maneuver = persianManeuver(instruction.maneuverType, instruction.maneuverModifier)
        return when {
            instruction.maneuverType == "arrive" -> "$distance دیگر به مقصد می‌رسید."
            maneuver.isNotBlank() -> "$distance دیگر $maneuver$road."
            instruction.instruction.isNotBlank() -> "$distance دیگر ادامه دهید."
            else -> "$distance دیگر مستقیم ادامه دهید."
        }
    }

    private fun persianManeuver(type: String?, modifier: String?): String {
        val normalizedType = type.orEmpty().lowercase()
        val normalizedModifier = modifier.orEmpty().replace('-', ' ').lowercase()
        return when (normalizedType) {
            "turn" -> when {
                "right" in normalizedModifier -> "به راست بپیچید"
                "left" in normalizedModifier -> "به چپ بپیچید"
                "straight" in normalizedModifier -> "مستقیم ادامه دهید"
                else -> "بپیچید"
            }
            "new name", "continue", "notification" -> "مستقیم ادامه دهید"
            "depart" -> "حرکت را شروع کنید"
            "arrive" -> "به مقصد می‌رسید"
            "merge" -> when {
                "right" in normalizedModifier -> "از سمت راست وارد مسیر شوید"
                "left" in normalizedModifier -> "از سمت چپ وارد مسیر شوید"
                else -> "وارد مسیر شوید"
            }
            "fork" -> when {
                "right" in normalizedModifier -> "از انشعاب سمت راست ادامه دهید"
                "left" in normalizedModifier -> "از انشعاب سمت چپ ادامه دهید"
                else -> "از انشعاب مناسب ادامه دهید"
            }
            "roundabout", "rotary" -> "وارد میدان شوید"
            else -> "ادامه دهید"
        }
    }

    private fun Double.formatDistance(language: GuidanceLanguage): String {
        val isKilometers = this >= 1000.0
        val rawValue = if (isKilometers) {
            val km = this / 1000.0
            if (km >= 10.0) km.roundToInt().toString() else "%.1f".format(km)
        } else {
            roundToNearestReadableMeter(this).toString()
        }
        return when (language) {
            GuidanceLanguage.English -> if (isKilometers) "$rawValue kilometers" else "$rawValue meters"
            GuidanceLanguage.Persian -> {
                val unit = if (isKilometers) "کیلومتر" else "متر"
                "$rawValue $unit".toPersianDigits()
            }
        }
    }

    private fun roundToNearestReadableMeter(value: Double): Int {
        return when {
            value >= 1000.0 -> value.roundToInt()
            value >= 100.0 -> ((value / 50.0).roundToInt() * 50).coerceAtLeast(50)
            value >= 20.0 -> ((value / 10.0).roundToInt() * 10).coerceAtLeast(10)
            else -> value.roundToInt().coerceAtLeast(1)
        }
    }

    private fun String.toPersianDigits(): String = buildString {
        this@toPersianDigits.forEach { char ->
            append(
                when (char) {
                    '0' -> '۰'
                    '1' -> '۱'
                    '2' -> '۲'
                    '3' -> '۳'
                    '4' -> '۴'
                    '5' -> '۵'
                    '6' -> '۶'
                    '7' -> '۷'
                    '8' -> '۸'
                    '9' -> '۹'
                    else -> char
                }
            )
        }
    }
}
