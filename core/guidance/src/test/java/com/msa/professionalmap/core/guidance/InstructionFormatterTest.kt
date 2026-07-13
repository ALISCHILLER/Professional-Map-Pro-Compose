package com.msa.professionalmap.core.guidance

import com.msa.professionalmap.core.guidance.data.DefaultInstructionFormatter
import com.msa.professionalmap.core.guidance.domain.AnnouncementPriority
import com.msa.professionalmap.core.guidance.domain.DistanceBucket
import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.RoadContext
import com.msa.professionalmap.core.progress.domain.NextInstruction
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionFormatterTest {
    private val formatter = DefaultInstructionFormatter()
    private val bucket = DistanceBucket(200.0, RoadContext.City, AnnouncementPriority.Medium)

    @Test fun formatsEnglishTurnInstruction() {
        val text = formatter.formatInstruction(nextInstruction(), bucket, GuidanceLanguage.English)
        assertTrue(text.contains("turn right"))
        assertTrue(text.contains("200 meters"))
    }

    @Test fun formatsPersianTurnInstruction() {
        val text = formatter.formatInstruction(nextInstruction(), bucket, GuidanceLanguage.Persian)
        assertTrue(text.contains("راست"))
        assertTrue(text.contains("۲۰۰"))
    }

    private fun nextInstruction(): NextInstruction = NextInstruction(
        instruction = "Turn right onto Vali-e Asr",
        distanceMeters = 180.0,
        roadName = "Vali-e Asr",
        maneuverType = "turn",
        maneuverModifier = "right",
        sourceInstruction = null,
    )
}
