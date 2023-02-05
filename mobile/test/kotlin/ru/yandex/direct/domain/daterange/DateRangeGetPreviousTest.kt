// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.domain.daterange

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.util.singletones.DateUtils
import ru.yandex.direct.web.report.request.DateRangeType
import java.util.*

@RunWith(Parameterized::class)
class DateRangeGetPreviousTest(private val source: DateRange, private val expected: DateRange) {
    companion object {
        private val today = DateUtils.getToday()
        private val yesterday = DateUtils.addDays(today, -1)
        private val tomorrow = DateUtils.addDays(today, 1)

        private val monday = DateUtils.addDays(yesterday, -2)
        private val tuesday = DateUtils.addDays(yesterday, -1)
        private val wednesday = yesterday
        private val thursday = today
        private val friday = tomorrow
        private val saturday = DateUtils.addDays(tomorrow, 1)
        private val sunday = DateUtils.addDays(tomorrow, 2)

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<DateRange>> {
            return listOf(
                    arrayOf(
                            range(thursday, friday), range(tuesday, wednesday)
                    ),
                    arrayOf(
                            range(thursday, saturday), range(monday, wednesday)
                    ),
                    arrayOf(
                            range(sunday, sunday), range(saturday, saturday)
                    ),
                    arrayOf(
                            range(DateRangeType.TODAY), range(yesterday, yesterday)
                    ),
                    arrayOf(
                            range(DateRangeType.YESTERDAY), range(tuesday, tuesday)
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_3_DAYS),
                            range(DateUtils.addDays(today, -3 * 2 - 1), DateUtils.addDays(today, -3 - 1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_5_DAYS),
                            range(DateUtils.addDays(today, -5 * 2 - 1), DateUtils.addDays(today, -5 - 1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_7_DAYS),
                            range(DateUtils.addDays(today, -7 * 2 - 1), DateUtils.addDays(today, -7 - 1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_14_DAYS),
                            range(DateUtils.addDays(today, -14 * 2 - 1), DateUtils.addDays(today, -14 - 1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_30_DAYS),
                            range(DateUtils.addDays(today, -30 * 2 - 1), DateUtils.addDays(today, -30 - 1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_90_DAYS),
                            range(DateUtils.addDays(today, -90 * 2 - 1), DateUtils.addDays(today, -90 - 1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_365_DAYS),
                            range(DateUtils.addDays(today, -365 * 2 - 1), DateUtils.addDays(today, -365 - 1))
                    ),
                    arrayOf(
                            range(DateRangeType.THIS_WEEK_MON_TODAY),
                            range(DateUtils.getLastWeekStart(), DateUtils.addWeeks(today, -1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_WEEK),
                            range(DateUtils.addWeeks(DateUtils.getLastWeekStart(), -1),
                                    DateUtils.addDays(DateUtils.getLastWeekStart(), -1))
                    ),
                    arrayOf(
                            range(DateRangeType.THIS_WEEK_SUN_TODAY),
                            range(DateUtils.getLastSunSatWeekStart(), DateUtils.addWeeks(today, -1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_BUSINESS_WEEK),
                            range(DateUtils.getBusinessWeekBeforeLastStart(), DateUtils.getBusinessWeekBeforeLastEnd())
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_WEEK_SUN_SAT),
                            range(DateUtils.addWeeks(DateUtils.getLastSunSatWeekStart(), -1),
                                    DateUtils.addDays(DateUtils.getLastSunSatWeekStart(), -1))
                    ),
                    arrayOf(
                            range(DateRangeType.THIS_MONTH),
                            range(DateUtils.getLastMonthStart(), DateUtils.addMonths(today, -1))
                    ),
                    arrayOf(
                            range(DateRangeType.LAST_MONTH),
                            range(DateUtils.addMonths(DateUtils.getLastMonthStart(), -1),
                                    DateUtils.addDays(DateUtils.getLastMonthStart(), -1))
                    )
            )
        }

        private fun range(type: DateRangeType) = DateRange.fromPreset(type)

        private fun range(start: Date, end: Date) = DateRange.fromExactDates(start, end)
    }

    @Test
    fun getPrevious_shouldReturnCorrectTimeIntervalInPast() {
        assertThat(source.previous)
                .isEqualToComparingOnlyGivenFields(expected, "mInclusiveStart", "mInclusiveEnd")
    }
}