package com.msa.professionalmap.core.observability

import com.msa.professionalmap.core.observability.domain.DisabledAppMonitor
import org.junit.Assert.assertNotNull
import org.junit.Test

class DisabledAppMonitorTest {
    @Test
    fun noOpMonitorAcceptsAllCalls() {
        DisabledAppMonitor.setUserId("user")
        DisabledAppMonitor.setCustomKey("key", "value")
        DisabledAppMonitor.logEvent("event", mapOf("k" to "v"))
        DisabledAppMonitor.recordException(IllegalStateException("test"), mapOf("area" to "unit_test"))
        val trace = DisabledAppMonitor.startTrace("trace")
        trace.incrementMetric("count")
        trace.putMetric("value", 10L)
        trace.stop()
        assertNotNull(trace)
    }
}
