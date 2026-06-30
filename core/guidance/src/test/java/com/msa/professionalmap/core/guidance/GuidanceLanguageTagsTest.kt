package com.msa.professionalmap.core.guidance

import com.msa.professionalmap.core.guidance.domain.GuidanceLanguage
import com.msa.professionalmap.core.guidance.domain.toLanguageTag
import org.junit.Assert.assertEquals
import org.junit.Test

class GuidanceLanguageTagsTest {
    @Test
    fun languageTags_areStableBcp47Values() {
        assertEquals("en", GuidanceLanguage.English.toLanguageTag())
        assertEquals("fa", GuidanceLanguage.Persian.toLanguageTag())
    }
}
