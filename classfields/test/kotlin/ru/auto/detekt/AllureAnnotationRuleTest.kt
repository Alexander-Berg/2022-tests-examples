package ru.auto.detekt

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import io.qameta.allure.kotlin.junit4.AllureRunner
import org.assertj.core.api.Assertions
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.detekt.rules.AllureAnnotationRule
import ru.auto.testextension.withPropertyValue

@RunWith(AllureRunner::class) class AllureAnnotationRuleTest {
    private val rule = AllureAnnotationRule()

    @Test
    fun shouldNotReportWhenClassHasNoTests() {
        @Language("Kotlin")
        val case = """
            class NotReallyATest {
                fun shouldDoSomething() {
                }
            }
        """.trimIndent()
        val findings = rule.lint(case)
        Assertions.assertThat(findings).isEmpty()
    }

    @Test
    fun shouldNotReportWhenClassHasAllureAnnotation() {
        @Language("Kotlin")
        val case = """
            @RunWith(AllureRunner::class)
            class SomeTest {
                @Test
                fun shouldDoSomething() {
                }
            }
        """.trimIndent()
        val findings = rule.lint(case)
        Assertions.assertThat(findings).isEmpty()
    }

    @Test
    fun shouldNotReportWhenClassHasAllureAndroidAnnotation() {
        @Language("Kotlin")
        val case = """
            @RunWith(AllureRobolectricRunner::class)
            class SomeTest {
                @Test
                fun shouldDoSomething() {
                }
            }
        """.trimIndent()
        val findings = rule.lint(case)
        Assertions.assertThat(findings).isEmpty()
    }

    @Test
    fun shouldReportWhenTestClassWithoutRunWith() {
        @Language("Kotlin")
        val case = """
            class SomeTest {
                @Test
                fun shouldDoSomething() {
                }
            }
        """.trimIndent()
        val findings = rule.lint(case)
        Assertions.assertThat(findings).hasSize(1).element(0)
            .has(withPropertyValue(Finding::signature, "Test.kt\$SomeTest"))
            .has(withPropertyValue(Finding::message, "Test class without @RunWith annotation"))
    }

    @Test
    fun shouldReportWhenTestClassWithNotAllowedRunner() {
        @Language("Kotlin")
        val case = """
            @RunWith(Parameterized::class)
            class SomeTest {
                @Test
                fun shouldDoSomething() {
                }
            }
        """.trimIndent()
        val findings = rule.lint(case)
        Assertions.assertThat(findings).hasSize(1).element(0)
            .has(withPropertyValue(Finding::signature, "Test.kt\$SomeTest\$@RunWith(Parameterized::class)"))
            .has(withPropertyValue(Finding::message, "@RunWith annotation have incorrect runner. " +
                "Use Allure runners. Valid runners are " +
                "[AllureRunner::class, AllureParametrizedRunner::class, AllureRobolectricRunner::class]"))
    }
}
