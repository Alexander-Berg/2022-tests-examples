package ru.yandex.market.clean.data.mapper

import android.os.Build
import ru.yandex.market.test.matchers.ExceptionalMatchers.hasValueThat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.utils.createDate
import java.util.Date
import org.hamcrest.Matchers.equalTo
import ru.yandex.market.common.datetimeparser.DateTimeParser

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DateTimeParsingTest(
    private val input: String,
    private val expectedResult: Date
) {

    private val parser: DateTimeParser = DateTimeParser()

    @Test
    fun `Parse strings in supported formats to dates`() {
        val parsed = parser.parse(input)

        assertThat(parsed).`is`(
            HamcrestCondition(hasValueThat(equalTo(expectedResult)))
        )
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: parse({0}) == {1}")
        @JvmStatic
        fun data(): Iterable<Array<Any>> = listOf(
            arrayOf("25-09-2018 18:35:44", createDate(2018, 8, 25, 18, 35, 44)),
            arrayOf("2018-09-25", createDate(2018, 8, 25)),
            arrayOf("2019-03-22T16:50:32", createDate(2019, 2, 22, 16, 50, 32)),
            arrayOf("11-06-2021", createDate(2021, 5, 11))
        )
    }
}