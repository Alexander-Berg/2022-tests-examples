package ru.yandex.market.clean.domain.model

import com.annimon.stream.test.hamcrest.StreamMatcher.assertElements
import org.hamcrest.Matchers.contains
import org.junit.Test

class OpenHoursTest {

    @Test(expected = IllegalStateException::class)
    fun `Throws exception if start day greater than end day`() {
        OpenHours.builder()
            .startDay(WeekDay.TUESDAY)
            .endDay(WeekDay.MONDAY)
            .timeInterval(TimeInterval.testInstance())
            .build()
    }

    @Test
    fun `Returns all days between start and end day as stream`() {
        OpenHours.builder()
            .startDay(WeekDay.MONDAY)
            .endDay(WeekDay.SUNDAY)
            .build()
            .weekDaysStream()
            .custom(assertElements(contains(*WeekDay.values())))
    }
}