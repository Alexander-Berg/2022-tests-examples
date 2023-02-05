// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.domain.daterange

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import ru.yandex.direct.util.singletones.DateUtils
import ru.yandex.direct.web.report.request.DateRangeType
import java.util.*

class DateRangeTest {
    @Test
    fun getExclusiveStart_shouldWorkCorrectly() {
        assertThat(DateRange.fromExactDates(today, tomorrow).exclusiveStart).isEqualTo(yesterday)
    }

    @Test
    fun getExclusiveEnd_shouldWorkCorrectly() {
        assertThat(DateRange.fromExactDates(today, tomorrow).exclusiveEnd).isEqualTo(dayAfterTomorrow)
    }

    @Test
    fun getDuration_shouldWorkCorrectly_inGenericCase() {
        val softly = SoftAssertions()
        softly.assertThat(DateRange.fromExactDates(today, today).duration).isEqualTo(Duration.days(1))
        softly.assertThat(DateRange.fromExactDates(today, tomorrow).duration).isEqualTo(Duration.days(2))
        softly.assertThat(DateRange.fromExactDates(yesterday, tomorrow).duration).isEqualTo(Duration.days(3))
        softly.assertThat(DateRange.fromExactDates(yesterday, dayAfterTomorrow).duration).isEqualTo(Duration.days(4))
        softly.assertAll()
    }

    @Test
    fun getDuration_shouldWorkCorrectly_forPresets() {
        val softly = SoftAssertions()
        softly.assertThat(DateRange.fromPreset(DateRangeType.TODAY).duration).isEqualTo(Duration.days(1))
        softly.assertThat(DateRange.fromPreset(DateRangeType.YESTERDAY).duration).isEqualTo(Duration.days(1))
        softly.assertThat(DateRange.fromPreset(DateRangeType.LAST_3_DAYS).duration).isEqualTo(Duration.days(4))
        softly.assertThat(DateRange.fromPreset(DateRangeType.LAST_5_DAYS).duration).isEqualTo(Duration.days(6))
        softly.assertThat(DateRange.fromPreset(DateRangeType.LAST_7_DAYS).duration).isEqualTo(Duration.days(8))
        softly.assertThat(DateRange.fromPreset(DateRangeType.LAST_14_DAYS).duration).isEqualTo(Duration.days(15))
        softly.assertThat(DateRange.fromPreset(DateRangeType.LAST_30_DAYS).duration).isEqualTo(Duration.days(31))
        softly.assertThat(DateRange.fromPreset(DateRangeType.LAST_90_DAYS).duration).isEqualTo(Duration.days(91))
        softly.assertThat(DateRange.fromPreset(DateRangeType.LAST_365_DAYS).duration).isEqualTo(Duration.days(366))
        softly.assertAll()
    }

    @Test
    fun thisWeek_shouldIncludeToday() {
        assertThat(DateRange.fromPreset(DateRangeType.THIS_WEEK_SUN_TODAY).inclusiveEnd).isEqualTo(DateUtils.getToday())
        assertThat(DateRange.fromPreset(DateRangeType.THIS_WEEK_MON_TODAY).inclusiveEnd).isEqualTo(DateUtils.getToday())
    }

    @Test
    fun thisMonth_shouldIncludeToday() {
        assertThat(DateRange.fromPreset(DateRangeType.THIS_MONTH).inclusiveEnd).isEqualTo(DateUtils.getToday())
    }

    @Test
    fun constructor_shouldThrow_ifStartAfterEnd() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            DateRange.fromExactDates(tomorrow, today)
        }
    }

    companion object {
        val yesterday = Date(1519239600000L)
        val today = Date(1519326000000L)
        val tomorrow = Date(1519412400000L)
        val dayAfterTomorrow = Date(1519498800000L)
    }
}