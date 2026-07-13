package com.msa.professionalmap.core.observability.data

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.msa.professionalmap.core.observability.domain.AppMonitor
import com.msa.professionalmap.core.observability.domain.AppTrace
import com.msa.professionalmap.core.observability.domain.DisabledAppMonitor
import com.msa.professionalmap.core.observability.domain.PrivacySanitizer
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Explicitly opt-in Firebase monitor with no automatic network-performance instrumentation. */
class FirebaseAppMonitor private constructor(
    context: Context,
    collectionEnabled: Boolean,
) : AppMonitor {
    private val appContext = context.applicationContext
    private val analytics = FirebaseAnalytics.getInstance(appContext)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        crashlytics.setCrashlyticsCollectionEnabled(collectionEnabled)
        analytics.setAnalyticsCollectionEnabled(collectionEnabled)
    }

    override fun setUserId(userId: String?) {
        val pseudonymousId = userId
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(::pseudonymize)
            .orEmpty()
        crashlytics.setUserId(pseudonymousId)
        analytics.setUserId(pseudonymousId.ifBlank { null })
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(
            PrivacySanitizer.sanitizeKey(key),
            PrivacySanitizer.sanitize(value),
        )
    }

    override fun recordException(throwable: Throwable, context: Map<String, String>) {
        PrivacySanitizer.sanitizeContext(context).forEach { (key, value) ->
            crashlytics.setCustomKey(PrivacySanitizer.sanitizeKey(key), value)
        }
        crashlytics.recordException(PrivacySanitizer.sanitizedException(throwable))
    }

    override fun logEvent(name: String, params: Map<String, String>) {
        analytics.logEvent(
            PrivacySanitizer.sanitizeEventName(name),
            PrivacySanitizer.sanitizeContext(params).toBundle(),
        )
    }

    override fun startTrace(name: String): AppTrace = AnalyticsTimingTrace(
        analytics = analytics,
        traceName = PrivacySanitizer.sanitizeEventName(name),
    )

    private fun pseudonymize(userId: String): String {
        val source = "${appContext.packageName}:$userId".encodeToByteArray()
        return MessageDigest.getInstance("SHA-256")
            .digest(source)
            .take(PseudonymousIdBytes)
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun Map<String, String>.toBundle(): Bundle = Bundle().apply {
        entries.take(MaxEventParameters).forEach { (key, value) ->
            putString(
                PrivacySanitizer.sanitizeKey(key).take(MaxParameterKeyLength),
                value.take(MaxParameterValueLength),
            )
        }
    }

    private class AnalyticsTimingTrace(
        private val analytics: FirebaseAnalytics,
        private val traceName: String,
    ) : AppTrace {
        private val startedAtMillis = SystemClock.elapsedRealtime()
        private val metrics = ConcurrentHashMap<String, Long>()
        private val stopped = AtomicBoolean(false)

        override fun putMetric(name: String, value: Long) {
            metrics[PrivacySanitizer.sanitizeKey(name)] = value
        }

        override fun incrementMetric(name: String, by: Long) {
            metrics.merge(PrivacySanitizer.sanitizeKey(name), by, Long::plus)
        }

        override fun stop() {
            if (!stopped.compareAndSet(false, true)) return
            val params = Bundle().apply {
                putString("trace_name", traceName.take(MaxParameterValueLength))
                putLong("duration_ms", (SystemClock.elapsedRealtime() - startedAtMillis).coerceAtLeast(0L))
                metrics.entries.take(MaxTraceMetrics).forEachIndexed { index, (key, value) ->
                    putString("metric_${index}_name", key.take(MaxParameterValueLength))
                    putLong("metric_${index}_value", value)
                }
            }
            analytics.logEvent(PerformanceEventName, params)
        }
    }

    companion object {
        private const val MaxParameterKeyLength = 40
        private const val MaxParameterValueLength = 100
        private const val MaxEventParameters = 20
        private const val MaxTraceMetrics = 8
        private const val PseudonymousIdBytes = 16
        private const val PerformanceEventName = "performance_trace"

        fun createOrDisabled(context: Context, collectionEnabled: Boolean): AppMonitor {
            if (!collectionEnabled) return DisabledAppMonitor
            return runCatching {
                FirebaseApp.initializeApp(context.applicationContext) ?: return DisabledAppMonitor
                FirebaseAppMonitor(context, collectionEnabled)
            }.getOrElse {
                DisabledAppMonitor
            }
        }
    }
}
