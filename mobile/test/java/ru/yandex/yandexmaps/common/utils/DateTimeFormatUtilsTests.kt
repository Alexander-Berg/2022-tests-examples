package ru.yandex.yandexmaps.common.utils

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.yandex.yandexmaps.common.R
import ru.yandex.yandexmaps.common.utils.DateTimeFormatUtils
import ru.yandex.yandexmaps.common.utils.extensions.quantityString
import ru.yandex.yandexmaps.mapsstrings.Plurals
import ru.yandex.yandexmaps.mapsstrings.Strings
import java.text.SimpleDateFormat

@RunWith(RobolectricTestRunner::class)
class DateTimeFormatUtilsTests {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val dateTimeFormatUtils = DateTimeFormatUtils(context)
    private val iso8601DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    @Test
    fun formatDateTimeAgo_pastAfterPresent_returnsEmpty() {
        assert("2018-10-19T11:00:00.000+0300", "2018-10-19T10:59:00.000+0300", "")
    }

    @Test
    fun formatDateTimeAgo_pastEqualsPresent_returnsJustNow() {
        assert("2018-10-19T11:00:00.000+0300", "2018-10-19T11:00:00.000+0300", context.getString(Strings.common_date_time_now))
    }

    @Test
    fun formatDateTimeAgo_lessThanMinuteDiff_returnsJustNow() {
        assert("2018-10-19T11:00:00.000+0300", "2018-10-19T11:00:59.000+0300", context.getString(Strings.common_date_time_now))
    }

    @Test
    fun formatDateTimeAgo_exactlyOneMinuteDiff_returnsMinuteAgo() {
        assert("2018-10-19T11:00:00.000+0300", "2018-10-19T11:01:00.000+0300", context.getString(Strings.common_date_time_minute_ago))
    }

    @Test
    fun formatDateTimeAgo_oneMinuteDiff_returnsMinuteAgo() {
        assert("2018-10-19T11:00:10.000+0300", "2018-10-19T11:01:30.000+0300", context.getString(Strings.common_date_time_minute_ago))
    }

    @Test
    fun formatDateTimeAgo_lessThanAnHourAgo_returnsNMinutesAgo() {
        assert("2018-10-19T11:10:10.000+0300", "2018-10-19T11:25:30.000+0300", context.quantityString(Plurals.common_date_time_n_minutes_ago, 15, 15))
    }

    @Test
    fun formatDateTimeAgo_exactlyOneHourAgo_returnsAnHourAgo() {
        assert("2018-10-19T11:10:10.000+0300", "2018-10-19T12:10:10.000+0300", context.getString(Strings.common_date_time_an_hour_ago))
    }

    @Test
    fun formatDateTimeAgo_oneHourAgo_returnsAnHourAgo() {
        assert("2018-10-19T11:10:10.000+0300", "2018-10-19T12:30:25.000+0300", context.getString(Strings.common_date_time_an_hour_ago))
    }

    @Test
    fun formatDateTimeAgo_todayButMoreThanAnHour_returnsToday() {
        assert("2018-10-19T11:10:10.000+0300", "2018-10-19T19:30:25.000+0300", context.getString(Strings.common_date_time_today))
    }

    @Test
    fun formatDateTimeAgo_yesterdayButMoreThanAnHour_returnsYesterday() {
        assert("2018-10-18T23:59:59.000+0300", "2018-10-19T03:30:25.000+0300", context.getString(Strings.common_date_time_yesterday))
    }

    @Test
    fun formatDateTimeAgo_NDaysAgo_returnsNDaysAgo() {
        assert("2018-10-17T23:59:59.000+0300", "2018-10-19T03:30:25.000+0300", context.quantityString(Plurals.common_date_time_n_days_ago, 2, 2))
    }

    @Test
    fun formatDateTimeAgo_weekAgo_returnsWeekAgo() {
        assert("2018-10-12T23:59:59.000+0300", "2018-10-19T03:30:25.000+0300", context.getString(Strings.common_date_time_a_week_ago))
    }

    @Test
    fun formatDateTimeAgo_sameYearButMoreThanAWeek_returnsDayOfMonth() {
        assert("2018-09-12T21:59:59.000+0300", "2018-10-19T03:30:25.000+0300", context.getString(Strings.common_date_time_day_in_september, 12))
    }

    @Test
    fun formatDateTimeAgo_yearAgo_returnsDayOfMonthAndYear() {
        assert("2017-08-12T21:59:59.000+0300", "2018-10-19T03:30:25.000+0300", "${context.getString(Strings.common_date_time_day_in_august, 12)} 2017")
    }

    private fun assert(pastDateIso8601: String, presentDateIso8601: String, expected: String) {
        val pastDate = iso8601DateFormat.parse(pastDateIso8601)!!
        val presentDate = iso8601DateFormat.parse(presentDateIso8601)!!
        Assert.assertEquals(expected, dateTimeFormatUtils.formatDateTimeAgo(pastDate, presentDate))
    }
}
