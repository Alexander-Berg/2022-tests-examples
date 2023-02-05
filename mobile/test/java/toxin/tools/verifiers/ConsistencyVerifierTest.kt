package toxin.tools.verifiers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import toxin.Component
import toxin.module
import toxin.scope

class ConsistencyVerifierTest {

    @Test
    fun `Check verifier returns no error for consistent scope`() {
        val errors = ConsistencyVerifier().verify(
            ConsistentComponent::class.java,
            getterFilter = { true },
            { _, _ -> },
            { _, _ -> }
        )
        assertThat(errors.size).isEqualTo(0)
    }

    @Test
    fun `Check verifier returns error for inconsistent scope`() {
        val errors = ConsistencyVerifier().verify(
            InconsistentComponent::class.java,
            getterFilter = { true },
            { _, _ -> },
            { _, _ -> }
        )
        assertThat(errors.size).isEqualTo(1)
    }
}

private val consistentScope = scope("consistent-scope") {
    useModule(module {
        factory<Int> { 42 }
        factory<String> { get<Int>().toString() }
    })
}

private val inconsistentScope = scope("inconsistent-scope") {
    useModule(module {
        factory<String> { get<Int>().toString() }
    })
}

private class ConsistentComponent : Component(consistentScope) {

    fun string() = auto<String>()
}

private class InconsistentComponent : Component(inconsistentScope) {

    fun string() = auto<String>()
}
