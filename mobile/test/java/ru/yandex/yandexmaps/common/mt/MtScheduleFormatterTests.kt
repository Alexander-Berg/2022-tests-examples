package ru.yandex.yandexmaps.common.mt

import android.app.Activity
import android.content.res.Resources
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Test
import ru.yandex.yandexmaps.mapsstrings.Plurals
import ru.yandex.yandexmaps.mapsstrings.Strings
import java.util.Locale
import kotlin.time.Duration

class MtScheduleFormatterTests {

    private val formatter: MtScheduleFormatter

    init {
        val activityMock = mock<Activity>()
        val resourcesMock = mock<Resources>()
        whenever(activityMock.resources).thenReturn(resourcesMock)
        whenever(activityMock.getString(eq(Strings.common_time_frequency_hour_and_minutes_format), any<Int>(), any<Int>())).thenAnswer {
            HOURS_MINS.format(Locale.getDefault(), it.getArgument(1), it.getArgument(2))
        }
        whenever(resourcesMock.getQuantityString(eq(Plurals.common_time_frequency_minutes_format), any(), any<Int>())).thenAnswer {
            MINUTES.format(Locale.getDefault(), it.getArgument(2))
        }
        whenever(resourcesMock.getQuantityString(eq(Plurals.common_time_frequency_hours_format), any(), any<Int>())).thenAnswer {
            HOURS.format(Locale.getDefault(), it.getArgument(2))
        }
        whenever(resourcesMock.getQuantityString(eq(Plurals.common_time_frequency_days_format), any(), any<Int>())).thenAnswer {
            DAYS.format(Locale.getDefault(), it.getArgument(2))
        }
        formatter = MtScheduleFormatter(activityMock)
    }

    @Test
    fun `formatScheduleIntervalFrequency less than one minute`() {
        Assert.assertEquals(MINUTES.format(1), formatter.formatScheduleIntervalFrequency(55))
    }

    @Test
    fun `formatScheduleIntervalFrequency more than one minute`() {
        Assert.assertEquals(MINUTES.format(1), formatter.formatScheduleIntervalFrequency(80))
    }

    @Test
    fun `formatScheduleIntervalFrequency many minutes`() {
        Assert.assertEquals(MINUTES.format(54), formatter.formatScheduleIntervalFrequency(Duration.minutes(54).inWholeSeconds))
    }

    @Test
    fun `formatScheduleIntervalFrequency one hour`() {
        Assert.assertEquals(HOURS.format(1), formatter.formatScheduleIntervalFrequency(Duration.hours(1).inWholeSeconds))
    }

    @Test
    fun `formatScheduleIntervalFrequency many hours`() {
        Assert.assertEquals(HOURS.format(12), formatter.formatScheduleIntervalFrequency(Duration.hours(12).inWholeSeconds + 20))
    }

    @Test
    fun `formatScheduleIntervalFrequency one hour and minutes`() {
        Assert.assertEquals(HOURS_MINS.format(1, 12), formatter.formatScheduleIntervalFrequency(Duration.hours(1).inWholeSeconds + Duration.minutes(12).inWholeSeconds))
    }

    @Test
    fun `formatScheduleIntervalFrequency many hours and minutes`() {
        Assert.assertEquals(HOURS_MINS.format(15, 25), formatter.formatScheduleIntervalFrequency(Duration.hours(15).inWholeSeconds + Duration.minutes(25).inWholeSeconds))
    }

    @Test
    fun `formatScheduleIntervalFrequency one day`() {
        Assert.assertEquals(DAYS.format(1), formatter.formatScheduleIntervalFrequency(Duration.days(1).inWholeSeconds + Duration.hours(5).inWholeSeconds))
    }

    @Test
    fun `formatScheduleIntervalFrequency many days`() {
        Assert.assertEquals(DAYS.format(22), formatter.formatScheduleIntervalFrequency(Duration.days(22).inWholeSeconds))
    }

    companion object {
        private const val HOURS_MINS = "каждые %d ч %d мин"
        private const val MINUTES = "каждые %d мин"
        private const val HOURS = "каждые %d ч"
        private const val DAYS = "каждые %d д"
    }
}
