package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class DtoNullablePropertyTest {

    @Test
    fun `Reports issue if property has SerializedName and not null`() {
        val findings = """ 
            class SomeClass(
                @SerializedName("field") val field: String?,
                @SerializedName("field1") val field1: String,
                @SerializedName("field2") val field2: String
            )
           """.toFindings()

        Assertions.assertThat(findings).hasSize(2)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The property field1 must be nullable and haven't value default"
        )
        Assertions.assertThat(findings[1].message).isEqualTo(
            "The property field2 must be nullable and haven't value default"
        )
    }

    @Test
    fun `Don't report issue if property has SerializedName and enum type`() {
        val findings = """ 
            enum class SomeClass(
                @SerializedName("EMIT") 
                EMIT
            )
           """.toFindings()
        Assertions.assertThat(findings).isEmpty()
    }

    @Test
    fun `Don't report issue if property has SerializedName and collections`() {
        val findings = """ 
           class SomeClass(
                @SerializedName("field") val field: List<String>
            )
           """.toFindings()
        Assertions.assertThat(findings).isEmpty()
    }

    @Test
    fun `Report issue if property has SerializedName and collections have default value`() {
        val findings = """ 
            class SomeClass(
                @SerializedName("field") val field1: List<String> = emptyList()
            )
           """.toFindings()
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The property field1 must be nullable and haven't value default"
        )
    }


    @Test
    fun `Report issue if property has SerializedName and hasn't default value`() {
        val findings = """ 
            class SomeClass(
                @SerializedName("EMIT") 
                val field:String? = ""
            )
           """.toFindings()
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The property field must be nullable and haven't value default"
        )
    }

    @Test
    fun `Report issue if property has SerializedName and class havn't interface FAPI parameters`() {
        val findings = """ 
            class SomeClass(
                @SerializedName("field") val field:String? = ""
            ):SomeInterface
           """.toFindings()
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "The property field must be nullable and haven't value default"
        )
    }

    @Test
    fun `Don't report issue if property has not SerializedName`() {
        val findings = """
            class SomeClass(
                val field: String?,
                val field1: String
            )
           """.toFindings()

        Assertions.assertThat(findings).isEmpty()
    }

    @Test
    fun `Don't report issue if class have interface FAPI parameters `() {
        val findings = """
            class SomeClass(
               @SerializedName("field") val field: String,
               @SerializedName(field1) val field1: String? = null
            ):SomeSuperClass(),FrontApiResolverParameters,SomeInterface
           """.toFindings()

        Assertions.assertThat(findings).isEmpty()
    }

    private fun String.toFindings(): List<Finding> = DtoNullableProperty().lint(trimIndent())
}