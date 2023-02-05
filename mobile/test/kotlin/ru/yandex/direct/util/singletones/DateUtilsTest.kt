// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.util.singletones

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import ru.yandex.direct.util.singletones.DateUtils
import ru.yandex.direct.utils.date
import java.text.SimpleDateFormat
import java.util.*

class DateUtilsTest {
    //    June 2018
    // Mo     4 11 18 25
    // Tu     5 12 19 26
    // We     6 13 20 27
    // Th     7 14 21 28
    // Fr  1  8 15 22 29
    // Sa  2  9 16 23 30
    // Su  3 10 17 24

    @Test
    fun getNextMonday_shouldWorkCorrectly() {
        val testData = mapOf(
                "04-06-2018" to "11-06-2018",
                "05-06-2018" to "11-06-2018",
                "06-06-2018" to "11-06-2018",
                "07-06-2018" to "11-06-2018",
                "08-06-2018" to "11-06-2018",
                "09-06-2018" to "11-06-2018",
                "10-06-2018" to "11-06-2018",
                "11-06-2018" to "18-06-2018"
        )
        val softly = SoftAssertions()
        for ((input, output) in testData) {
            softly.assertThat(DateUtils.getNextMonday(date(input))).isEqualTo(date(output))
        }
        softly.assertAll()
    }

    @Test
    fun getNextMonthStart_shouldWorkCorrectly() {
        val testData = mapOf(
                "01-06-2018" to "01-07-2018",
                "15-06-2018" to "01-07-2018",
                "30-06-2018" to "01-07-2018",
                "01-07-2018" to "01-08-2018"
        )
        val softly = SoftAssertions()
        for ((input, output) in testData) {
            softly.assertThat(DateUtils.getNextMonthStart(date(input))).isEqualTo(date(output))
        }
        softly.assertAll()
    }

    @Test
    fun getNextQuarterStart_shouldWorkCorrectly() {
        // Q1 = {January, February, March}
        // Q2 = {April, May, June}
        // Q3 = {July, August, September}
        // Q4 = {October, November, December}
        val testData = mapOf(
                "29-06-2018" to "01-07-2018",
                "30-06-2018" to "01-07-2018",
                "01-07-2018" to "01-10-2018"
        )
        val softly = SoftAssertions()
        for ((input, output) in testData) {
            softly.assertThat(DateUtils.getNextQuarterStart(date(input))).isEqualTo(date(output))
        }
        softly.assertAll()
    }

    @Test
    fun getNextYearStart_shouldWorkCorrectly() {
        val testData = mapOf(
                "29-06-2018" to "01-01-2019",
                "31-12-2018" to "01-01-2019",
                "01-01-2019" to "01-01-2020",
                "31-12-2019" to "01-01-2020"
        )
        val softly = SoftAssertions()
        for ((input, output) in testData) {
            softly.assertThat(DateUtils.getNextYearStart(date(input))).isEqualTo(date(output))
        }
        softly.assertAll()
    }

    @Test
    fun roundToMidnight_shouldWorkCorrectly() {
        val testData = listOf(
                "27-06-2018 00:00:00.000",
                "27-06-2018 12:00:00.000",
                "27-06-2018 23:00:00.000",
                "27-06-2018 00:01:00.000",
                "27-06-2018 00:00:01.000",
                "27-06-2018 00:00:00.010",
                "27-06-2018 23:59:59.999"
        )
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS")
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val expectedDate = dateFormat.parse("27-06-2018 00:00:00.000")
        val softly = SoftAssertions()
        for (input in testData) {
            softly.assertThat(DateUtils.roundToMidnight(dateFormat.parse(input))).isEqualTo(expectedDate)
        }
        softly.assertAll()
    }
}
