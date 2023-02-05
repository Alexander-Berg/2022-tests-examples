package ru.yandex.market.clean.data.mapper

import com.annimon.stream.Stream
import com.annimon.stream.test.hamcrest.StreamMatcher.assertElements
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers.contains
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.test.extensions.errorExceptional
import ru.yandex.market.utils.asExceptional
import ru.yandex.market.clean.data.model.dto.OpenHoursDto
import ru.yandex.market.clean.domain.model.OpenHours
import ru.yandex.market.clean.domain.model.WeekDay

class WorkScheduleMapperTest {

    private val openHoursMapper = mock<OpenHoursMapper>()

    private val mapper = WorkScheduleMapper(openHoursMapper)

    @Test
    fun `Returns error if input is null`() {
        val result = mapper.map(null)
        assertThat(result).matches { it.exception != null }

    }

    @Test
    fun `Returns error if input is empty`() {
        val result = mapper.map(emptyList())
        assertThat(result).matches { it.exception != null }
    }

    @Test
    fun `Returns error if failed to match any open hours`() {
        whenever(openHoursMapper.map(any()))
            .thenReturn(errorExceptional())
            .thenReturn(errorExceptional())
        val input = listOf(OpenHoursDto.testBuilder().build(), OpenHoursDto.testBuilder().build())

        val result = mapper.map(input)

        assertThat(result).matches { it != null }
    }

    @Test
    fun `Appends missing not-working days`() {
        val openHours = OpenHours.builder()
            .startDay(WeekDay.TUESDAY)
            .endDay(WeekDay.SATURDAY)
            .build()
        whenever(openHoursMapper.map(any())).thenReturn(openHours.asExceptional())
        val input = listOf(OpenHoursDto.testBuilder().build())

        val result = mapper.map(input)

        Stream.of(result.get())
            .map { it.startDay() }
            .custom(assertElements(contains(WeekDay.MONDAY, WeekDay.TUESDAY, WeekDay.SUNDAY)))
    }

    @Test
    fun `Sorts open hours by start day`() {
        val one = OpenHours.builder()
            .startDay(WeekDay.FRIDAY)
            .endDay(WeekDay.SUNDAY)
            .build()
        val two = OpenHours.builder()
            .startDay(WeekDay.MONDAY)
            .endDay(WeekDay.THURSDAY)
            .build()
        whenever(openHoursMapper.map(any()))
            .thenReturn(one.asExceptional())
            .thenReturn(two.asExceptional())
        val input = listOf(OpenHoursDto.testBuilder().build(), OpenHoursDto.testBuilder().build())

        val result = mapper.map(input)

        Stream.of(result.get())
            .map { it.startDay() }
            .custom(assertElements(contains(WeekDay.MONDAY, WeekDay.FRIDAY)))
    }

    @Test
    fun `Appends nothing when open daily`() {
        val openHours = OpenHours.builder()
            .startDay(WeekDay.MONDAY)
            .endDay(WeekDay.SUNDAY)
            .build()
        whenever(openHoursMapper.map(any())).thenReturn(openHours.asExceptional())
        val input = listOf(OpenHoursDto.testBuilder().build())

        val result = mapper.map(input)

        assertThat(result).extracting { it.get().count() }.isEqualTo(1)
    }
}