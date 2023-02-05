package ru.yandex.telepathy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class MergeStrategyTest(
    @Suppress("unused") val testCaseName: String,
    val first: Map<String, Any?>,
    val second: Map<String, Any?>,
    val expected: Map<MergeStrategy, Map<String, Any?>>
) {
    @Test
    fun testMergeStrategy() {
        val softly = SoftAssertions()
        for (entry in expected) {
            val strategy = entry.key
            val expectedResult = entry.value
            val actualResult = strategy.merge(first, second)
            softly.assertThat(actualResult)
                .withFailMessage("Strategy: ${strategy.name}\nExpected: $expectedResult\nActual: $actualResult")
                .isEqualTo(expectedResult)
        }
        softly.assertAll()
    }

    companion object {
        private fun testCase(
            name: String,
            first: Map<String, Any?>,
            second: Map<String, Any?>,
            expected: List<Pair<MergeStrategy, Map<String, Any?>>>
        ): Array<Any> {
            return arrayOf(name, first, second, expected.toMap())
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun getParameters(): Iterable<Array<Any>> {
            return listOf(
                testCase(
                    name = "Both maps are empty",
                    first = emptyMap(),
                    second = emptyMap(),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to emptyMap(),
                        MergeStrategy.RewriteOnly to emptyMap(),
                        MergeStrategy.AppendOnly to emptyMap()
                    )
                ),
                testCase(
                    name = "First is singleton, second is empty",
                    first = mapOf("a" to "a"),
                    second = emptyMap(),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to "a"),
                        MergeStrategy.RewriteOnly to mapOf("a" to "a"),
                        MergeStrategy.AppendOnly to mapOf("a" to "a")
                    )
                ),
                testCase(
                    name = "First is empty, second is singleton",
                    first = emptyMap(),
                    second = mapOf("a" to "a"),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to "a"),
                        MergeStrategy.RewriteOnly to emptyMap(),
                        MergeStrategy.AppendOnly to mapOf("a" to "a")
                    )
                ),
                testCase(
                    name = "Both are singleton, second == first",
                    first = mapOf("a" to "a"),
                    second = mapOf("a" to "a"),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to "a"),
                        MergeStrategy.RewriteOnly to mapOf("a" to "a"),
                        MergeStrategy.AppendOnly to mapOf("a" to "a")
                    )
                ),
                testCase(
                    name = "Both are singleton, second != first",
                    first = mapOf("a" to "a"),
                    second = mapOf("a" to "b"),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to "b"),
                        MergeStrategy.RewriteOnly to mapOf("a" to "b"),
                        MergeStrategy.AppendOnly to mapOf("a" to "a")
                    )
                ),
                testCase(
                    name = "First is singleton, second > first, common values are equal",
                    first = mapOf("a" to "a"),
                    second = mapOf("a" to "a", "b" to "b"),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to "a", "b" to "b"),
                        MergeStrategy.RewriteOnly to mapOf("a" to "a"),
                        MergeStrategy.AppendOnly to mapOf("a" to "a", "b" to "b")
                    )
                ),
                testCase(
                    name = "First is singleton, second > first, common values aren't equal",
                    first = mapOf("a" to "a"),
                    second = mapOf("a" to "c", "b" to "b"),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to "c", "b" to "b"),
                        MergeStrategy.RewriteOnly to mapOf("a" to "c"),
                        MergeStrategy.AppendOnly to mapOf("a" to "a", "b" to "b")
                    )
                ),
                testCase(
                    name = "First is empty, second is nested",
                    first = emptyMap(),
                    second = mapOf("a" to mapOf("a" to "a")),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to mapOf("a" to "a")),
                        MergeStrategy.RewriteOnly to emptyMap(),
                        MergeStrategy.AppendOnly to mapOf("a" to mapOf("a" to "a"))
                    )
                ),
                testCase(
                    name = "First is nested, second is nested, collision on second level",
                    first = mapOf("a" to mapOf("a" to "a")),
                    second = mapOf("a" to mapOf("a" to "b", "c" to "c")),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to mapOf("a" to "b", "c" to "c")),
                        MergeStrategy.RewriteOnly to mapOf("a" to mapOf("a" to "b")),
                        MergeStrategy.AppendOnly to mapOf("a" to mapOf("a" to "a", "c" to "c"))
                    )
                ),
                testCase(
                    name = "First is nested, second is flat, collision on first level",
                    first = mapOf("a" to mapOf("a" to "a")),
                    second = mapOf("a" to "a"),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to "a"),
                        MergeStrategy.RewriteOnly to mapOf("a" to "a"),
                        MergeStrategy.AppendOnly to mapOf("a" to mapOf("a" to "a"))
                    )
                ),
                testCase(
                    name = "First is flat, second is nested, collision on second level",
                    first = mapOf("a" to "a"),
                    second = mapOf("a" to mapOf("a" to "a")),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to mapOf("a" to "a")),
                        MergeStrategy.RewriteOnly to mapOf("a" to mapOf("a" to "a")),
                        MergeStrategy.AppendOnly to mapOf("a" to "a")
                    )
                ),
                testCase(
                    name = "Swap object and value",
                    first = mapOf("a" to "a", "b" to mapOf("b" to "b")),
                    second = mapOf("a" to mapOf("b" to "b"), "b" to "a"),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf("a" to mapOf("b" to "b"), "b" to "a"),
                        MergeStrategy.RewriteOnly to mapOf("a" to mapOf("b" to "b"), "b" to "a"),
                        MergeStrategy.AppendOnly to mapOf("a" to "a", "b" to mapOf("b" to "b"))
                    )
                ),
                testCase(
                    name = "Merge two objects",
                    first = mapOf("a" to mapOf("a" to "a"), "b" to mapOf("b" to "b")),
                    second = mapOf("a" to mapOf("b" to "b"), "b" to mapOf("a" to "a")),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf(
                            "a" to mapOf("a" to "a", "b" to "b"),
                            "b" to mapOf("a" to "a", "b" to "b")
                        ),
                        MergeStrategy.RewriteOnly to mapOf(
                            "a" to mapOf("a" to "a"),
                            "b" to mapOf("b" to "b")
                        ),
                        MergeStrategy.AppendOnly to mapOf(
                            "a" to mapOf("a" to "a", "b" to "b"),
                            "b" to mapOf("a" to "a", "b" to "b")
                        )
                    )
                ),
                testCase(
                    name = "3-level nesting",
                    first = mapOf(
                        "a" to mapOf(
                            "a" to mapOf<String, Any>(
                                "a" to mapOf<String, Any>(
                                    "a" to "a"
                                )
                            )
                        )
                    ),
                    second = mapOf(
                        "a" to mapOf(
                            "a" to mapOf<String, Any>(
                                "a" to mapOf<String, Any>(
                                    "a" to "b",
                                    "b" to "b"
                                )
                            )
                        )
                    ),
                    expected = listOf(
                        MergeStrategy.RewriteAndAppend to mapOf(
                            "a" to mapOf(
                                "a" to mapOf<String, Any>(
                                    "a" to mapOf<String, Any>(
                                        "a" to "b",
                                        "b" to "b"
                                    )
                                )
                            )
                        ),
                        MergeStrategy.RewriteOnly to mapOf(
                            "a" to mapOf(
                                "a" to mapOf<String, Any>(
                                    "a" to mapOf<String, Any>(
                                        "a" to "b"
                                    )
                                )
                            )
                        ),
                        MergeStrategy.AppendOnly to mapOf(
                            "a" to mapOf(
                                "a" to mapOf<String, Any>(
                                    "a" to mapOf<String, Any>(
                                        "a" to "a",
                                        "b" to "b"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        }
    }
}
