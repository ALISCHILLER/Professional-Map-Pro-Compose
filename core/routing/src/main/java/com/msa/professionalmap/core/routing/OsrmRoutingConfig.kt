package com.msa.professionalmap.core.routing

/**
 * OSRM endpoint configuration.
 *
 * The default public routing server is convenient for development only. For production, self-host OSRM
 * or use a provider whose terms explicitly allow your traffic volume and product use case.
 */
data class OsrmRoutingConfig(
    val baseUrl: String = "https://router.project-osrm.org",
    val userAgent: String = "ProfessionalMapPro/1.0",
    val requestTimeoutMillis: Long = 15_000L,
    val allowCleartextTraffic: Boolean = false,
) {
    init {
        val trimmedBaseUrl = baseUrl.trim()
        require(trimmedBaseUrl.isNotBlank()) { "baseUrl must not be blank." }
        require(trimmedBaseUrl.startsWith("https://") || (allowCleartextTraffic && trimmedBaseUrl.startsWith("http://"))) {
            "baseUrl must use https:// unless allowCleartextTraffic is explicitly enabled."
        }
        require(userAgent.isNotBlank()) { "userAgent must not be blank." }
        require(requestTimeoutMillis > 0L) { "requestTimeoutMillis must be positive." }
    }

    val normalizedBaseUrl: String get() = baseUrl.trim().trimEnd('/')
}
