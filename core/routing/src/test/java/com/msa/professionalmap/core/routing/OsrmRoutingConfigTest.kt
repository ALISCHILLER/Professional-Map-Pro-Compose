package com.msa.professionalmap.core.routing

import org.junit.Assert.assertEquals
import org.junit.Test

class OsrmRoutingConfigTest {
    @Test
    fun trimsTrailingSlashFromBaseUrl() {
        val config = OsrmRoutingConfig(baseUrl = "https://routing.example.com///")
        assertEquals("https://routing.example.com", config.normalizedBaseUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCleartextByDefault() {
        OsrmRoutingConfig(baseUrl = "http://10.0.2.2:5000")
    }

    @Test
    fun allowsCleartextOnlyWhenExplicitlyEnabled() {
        val config = OsrmRoutingConfig(
            baseUrl = "http://10.0.2.2:5000",
            allowCleartextTraffic = true,
        )
        assertEquals("http://10.0.2.2:5000", config.normalizedBaseUrl)
    }
}
