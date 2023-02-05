package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ImplicitPublicTypeRuleTest {

    @Test
    fun `Reports issue for public class without public modifier`() {
        val findings = ImplicitPublicTypeRule(checkPackage = false).lint(
            """
                class Foo
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("Ambiguous visibility. Use internal or public keyword.")
    }

    @Test
    fun `Reports issue for public object without public modifier`() {
        val findings = ImplicitPublicTypeRule(checkPackage = false).lint(
            """
                object Foo
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("Ambiguous visibility. Use internal or public keyword.")
    }

    @Test
    fun `Don't reports issue for public class with public modifier`() {
        val findings = ImplicitPublicTypeRule(checkPackage = false).lint(
            """
                public class Foo
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }

    @Test
    fun `Don't reports issue for public object with public modifier`() {
        val findings = ImplicitPublicTypeRule(checkPackage = false).lint(
            """
                public object Foo
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }

    @Test
    fun `Don't reports issue for non-public class without public modifier`() {
        val findings = ImplicitPublicTypeRule(checkPackage = false).lint(
            """
                internal class Foo
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }
}