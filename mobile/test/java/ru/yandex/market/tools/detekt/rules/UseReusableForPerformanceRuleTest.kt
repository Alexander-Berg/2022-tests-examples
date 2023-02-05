package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class UseReusableForPerformanceRuleTest {

    @Test
    fun `Reports issue if stateless class does not use Reusable annotation`() {
        val findings = """
            
            class Sample @Inject constructor() {
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The class Sample is stateless. For best performance use Reusable annotation"
        )
    }

    @Test
    fun `Do not reports issue if stateless class without dependencies (not in Dagger)`() {
        val findings = """
            
            class Sample() {
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Do not reports issue if atateless class already Reusable`() {
        val findings = """
            
            @Reusable
            class Sample @Inject constructor() {
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Do not reports issue if atateless class already Singleton`() {
        val findings = """
            
            @Singleton
            class Sample @Inject constructor() {
            
            }
           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    private fun String.toFindings(): List<Finding> = UseReusableForPerformanceRule().lint(trimIndent())
}