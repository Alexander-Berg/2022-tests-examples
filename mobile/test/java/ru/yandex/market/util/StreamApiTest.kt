package ru.yandex.market.util

import com.annimon.stream.Exceptional
import com.annimon.stream.Stream
import com.annimon.stream.test.hamcrest.StreamMatcher.assertElements
import com.annimon.stream.test.hamcrest.StreamMatcher.assertIsEmpty
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.utils.StreamApi

@RunWith(Enclosed::class)
class StreamApiTest {

    class SimpleTests {

        @Test
        fun `Skip errors`() {
            val item = Any()

            Stream.of(
                Exceptional.of(RuntimeException()),
                Exceptional.of { item },
                Exceptional.of(RuntimeException())
            )
                .custom(StreamApi.skipErrors())
                .custom(assertElements(contains(item)))
        }

        @Test
        fun `Skip until changed on empty stream returns empty stream`() {
            Stream.empty<String>()
                .custom(StreamApi.skipUntilChanged { it })
                .custom(assertIsEmpty())
        }

        @Test
        fun `Partition stream by predicate works as expected`() {
            val result = Stream.of(1, 2, 3, 4)
                .custom(StreamApi.partition { it < 3 })

            assertThat(result.first).contains(1, 2)
            assertThat(result.second).contains(3, 4)
        }

        @Test
        fun `selectInstanceOf selects only subclasses`() {
            val s: CharSequence = "1"
            val sb: CharSequence = StringBuilder("2")
            Stream.of<Any>(Object(), s, sb)
                .custom(StreamApi.selectInstanceOf(CharSequence::class.java))
                .custom(assertElements(contains(s, sb)))
        }
    }

    @RunWith(Parameterized::class)
    class SkipUntilChangedTest(
        private val input: Iterable<Pair<Int, String>>,
        private val expected: Array<Pair<Int, String>>
    ) {
        @Test
        fun `Skips elements until changes`() {
            Stream.of(input)
                .custom(StreamApi.skipUntilChanged { it.second })
                .custom(assertElements(contains(*expected)))
        }

        companion object {

            @Parameterized.Parameters
            @JvmStatic
            fun parameters(): Iterable<Array<*>> {
                return listOf(
                    arrayOf(
                        listOf(Pair(1, "a")),
                        arrayOf(Pair(1, "a"))
                    ),
                    arrayOf(
                        listOf(
                            Pair(1, "a"),
                            Pair(2, "b"),
                            Pair(3, "c"),
                            Pair(4, "c")
                        ),
                        arrayOf(Pair(1, "a"), Pair(2, "b"), Pair(4, "c"))
                    ),
                    arrayOf(
                        listOf(
                            Pair(1, "a"),
                            Pair(2, "a"),
                            Pair(3, "b"),
                            Pair(4, "b")
                        ),
                        arrayOf(Pair(2, "a"), Pair(4, "b"))
                    ),
                    arrayOf(
                        listOf(
                            Pair(1, "a"),
                            Pair(2, "a"),
                            Pair(3, "b"),
                            Pair(4, "b"),
                            Pair(5, "c")
                        ),
                        arrayOf(Pair(2, "a"), Pair(4, "b"), Pair(5, "c"))
                    ),
                    arrayOf(
                        listOf(
                            Pair(1, "a"),
                            Pair(2, "a"),
                            Pair(3, "a"),
                            Pair(4, "b"),
                            Pair(5, "b"),
                            Pair(6, "b"),
                            Pair(7, "c"),
                            Pair(8, "c"),
                            Pair(9, "c")
                        ),
                        arrayOf(Pair(3, "a"), Pair(6, "b"), Pair(9, "c"))
                    )
                )
            }
        }
    }
}