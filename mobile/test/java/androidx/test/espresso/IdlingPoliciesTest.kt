package androidx.test.espresso

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.util.concurrent.TimeUnit

private const val TIMEOUT = 42L

class IdlingPoliciesTest {

    private val defaultPolicy = IdlingPolicies.getDynamicIdlingResourceWarningPolicy()

    private val timeUnit = TimeUnit.MILLISECONDS

    @Test
    fun testSetWarningIdlingPolicyTimeout() {
        setWarningIdlingPolicyTimeout(TIMEOUT, timeUnit)
        val policy = IdlingPolicies.getDynamicIdlingResourceWarningPolicy()

        assertThat(policy.idleTimeout).isEqualTo(TIMEOUT)
        assertThat(policy.idleTimeoutUnit).isEqualTo(timeUnit)
    }

    @After
    fun after() {
        setWarningIdlingPolicy(defaultPolicy)
    }
}
