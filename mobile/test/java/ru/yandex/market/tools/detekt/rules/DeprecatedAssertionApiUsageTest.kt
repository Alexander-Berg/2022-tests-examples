package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeprecatedAssertionApiUsageTest {

    @Test
    fun `Reports issue for junit assertion api import`() {
        val findings = DeprecatedAssertionApiUsage().lint(
            """
                import org.junit.Assert.assertThat
                
                class MyTest {
                
                }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("Junit assertion Api usage.")
    }

    @Test
    fun `Reports issue for hamcrest assertion api import`() {
        val findings = DeprecatedAssertionApiUsage().lint(
            """
                import org.hamcrest.MatcherAssert.assertThat
                
                class MyTest {
                
                }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
        assertThat(findings[0].message).isEqualTo("Hamcrest assertion Api usage.")
    }

    @Test
    fun `Don't reports issue for assertj assertion api import`() {
        val findings = DeprecatedAssertionApiUsage().lint(
            """
                import org.assertj.core.api.Assertions.assertThat   
                
                class MyTest {
                
                }
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }
}