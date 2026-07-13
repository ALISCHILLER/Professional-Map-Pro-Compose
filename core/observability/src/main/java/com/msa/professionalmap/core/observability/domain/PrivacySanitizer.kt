package com.msa.professionalmap.core.observability.domain

/** Last-resort privacy boundary before data leaves the process. */
object PrivacySanitizer {
    private val urlPattern = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
    private val coordinatePairPattern = Regex(
        "[\\[(]?[-+]?\\d{1,3}\\.\\d{3,}\\s*[,; ]\\s*[-+]?\\d{1,3}\\.\\d{3,}[\\])]?"
    )
    private val labeledCoordinatePattern = Regex(
        "(?i)(latitude|longitude|lat|lon|lng)[\\\"']?\\s*[:=]\\s*[\\\"']?[-+]?\\d{1,3}(?:\\.\\d{3,})?"
    )
    private val bearerPattern = Regex("(?i)bearer\\s+[a-z0-9._~+/=-]+")
    private val querySecretPattern = Regex("(?i)(token|key|api[_-]?key|access[_-]?token|signature)=([^&\\s]+)")
    private val unsafeNameCharacters = Regex("[^a-zA-Z0-9_]")

    fun sanitize(value: String?): String {
        if (value.isNullOrBlank()) return "unknown"
        return value
            .replace(urlPattern, "[redacted-url]")
            .replace(labeledCoordinatePattern) { match ->
                "${match.groupValues[1]}=[redacted-location]"
            }
            .replace(coordinatePairPattern, "[redacted-location]")
            .replace(bearerPattern, "Bearer [redacted]")
            .replace(querySecretPattern) { match -> "${match.groupValues[1]}=[redacted]" }
            .take(MaxValueLength)
    }

    fun sanitizeKey(value: String): String = value
        .trim()
        .replace(unsafeNameCharacters, "_")
        .trim('_')
        .take(MaxKeyLength)
        .ifBlank { "unknown_key" }

    fun sanitizeEventName(value: String): String = sanitizeKey(value)
        .let { name -> if (name.firstOrNull()?.isLetter() == true) name else "event_$name" }
        .take(MaxEventNameLength)

    fun sanitizeContext(context: Map<String, String>): Map<String, String> =
        context.entries.associate { (key, value) -> sanitizeKey(key) to sanitize(value) }

    fun sanitizedException(throwable: Throwable): RuntimeException {
        val type = throwable::class.simpleName ?: "Throwable"
        return RuntimeException("$type: ${sanitize(throwable.message)}").also { sanitized ->
            // Keep the useful call path while intentionally omitting the original cause, whose
            // message may still contain precise coordinates, URLs or credentials.
            sanitized.stackTrace = throwable.stackTrace
        }
    }

    private const val MaxValueLength = 200
    private const val MaxKeyLength = 40
    private const val MaxEventNameLength = 40
}
