package com.msa.professionalmap.core.observability

import com.msa.professionalmap.core.observability.domain.PrivacySanitizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacySanitizerTest {
    @Test
    fun `redacts location url bearer token and query secrets`() {
        val raw = "GET https://router.example/route/51.3890,35.6892?api_key=secret Authorization: Bearer token.value"

        val sanitized = PrivacySanitizer.sanitize(raw)

        assertTrue(sanitized.contains("[redacted-url]"))
        assertFalse(sanitized.contains("51.3890"))
        assertFalse(sanitized.contains("35.6892"))
        assertFalse(sanitized.contains("secret"))
        assertFalse(sanitized.contains("token.value"))
    }

    @Test
    fun `sanitized exception does not retain original cause or message`() {
        val original = IllegalStateException("route failed at 51.3890,35.6892")

        val sanitized = PrivacySanitizer.sanitizedException(original)

        assertTrue(sanitized.message.orEmpty().contains("[redacted-location]"))
        assertFalse(sanitized.message.orEmpty().contains("51.3890"))
        assertTrue(sanitized.cause == null)
        assertTrue(sanitized.stackTrace.contentEquals(original.stackTrace))
    }

    @Test
    fun `redacts labeled and bracketed coordinate formats`() {
        val raw = "latitude=35.6892 longitude:51.3890 point=[51.4000, 35.7000]"

        val sanitized = PrivacySanitizer.sanitize(raw)

        assertFalse(sanitized.contains("35.6892"))
        assertFalse(sanitized.contains("51.3890"))
        assertFalse(sanitized.contains("51.4000"))
        assertFalse(sanitized.contains("35.7000"))
    }

    @Test
    fun `redacts quoted json coordinates and three decimal precision`() {
        val raw = "{\"latitude\":35.689,\"longitude\":51.389}"

        val sanitized = PrivacySanitizer.sanitize(raw)

        assertFalse(sanitized.contains("35.689"))
        assertFalse(sanitized.contains("51.389"))
        assertTrue(sanitized.contains("[redacted-location]"))
    }

    @Test
    fun `normalizes unsafe telemetry names`() {
        assertTrue(PrivacySanitizer.sanitizeEventName("123 route.failed").startsWith("event_"))
        assertFalse(PrivacySanitizer.sanitizeKey("route key/location").contains("/"))
        assertFalse(PrivacySanitizer.sanitizeKey("route key/location").contains(" "))
    }
}
