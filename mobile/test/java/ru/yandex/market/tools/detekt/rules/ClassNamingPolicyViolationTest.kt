package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class ClassNamingPolicyViolationTest {

    class Dto {

        @Test
        fun `Report error when using incorrect case in dto class`() {
            val findings = ClassNamingPolicyViolation().lint(
                """
                    package foo
                    data class ModelDTO(val value: String)
                    """.trimIndent()
            )
            Assertions.assertThat(findings).hasSize(1)
            Assertions.assertThat(findings[0].message).isEqualTo("Неверный суффикс у dto-шки, должно быть \"Dto\"!")
        }

        @Test
        fun `Expect clean when using upper case for dto suffix`() {
            val findings = ClassNamingPolicyViolation().lint(
                """
                    package foo
                    data class ModelDto(val value: String)
                    """.trimIndent()
            )
            Assertions.assertThat(findings).hasSize(0)
        }

        @Test
        fun `Expect clean when dto suffix is not present`() {
            val findings = ClassNamingPolicyViolation().lint(
                """
                        package foo
                        data class Model(val value: String)
                    """.trimIndent()
            )
            Assertions.assertThat(findings).hasSize(0)
        }

        @Test
        fun `Expect clean when only part of dto suffix is present`() {
            val findings = ClassNamingPolicyViolation().lint(
                """
                    package foo
                    data class ModelDt(val value: String)
                    """.trimIndent()
            )
            Assertions.assertThat(findings).hasSize(0)
        }
    }

    class Vo {

        @Test
        fun `Report error when using incorrect case in vo class`() {
            val findings = ClassNamingPolicyViolation().lint(
                """
                    package foo
                    data class ModelVO(val value: String)
                    """.trimIndent()
            )
            Assertions.assertThat(findings).hasSize(1)
            Assertions.assertThat(findings[0].message).isEqualTo("Неверный суффикс у vo-шки, должно быть \"Vo\"!")
        }

        @Test
        fun `Expect clean when using camel case for vo suffix`() {
            val findings = ClassNamingPolicyViolation().lint(
                """
                    package foo
                    data class ModelVo(val value: String)
                    """.trimIndent()
            )
            Assertions.assertThat(findings).hasSize(0)
        }

        @Test
        fun `Expect clean when vo suffix is not present`() {
            val findings = ClassNamingPolicyViolation().lint(
                """
                    package foo
                    data class Model(val value: String)
                    """.trimIndent()
            )
            Assertions.assertThat(findings).hasSize(0)
        }

        @Test
        fun `Expect clean when vo suffix is present in lower case`() {
            val findings = ClassNamingPolicyViolation().lint(
                """
                    package foo
                    data class Volvo(val value: String)
                    """.trimIndent()
            )
            Assertions.assertThat(findings).hasSize(0)
        }
    }
}