// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.domain.statistics

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import ru.yandex.direct.domain.daterange.DateRange
import ru.yandex.direct.utils.date
import ru.yandex.direct.web.report.request.DateRangeType

class GroupingTest {
    @Test
    fun getBestGroupingForDateRange_shouldWork() {
        val testData = mapOf(
                DateRangeType.TODAY             to      Grouping.DATE,
                DateRangeType.LAST_3_DAYS       to      Grouping.DATE,
                DateRangeType.LAST_5_DAYS       to      Grouping.DATE,
                DateRangeType.LAST_7_DAYS       to      Grouping.DATE,
                DateRangeType.LAST_14_DAYS      to      Grouping.DATE,
                DateRangeType.LAST_30_DAYS      to      Grouping.DATE,
                DateRangeType.LAST_90_DAYS      to      Grouping.WEEK,
                DateRangeType.LAST_365_DAYS     to      Grouping.MONTH,
                DateRangeType.ALL_TIME          to      Grouping.QUARTER
        )
        val softly = SoftAssertions()
        for ((input, output) in testData) {
            softly.assertThat(bestGroupingFor(input)).isEqualTo(output)
        }
        softly.assertAll()
    }

    private fun bestGroupingFor(type: DateRangeType) = Grouping.getBestGroupingForDateRange(DateRange.fromPreset(type))

    @Test
    fun sliceUp_shouldSliceOnDays_withDateGrouping() {
        val testData = mapOf(
                ("01-06-2018" to "03-06-2018")
                        to listOf("01-06-2018", "02-06-2018", "03-06-2018"),
                ("31-05-2018" to "02-06-2018")
                        to listOf("31-05-2018", "01-06-2018", "02-06-2018"),
                ("01-06-2018" to "01-06-2018")
                        to listOf("01-06-2018")
        )
        runSlicingTest(testData, Grouping.DATE)
    }

    @Test
    fun sliceUp_shouldSliceOnWeeks_withWeekGrouping() {
        //    June 2018
        // Mo     4 11 18 25
        // Tu     5 12 19 26
        // We     6 13 20 27
        // Th     7 14 21 28
        // Fr  1  8 15 22 29
        // Sa  2  9 16 23 30
        // Su  3 10 17 24

        val testData = mapOf(
                ("01-06-2018" to "03-06-2018")
                        to listOf("01-06-2018"),
                ("01-06-2018" to "04-06-2018")
                        to listOf("01-06-2018", "04-06-2018"),
                ("05-06-2018" to "26-06-2018")
                        to listOf("05-06-2018", "11-06-2018", "18-06-2018", "25-06-2018")
        )
        runSlicingTest(testData, Grouping.WEEK)
    }

    @Test
    fun sliceUp_shouldSliceOnMonths_withMonthGrouping() {
        val testData = mapOf(
                ("01-06-2018" to "03-06-2018")
                        to listOf("01-06-2018"),
                ("31-05-2018" to "02-06-2018")
                        to listOf("31-05-2018", "01-06-2018"),
                ("01-07-2018" to "31-08-2018")
                        to listOf("01-07-2018", "01-08-2018"),
                ("01-07-2018" to "01-09-2018")
                        to listOf("01-07-2018", "01-08-2018", "01-09-2018"),
                ("24-06-2018" to "24-09-2018")
                        to listOf("24-06-2018", "01-07-2018", "01-08-2018", "01-09-2018")
        )
        runSlicingTest(testData, Grouping.MONTH)
    }

    @Test
    fun sliceUp_shouldSliceOnQuarters_withQuarterGrouping() {
        val testData = mapOf(
                ("01-06-2018" to "03-06-2018")
                        to listOf("01-06-2018"),
                ("31-05-2018" to "02-06-2018")
                        to listOf("31-05-2018"),
                ("24-06-2018" to "24-12-2018")
                        to listOf("24-06-2018", "01-07-2018", "01-10-2018"),
                ("01-07-2018" to "24-12-2018")
                        to listOf("01-07-2018", "01-10-2018")
        )
        runSlicingTest(testData, Grouping.QUARTER)
    }

    @Test
        fun sliceUp_shouldSliceOnYears_withYearGrouping() {
        val testData = mapOf(
                ("01-06-2018" to "03-06-2018")
                        to listOf("01-06-2018"),
                ("31-05-2018" to "02-06-2018")
                        to listOf("31-05-2018"),
                ("24-06-2018" to "24-06-2019")
                        to listOf("24-06-2018", "01-01-2019"),
                ("01-01-2018" to "24-06-2020")
                        to listOf("01-01-2018", "01-01-2019", "01-01-2020")
        )
        runSlicingTest(testData, Grouping.YEAR)
    }

    private fun runSlicingTest(testData: Map<Pair<String, String>, List<String>>, grouping: Grouping) {
        val softly = SoftAssertions()
        for ((input, output) in testData) {
            val dateRange = DateRange.fromExactDates(date(input.first), date(input.second))
            val slicing = output.map { date(it) }
            softly.assertThat(grouping.sliceUp(dateRange)).isEqualTo(slicing)
        }
        softly.assertAll()
    }
}