package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class ExperimentAnnotationRuleTest {

    @Test
    fun `Does not Report issue if class is not abstract experiment`() {
        val findings = """
            class SimpleClass : Interface { }
        """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not Report issue if class is abstract experiment with correct annotation`() {
        val findings = """
            @Experiment
            class SimpleClass : AbstractExperiment { }
        """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Report issue if class is abstract experiment without correct annotation`() {
        val findings = """
            class SimpleClass : AbstractExperiment { }
        """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "It is necessary to specify the annotation \"Experiment\" for the experiment class"
        )
    }

    private fun String.toFindings(): List<Finding> = ExperimentAnnotationRule().lint(trimIndent())
}