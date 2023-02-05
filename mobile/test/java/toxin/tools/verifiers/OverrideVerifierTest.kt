package toxin.tools.verifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import toxin.Component
import toxin.module
import toxin.scope

class OverrideVerifierTest {

    @Test
    fun `Check verifier returns no errors if definition is not overridden`() {
        val errors = OverrideVerifier().verify(
            NoOverrideComponent::class.java
        ) { _, _ -> }
        assertThat(errors.size).isEqualTo(0)
    }

    @Test
    fun `Check verifier returns errors if definition is overridden without flag`() {
        val errors = OverrideVerifier().verify(
            IllegalOverrideComponent::class.java
        ) { _, _ -> }
        assertThat(errors.size).isEqualTo(1)
    }

    @Test
    fun `Check verifier returns no errors if definition is overridden with flag`() {
        val errors = OverrideVerifier().verify(
            LegalOverrideComponent::class.java
        ) { _, _ -> }
        assertThat(errors.size).isEqualTo(0)
    }
}

private class NoOverrideComponent : Component(scopeWithoutOverride)
private class IllegalOverrideComponent : Component(scopeWithIllegalOverride)
private class LegalOverrideComponent : Component(scopeWithLegalOverride)

private val baseScope = scope("base-scope") {
    useModule(module {
        factory<String> { "base-string" }
    })
}
private val scopeWithoutOverride = scope("feature-scope") {
    dependsOn(baseScope)
}
private val scopeWithIllegalOverride = scope("feature-scope") {
    dependsOn(baseScope)
    useModule(module {
        factory<String> { "feature-string" }
    })
}
private val scopeWithLegalOverride = scope("feature-scope") {
    dependsOn(baseScope)
    useModule(module {
        factory<String>(allowOverride = true) { "feature-string" }
    })
}
