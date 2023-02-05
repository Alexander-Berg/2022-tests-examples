package ru.yandex.market.tools.detekt.rules

import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions
import org.junit.Test

class NonNullAssertionOperatorTest {

    @Test
    fun `Expect clean when we don't use !! operator`() {
        val findings = NonNullAssertionOperator().lint(
            """
                fun main(args: Array<String>) {
                    testFunc("test!!")
                }

                fun testFunc(str: String) {
                    println(str)
                }
            """.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(0)
    }

    @Test
    fun `Expect warning when we use !! operator for parameter`() {
        val findings = NonNullAssertionOperator().lint(
            """
                fun main(args: Array<String>) {
                    println(testFunc("test!!"))
                }

                fun testFunc(str: String?) : String {
                    return str!!
                }
            """.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "Try not to use non-null assertion operator. Use 'requireNotNull(value) { ... }' instead!"
        )
    }

    @Test
    fun `Expect warning when we use !! operator for function result`() {
        val findings = NonNullAssertionOperator().lint(
            """
                fun main(args: Array<String>) {
                    println(testFunc("test!!")!!)
                }

                fun testFunc(str: String?) : String? {
                    return str
                }
            """.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "Try not to use non-null assertion operator. Use 'requireNotNull(value) { ... }' instead!"
        )
    }

    @Test
    fun `Expect warning when we use !! operator for 'it'`() {
        val findings = NonNullAssertionOperator().lint(
            """
                class Foo(val name: String)

                fun main() {
                    val list = listOf(Foo("Big"), null, Foo("Small"))
                    list.filter { it != null }.map { it!!.name.toUpperCase() }
                }
            """.trimIndent()
        )
        Assertions.assertThat(findings).hasSize(1)
        Assertions.assertThat(findings[0].message).isEqualTo(
            "Try not to use non-null assertion operator. Use 'requireNotNull(value) { ... }' instead!"
        )
    }
}