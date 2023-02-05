package androidx.test.espresso

import androidx.annotation.VisibleForTesting
import java.util.concurrent.TimeUnit

fun setWarningIdlingPolicyTimeout(timeout: Long, timeUnit: TimeUnit) {
    val policy = IdlingPolicy.Builder()
        .withIdlingTimeout(timeout)
        .withIdlingTimeoutUnit(timeUnit)
        .logWarning()
        .build()
    setWarningIdlingPolicy(policy)
}

@VisibleForTesting internal fun setWarningIdlingPolicy(idlingPolicy: IdlingPolicy) {
    val policyField = IdlingPolicies::class.java.getDeclaredField("dynamicIdlingResourceWarningPolicy")
    policyField.isAccessible = true
    policyField.set(null, idlingPolicy)
}
