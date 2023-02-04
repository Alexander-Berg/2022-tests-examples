package ru.auto.detekt

import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.core.spec.style.DescribeSpec
import org.assertj.core.api.Assertions.assertThat
import ru.auto.detekt.rules.NullableFieldInApiEntityRule

class NullableFieldInApiEntitySpec : DescribeSpec({
    describe("NullableFieldInApiEntity rule") {
        val rule = NullableFieldInApiEntityRule()

        for ((name, code, numAsserts) in testCases) {
            it(name) {
                val findings = rule.lint(code)
                assertThat(findings).hasSize(numAsserts)
            }
        }
    }
}) {
    data class TestCase(
        val name: String,
        val code: String,
        val numAsserts: Int
    )
}

private val testCases = listOf(
    NullableFieldInApiEntitySpec.TestCase(
        name = "Data class with 2 nullable fields with no default",
        code = """
            @Serializable
            data class ApiEntity(
                val weight: Int = 0,
                val height: Int?,
                val width: Int?
            )""".trimIndent(),
        numAsserts = 2
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Data class with 2 nullable fields but only 1 has a default",
        code = """
            @Serializable
            data class ApiEntity(
                val weight: Int = 0,
                val height: Int? = null,
                val width: Int?
            )""".trimIndent(),
        numAsserts = 1
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Data class with 2 nullable fields with 2 defaults",
        code = """
            @Serializable
            data class ApiEntity(
                val weight: Int = 0,
                val height: Int? = null,
                val width: Int? = 0
            )""".trimIndent(),
        numAsserts = 0
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Data class with 1 inner nullable field with no default",
        code = """
            @Serializable
            data class ApiEntity(
                val weight: Int = 0,
                val height: Int? = null
            ) {
                lateinit var width: Int?
            }""".trimIndent(),
        numAsserts = 1
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Data class with 1 inner nullable field with a default",
        code = """
            @Serializable
            data class ApiEntity(
                val weight: Int = 0,
                val height: Int? = null
            ) {
                lateinit var width: Int? = null
            }
            """.trimIndent(),
        numAsserts = 0
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Data class with 1 nullable field with default binary expression",
        code = """
            @Serializable
            data class ApiEntity (
                val i: Int? = 1 + 3
            )
            """.trimIndent(),
        numAsserts = 0
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Data class with 1 nullable field with default expression",
        code = """
            @Serializable
            data class ApiEntity (
                val i: Int? = "123".toInt()
            )
            """.trimIndent(),
        numAsserts = 0
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Data class with 1 nullable field with default expression as class",
        code = """
            @Serializable
            data class ApiEntity (
                val i: String? = String()
            )
            """.trimIndent(),
        numAsserts = 0
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Ordinary class with 1 inner nullable field with no default",
        code = """
            @Serializable
            class ApiEntity {
                lateinit var width: Int?
            }
            """.trimIndent(),
        numAsserts = 0
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "Not a serialized class with 1 inner nullable field with no default",
        code = """
            data class ApiEntity(
                lateinit var width: Int?
            )
            """.trimIndent(),
        numAsserts = 0
    ),
    NullableFieldInApiEntitySpec.TestCase(
        name = "bug with Long",
        code = """
            @Serializable
            data class NWReloadParams(
                val allow_reload: Boolean? = null,
                val remaining_time_till_reload: Long? = null //after this time in millis we can reload
            )
        """.trimIndent(),
        numAsserts = 0
    )
)

