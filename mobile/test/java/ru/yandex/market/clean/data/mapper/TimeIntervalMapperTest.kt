package ru.yandex.market.clean.data.mapper

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.utils.asExceptional
import ru.yandex.market.checkout.data.mapper.LocalTimeParser
import ru.yandex.market.common.LocalTime
import ru.yandex.market.clean.domain.model.TimeInterval

@RunWith(MockitoJUnitRunner::class)
class TimeIntervalMapperTest {

    private val localTimeParser = mock<LocalTimeParser>()
    private val mapper = TimeIntervalMapper(localTimeParser)

    @Test
    fun `Returns 23-59-59 when end time is midnight`() {
        val parsedStartTime = LocalTimeParser.Result.create(0, 0, 0)
        whenever(localTimeParser.parse("0")).thenReturn(parsedStartTime.asExceptional())

        val parsedEndTime = LocalTimeParser.Result.create(24, 0, 0)
        whenever(localTimeParser.parse("1")).thenReturn(parsedEndTime.asExceptional())

        val result = mapper.map("0", "1")

        val expected = TimeInterval.create(LocalTime.midnight(), LocalTime.dayEnd())
        assertThat(result).extracting { it.get() }.isEqualTo(expected)
    }

    @Test
    fun `Returns 00-00-00 when start time is 24-00-00`() {
        val parsedStartTime = LocalTimeParser.Result.create(24, 0, 0)
        whenever(localTimeParser.parse("0")).thenReturn(parsedStartTime.asExceptional())

        val parsedEndTime = LocalTimeParser.Result.create(7, 0, 0)
        whenever(localTimeParser.parse("1")).thenReturn(parsedEndTime.asExceptional())

        val result = mapper.map("0", "1")

        val expected = TimeInterval.create(LocalTime.midnight(), LocalTime.create(7, 0))
        assertThat(result).extracting { it.get() }.isEqualTo(expected)
    }
}
