package com.msa.professionalmap.core.observability.domain

interface AppMonitor {
    fun setUserId(userId: String?)
    fun setCustomKey(key: String, value: String)
    fun recordException(throwable: Throwable, context: Map<String, String> = emptyMap())
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun startTrace(name: String): AppTrace
}

interface AppTrace {
    fun putMetric(name: String, value: Long)
    fun incrementMetric(name: String, by: Long = 1L)
    fun stop()
}

object DisabledAppMonitor : AppMonitor {
    override fun setUserId(userId: String?) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
    override fun recordException(throwable: Throwable, context: Map<String, String>) = Unit
    override fun logEvent(name: String, params: Map<String, String>) = Unit
    override fun startTrace(name: String): AppTrace = DisabledAppTrace
}

object DisabledAppTrace : AppTrace {
    override fun putMetric(name: String, value: Long) = Unit
    override fun incrementMetric(name: String, by: Long) = Unit
    override fun stop() = Unit
}

object MonitorEvents {
    const val APP_STARTED = "app_started"
    const val ROUTE_CALCULATED = "route_calculated"
    const val ROUTE_CALCULATION_FAILED = "route_calculation_failed"
    const val NAVIGATION_STARTED = "navigation_started"
    const val NAVIGATION_COMPLETED = "navigation_completed"
    const val NAVIGATION_CANCELLED = "navigation_cancelled"
    const val OFF_ROUTE_DETECTED = "off_route_detected"
    const val REROUTE_TRIGGERED = "reroute_triggered"
    const val OFFLINE_DOWNLOAD_QUEUED = "offline_download_queued"
    const val VOICE_GUIDANCE_ERROR = "voice_guidance_error"
    const val GEO_ENGINE_SELECTED = "geo_engine_selected"
}

object MonitorTraces {
    const val ROUTE_CALCULATION = "route_calculation"
    const val OFFLINE_DOWNLOAD_ENQUEUE = "offline_download_enqueue"
}
