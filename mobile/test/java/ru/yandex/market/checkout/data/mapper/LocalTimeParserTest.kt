package ru.yandex.market.checkout.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.test.matchers.ExceptionalMatchers.containsError
import ru.yandex.market.test.matchers.ExceptionalMatchers.hasValueThat
import java.util.*
import org.hamcrest.Matchers.equalTo

@RunWith(Enclosed::class)
class LocalTimeParserTest {

    @RunWith(Parameterized::class)
    class CommonCases(
        private val input: String,
        private val expected: LocalTimeParser.Result
    ) {

        val mapper = LocalTimeParser()

        @Test
        fun `Returns parsed value for common cases as expected`() {
            val actual = mapper.parse(input)
            assertThat(actual).`is`(
                HamcrestCondition(hasValueThat(equalTo(expected)))
            )
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
            @JvmStatic
            fun data(): Iterable<Array<*>> {
                return Arrays.asList(
                    arrayOf(
                        "00:00:00",
                        LocalTimeParser.Result.create(0, 0, 0)
                    ),
                    arrayOf(
                        "13:54:11",
                        LocalTimeParser.Result.create(13, 54, 11)
                    ),
                    arrayOf(
                        "21:08",
                        LocalTimeParser.Result.create(21, 8, 0)
                    ),
                    arrayOf(
                        "11",
                        LocalTimeParser.Result.create(11, 0, 0)
                    ),
                    arrayOf(
                        "11:11:11:12",
                        LocalTimeParser.Result.create(11, 11, 11)
                    ),
                    arrayOf(
                        "8:00:00",
                        LocalTimeParser.Result.create(8, 0, 0)
                    ),
                    arrayOf(
                        "8",
                        LocalTimeParser.Result.create(8, 0, 0)
                    ),
                    arrayOf(
                        "0008:00:00",
                        LocalTimeParser.Result.create(8, 0, 0)
                    ),
                    arrayOf(
                        "24:00:00",
                        LocalTimeParser.Result.create(24, 0, 0)
                    ),
                    arrayOf(
                        "00:60:00",
                        LocalTimeParser.Result.create(0, 60, 0)
                    ),
                    arrayOf(
                        "00:00:60",
                        LocalTimeParser.Result.create(0, 0, 60)
                    ),
                    arrayOf(
                        "123:12:01",
                        LocalTimeParser.Result.create(123, 12, 1)
                    )
                )
            }
        }
    }

    @RunWith(Parameterized::class)
    class ErrorCases(private val input: String?) {

        val mapper = LocalTimeParser()

        @Test
        fun `Returns error for incorrect input`() {
            val exceptional = mapper.parse(input)
            assertThat(exceptional).`is`(HamcrestCondition(containsError()))
        }

        companion object {

            @Parameterized.Parameters(name = "{index} -> {0}")
            @JvmStatic
            fun data(): Iterable<String?> {
                return listOf(null, "ab:cd:ef", "")
            }
        }
    }
}