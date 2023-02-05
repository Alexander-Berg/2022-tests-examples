package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PublicTypeInjectConstructorRuleTest {

    @Test
    fun `Reports issue for public class with @Inject primary constructor`() {
        val findings = PublicTypeInjectConstructorRule(checkPackage = false).lint(
            """
                class Foo @Inject constructor
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("Using @Inject on constructor of public type.")
    }

    @Test
    fun `Reports issue for public class with @Inject secondary constructor`() {
        val findings = PublicTypeInjectConstructorRule(checkPackage = false).lint(
            """
                class Foo {
                    @Inject constructor()
                }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("Using @Inject on constructor of public type.")
    }

    @Test
    fun `Don't reports issue for non-public class with @Inject primary constructor`() {
        val findings = PublicTypeInjectConstructorRule(checkPackage = false).lint(
            """
                internal class Foo @Inject constructor
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }

    @Test
    fun `Don't reports issue for non-public class with @Inject secondary constructor`() {
        val findings = PublicTypeInjectConstructorRule(checkPackage = false).lint(
            """
                internal class Foo {
                    @Inject constructor()
                }
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }
}