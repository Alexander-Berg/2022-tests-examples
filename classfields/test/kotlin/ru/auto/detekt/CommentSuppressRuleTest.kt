package ru.auto.detekt

import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.core.spec.style.DescribeSpec
import org.assertj.core.api.Assertions.assertThat
import ru.auto.detekt.rules.CommentSuppressRule

/**
 * @author dumchev on 25/10/2018.
 */
class CommentSuppressRuleSpec : DescribeSpec({
    describe("CommentSuppress rule") {
        val rule = CommentSuppressRule()
        for ((element, suppressNoComment, suppressWithComment, otherAnnotation) in testData) {
            context("lint $element") {
                it("reports @Suppress with no comments") {
                    val findings = rule.lint(suppressNoComment)
                    assertThat(findings).hasSize(1)
                }
                it("does not report @Suppress with clarifying comment") {
                    val findings = rule.lint(suppressWithComment)
                    assertThat(findings).isEmpty()
                }
                it("does not report other annotations") {
                    val findings = rule.lint(otherAnnotation)
                    assertThat(findings).isEmpty()
                }
            }
        }
    }
}) {
    data class TestCase(
        val elementName: String,
        val suppressNoComment: String,
        val suppressWithComment: String,
        val otherAnnotation: String
    )
}


private val testData = listOf(
    CommentSuppressRuleSpec.TestCase(
        elementName = "top-level property",
        suppressNoComment = """
            @Suppress("MayBeConstant")
            val NOT_CONSTANT = 1
        """.trimIndent(),
        suppressWithComment = """
            @Suppress("MayBeConstant") // Hello isn't me you looking for
            val NOT_CONSTANT = 1
        """.trimIndent(),
        otherAnnotation = """
            @DimenRes
            val NOT_CONSTANT = 1
        """.trimIndent()
    ),
    CommentSuppressRuleSpec.TestCase(
        elementName = "top-level function",
        suppressNoComment = """
            @Suppress("SomeStupidRule")
            fun <T> T.toStringAlias() = toString()
        """.trimIndent(),
        suppressWithComment = """
            @Suppress("SomeStupidRule") // I am the programmer, trust me
            fun <T> T.toStringAlias() = toString()
        """.trimIndent(),
        otherAnnotation = """
            @Inject
            fun <T> T.toStringAlias() = toString()
        """.trimIndent()
    ),
    CommentSuppressRuleSpec.TestCase(
        elementName = "class",
        suppressNoComment = """
            @Suppress("SomeStupidRule")
            class AwesomeClass
        """.trimIndent(),
        suppressWithComment = """
            @Suppress("SomeStupidRule") // I am the programmer, trust me
            class AwesomeClass
        """.trimIndent(),
        otherAnnotation = """
            @Inject
            class AwesomeClass
        """.trimIndent()
    ),
    CommentSuppressRuleSpec.TestCase(
        elementName = "class method",
        suppressNoComment = """
            class AwesomeClass {
                @Suppress("SomeStupidRule")
                fun someMethod() = 1
            }
        """.trimIndent(),
        suppressWithComment = """
            class AwesomeClass {
                @Suppress("SomeStupidRule") // I am the programmer, trust me
                fun someMethod() = 1
            }
        """.trimIndent(),
        otherAnnotation = """
            class AwesomeClass {
                @Inject
                fun someMethod() = 1
            }
        """.trimIndent()
    ),
    CommentSuppressRuleSpec.TestCase(
        elementName = "expression",
        suppressNoComment = """
            fun <T> T.toStringAlias() {
                @Suppress("SomeStupidRule")
                println()
            }
        """.trimIndent(),
        suppressWithComment = """
            fun <T> T.toStringAlias() {
                @Suppress("SomeStupidRule") // I am the programmer, trust me
                println()
            }
        """.trimIndent(),
        otherAnnotation = """
            fun <T> T.toStringAlias() {
                @Inject
                println()
            }
        """.trimIndent()
    )
)
