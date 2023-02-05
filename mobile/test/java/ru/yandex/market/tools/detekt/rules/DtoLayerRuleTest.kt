package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class DtoLayerRuleTest {

    @Test
    fun `Reports issue if class has SerializedName annotation and not in data layer`() {
        val findings = """
            
            package ru.yandex.market
            
            class SomeClass(
                @SerializedName("field") val field: String
            )
            
           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "DTO must be in data layer"
        )
    }

    @Test
    fun `Reports issue if class name endsWith Dto and not in data layer`() {
        val findings = """

            package ru.yandex.market

            class SomeDto(
                val field: String
            )

           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "DTO must be in data layer"
        )
    }

    @Test
    fun `Reports issue if class name endsWith DTO and not in data layer`() {
        val findings = """

            package ru.yandex.market

            class SomeDTO(
                val field: String
            )

           """.toFindings()

        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "DTO must be in data layer"
        )
    }

    @Test
    fun `Does not report issue if class is not DTO and not in data layer`() {
        val findings = """

            package ru.yandex.market

            class SomeClass(
                val field: String
            )

           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not report issue if class is DTO and in data layer`() {
        val findings = """

            package ru.yandex.market.clean.data

            class SomeDto(
                @SerializedName val field: String
            )

           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not report issue if class is DTO and in feature toggles layer`() {
        val findings = """

            package ru.yandex.market.common.featureconfigs.managers

            class SomeDto(
                @SerializedName val field: String
            )

           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Does not report issue if class is DTO and in data module`() {
        val findings = """

            package ru.yandex.market.data

            class SomeDto(
                @SerializedName val field: String
            )

           """.toFindings()

        Assertions.assertThat(findings).hasSize(0)
    }


    private fun String.toFindings(): List<Finding> = DtoLayerRule().lint(trimIndent())
}