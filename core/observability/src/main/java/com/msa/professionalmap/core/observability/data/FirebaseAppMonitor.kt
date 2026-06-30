package com.msa.professionalmap.core.observability.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.msa.professionalmap.core.observability.domain.AppMonitor
import com.msa.professionalmap.core.observability.domain.AppTrace
import com.msa.professionalmap.core.observability.domain.DisabledAppMonitor

class FirebaseAppMonitor private constructor(
    context: Context,
    collectionEnabled: Boolean,
) : AppMonitor {
    private val appContext = context.applicationContext
    private val analytics = FirebaseAnalytics.getInstance(appContext)
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val performance = FirebasePerformance.getInstance()

    init {
        crashlytics.setCrashlyticsCollectionEnabled(collectionEnabled)
        analytics.setAnalyticsCollectionEnabled(collectionEnabled)
        performance.isPerformanceCollectionEnabled = collectionEnabled
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId.orEmpty())
        analytics.setUserId(userId)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun recordException(throwable: Throwable, context: Map<String, String>) {
        context.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
        crashlytics.recordException(throwable)
    }

    override fun logEvent(name: String, params: Map<String, String>) {
        analytics.logEvent(name, params.toBundle())
    }

    override fun startTrace(name: String): AppTrace {
        val trace = performance.newTrace(name)
        trace.start()
        return FirebaseAppTrace(trace)
    }

    private fun Map<String, String>.toBundle(): Bundle = Bundle().apply {
        forEach { (key, value) -> putString(key.take(MAX_PARAM_KEY_LENGTH), value.take(MAX_PARAM_VALUE_LENGTH)) }
    }

    private class FirebaseAppTrace(private val trace: Trace) : AppTrace {
        override fun putMetric(name: String, value: Long) {
            trace.putMetric(name, value)
        }

        override fun incrementMetric(name: String, by: Long) {
            trace.incrementMetric(name, by)
        }

        override fun stop() {
            trace.stop()
        }
    }

    companion object {
        private const val MAX_PARAM_KEY_LENGTH = 40
        private const val MAX_PARAM_VALUE_LENGTH = 100

        fun createOrDisabled(context: Context, collectionEnabled: Boolean): AppMonitor {
            return runCatching {
                FirebaseApp.initializeApp(context.applicationContext) ?: return DisabledAppMonitor
                FirebaseAppMonitor(context, collectionEnabled)
            }.getOrElse {
                DisabledAppMonitor
            }
        }
    }
}
