package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class ArchitectureLayersRuleTest {

    @Test
    fun `Reports if data layer in domain layer`() {
        val findings = """
            package ru.yandex.market.clean.domain
            
            import ru.yandex.market.clean.data.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "You cannot import classes from data or presentation layers in domain layer"
        )
    }

    @Test
    fun `Reports if data layer in presentation layer`() {
        val findings = """
            package ru.yandex.market.clean.presentation
            
            import ru.yandex.market.clean.data.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "You cannot import classes from data layer in presentation layer"
        )
    }

    @Test
    fun `Reports if presentation layer in domain layer`() {
        val findings = """
            package ru.yandex.market.clean.domain
            
            import ru.yandex.market.clean.presentation.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "You cannot import classes from data or presentation layers in domain layer"
        )
    }

    @Test
    fun `Reports if presentation layer in data layer`() {
        val findings = """
            package ru.yandex.market.clean.data
            
            import ru.yandex.market.clean.presentation.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "You cannot import classes from presentation layer in data layer"
        )
    }

    @Test
    fun `Does not report if domain layer in data layer`() {
        val findings = """
            package ru.yandex.market.clean.data
            
            import ru.yandex.market.clean.domain.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not report if domain layer in presentation layer`() {
        val findings = """
            package ru.yandex.market.clean.presentation
            
            import ru.yandex.market.clean.domain.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not report if presentation layer in presentation layer`() {
        val findings = """
            package ru.yandex.market.clean.presentation
            
            import ru.yandex.market.clean.presentation.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not report if domain layer in domain layer`() {
        val findings = """
            package ru.yandex.market.clean.domain
            
            import ru.yandex.market.clean.domain.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not report if data layer in data layer`() {
        val findings = """
            package ru.yandex.market.clean.data
            
            import ru.yandex.market.clean.data.SomeClass
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not report if repository in domain layer`() {
        val findings = """
            package ru.yandex.market.clean.domain
            
            import ru.yandex.market.clean.data.SomeRepository
            
        """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = ArchitectureLayersRule().lint(trimIndent())
}