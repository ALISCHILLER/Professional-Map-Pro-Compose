package com.msa.professionalmap.core.service

import com.msa.professionalmap.core.service.data.NavigationReroutePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationReroutePolicyTest {
    @Test
    fun requiresRepeatedOffRouteEvidenceAndEnforcesCooldown() {
        val policy = NavigationReroutePolicy(
            debounceMillis = 5_000L,
            retryCooldownMillis = 20_000L,
            minimumConsecutiveSamples = 2,
        )

        assertNull(policy.delayBeforeAttempt(1_000L))
        assertEquals(4_000L, policy.delayBeforeAttempt(2_000L))
        policy.markAttempt(6_000L)
        assertEquals(19_000L, policy.delayBeforeAttempt(7_000L))
    }

    @Test
    fun recoveryResetsConsecutiveEvidence() {
        val policy = NavigationReroutePolicy(5_000L, minimumConsecutiveSamples = 2)
        assertNull(policy.delayBeforeAttempt(1_000L))
        policy.onRouteRecovered()
        assertNull(policy.delayBeforeAttempt(2_000L))
    }
}
