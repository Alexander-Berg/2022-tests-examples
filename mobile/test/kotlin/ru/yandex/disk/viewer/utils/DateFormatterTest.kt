package ru.yandex.disk.viewer.utils

import android.content.res.Resources
import android.text.format.DateUtils
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.hamcrest.core.IsEqual.equalTo
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.robolectric.annotation.Config
import ru.yandex.disk.test.TestCase2
import ru.yandex.disk.util.SystemClock
import ru.yandex.disk.util.withTz
import ru.yandex.disk.utils.FixedSystemClock
import ru.yandex.disk.viewer.R
import ru.yandex.disk.viewer.util.DateTimeFormatter
import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.util.*


@Ignore("MOBDISK-20597")
@Config(manifest = Config.NONE)
class DateFormatterTest : TestCase2() {
    private val resources = mock<Resources> {
        on(it.getString(R.string.today)) doReturn "Сегодня"
        on(it.getString(R.string.yesterday)) doReturn "Вчера"
        on(it.getString(R.string.week_day_monday)) doReturn "Понедельник"
        on(it.getString(R.string.week_day_tuesday)) doReturn "Вторник"
        on(it.getString(R.string.week_day_wednesday)) doReturn "Среда"
        on(it.getString(R.string.week_day_thursday)) doReturn "Четверг"
        on(it.getString(R.string.week_day_friday)) doReturn "Пятница"
        on(it.getString(R.string.week_day_saturday)) doReturn "Суббота"
        on(it.getString(R.string.week_day_sunday)) doReturn "Воскресенье"
    }

    private val NOW = 1571828000000L //23.10.2019 13:53:20 GMT+0300
    private val TODAY = 1571778000000L //23.10.2019
    private val YESTERDAY = 1571691600000L //22.10.2019
    private val ANOTHER_DATE = 1514851200000L //02.01.2018
    private val JANUARY_THE_FIRST = 1546290000000L //01.01.2019
    private val DECEMBER_31 = 1546203600000L //31.12.2018

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+0300"))
        Locale.setDefault(Locale.forLanguageTag("ru"))
    }

    @After
    public override fun tearDown() {
        super.tearDown()
        DateTimeFormatter.setSystemClockForTest(SystemClock.REAL)
    }

    @Test
    fun `should be today string`() {
        setUpMockSystemClock(NOW)
        val format = DateTimeFormatter.format(resources, TODAY)
        assertThat(format, equalTo("Сегодня"))
    }

    @Test
    fun `should be yesterday string`() {
        setUpMockSystemClock(NOW)
        val format = DateTimeFormatter.format(resources, YESTERDAY)
        assertThat(format, equalTo("Вчера"))
    }

    @Test
    fun `should be yesterday string when different years`() {
        setUpMockSystemClock(JANUARY_THE_FIRST)
        val format = DateTimeFormatter.format(resources, DECEMBER_31)
        assertThat(format, equalTo("Вчера"))
    }

    @Test
    fun `should format another day`() {
        setUpMockSystemClock(NOW)
        val format = DateTimeFormatter.format(resources, ANOTHER_DATE)
        assertThat(format, equalTo("2 января"))
    }

    @Test
    fun `should format year`() {
        val format = DateTimeFormatter.formatYear(ANOTHER_DATE)
        assertThat(format, equalTo("2018"))
    }

    @Test
    fun `should format vista section`() {
        val format = DateTimeFormatter.formatVistaSection(ANOTHER_DATE)
        assertThat(format, equalTo("Январь"))
    }

    @Test
    fun `should count days from epoch`() {
        withTz(TimeZone.getTimeZone("Europe/Kiev")) {

            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm")
            fun days(date: String) = DateTimeFormatter.daysFromEpoch(fmt.parse(date).time)

            val standardEve = days("2019-03-30 23:00")
            val standardMidnight = days("2019-03-31 00:00")
            val standardOne = days("2019-03-31 01:00")

            val daylightEve = days("2019-03-31 23:00")
            val daylightMidnight = days("2019-04-01 00:00")
            val daylightOne = days("2019-04-01 01:00")

            assertThat(standardEve, equalTo(standardMidnight - 1))
            assertThat(standardMidnight, equalTo(standardOne))

            assertThat(standardOne, equalTo(daylightEve))

            assertThat(daylightEve, equalTo(daylightMidnight - 1))
            assertThat(daylightMidnight, equalTo(daylightOne))

            assertThat(days("2020-04-01 00:00"), equalTo(daylightOne + 366))
        }
    }

    @Test
    fun `should the correct day of the week`() {
        setUpMockSystemClock(NOW)
        val today = formatWithDayOfWeek(getTimeFromDate(23, 10, 2019))
        val yesterday = formatWithDayOfWeek(getTimeFromDate(22, 10, 2019))
        val saturday = formatWithDayOfWeek(getTimeFromDate(21, 10, 2019))
        val friday = formatWithDayOfWeek(getTimeFromDate(20, 10, 2019))
        val thursday = formatWithDayOfWeek(getTimeFromDate(19, 10, 2019))
        val wednesday = formatWithDayOfWeek(getTimeFromDate(18, 10, 2019))
        val tuesday = formatWithDayOfWeek(getTimeFromDate(17, 10, 2019))
        val date = formatWithDayOfWeek(getTimeFromDate(16, 10, 2019))

        assertThat(today, equalTo("Сегодня"))
        assertThat(yesterday, equalTo("Вчера"))
        assertThat(saturday, equalTo("Понедельник"))
        assertThat(friday, equalTo("Воскресенье"))
        assertThat(thursday, equalTo("Суббота"))
        assertThat(wednesday, equalTo("Пятница"))
        assertThat(tuesday, equalTo("Четверг"))
        assertThat(date, equalTo("16 октября"))
    }

    @Test
    fun `should the correct format range dates`() {
        setUpMockSystemClock(NOW)
        val thursday_sunday = DateTimeFormatter.formatWithDayOfWeek(resources,
            getTimeFromDate(17, 10, 2019), getTimeFromDate(20, 10, 2019))
        val friday_saturday = DateTimeFormatter.formatWithDayOfWeek(resources,
            getTimeFromDate(18, 10, 2019), getTimeFromDate(19, 10, 2019))
        val monday_wednesday = DateTimeFormatter.formatWithDayOfWeek(resources,
            getTimeFromDate(21, 10, 2019), getTimeFromDate(23, 10, 2019))
        val date_19_22 = DateTimeFormatter.formatWithDayOfWeek(resources,
            getTimeFromDate(19, 10, 2019), getTimeFromDate(22, 10, 2019))
        val date_20_21 = DateTimeFormatter.formatWithDayOfWeek(resources,
            getTimeFromDate(20, 10, 2019), getTimeFromDate(21, 10, 2019))
        val date_16_19 = DateTimeFormatter.formatWithDayOfWeek(resources,
            getTimeFromDate(16, 10, 2019), getTimeFromDate(19, 10, 2019))
        val date_01_03 = DateTimeFormatter.formatWithDayOfWeek(resources,
            getTimeFromDate(1, 3, 2019), getTimeFromDate(3, 3, 2019))

        assertThat(thursday_sunday, equalTo("Четверг – Воскресенье"))
        assertThat(friday_saturday, equalTo("Пятница – Суббота"))
        assertThat(monday_wednesday, equalTo("Понедельник – Среда"))
        assertThat(date_19_22, equalTo("19 октября – 22 октября"))
        assertThat(date_20_21, equalTo("20 октября – 21 октября"))
        assertThat(date_16_19, equalTo("16 октября – 19 октября"))
        assertThat(date_01_03, equalTo("1 марта – 3 марта"))
    }

    private fun formatWithDayOfWeek(time: Long): String {
        return DateTimeFormatter.formatWithDayOfWeek(resources, time)
    }

    private fun getTimeFromDate(day: Int, month: Int, year: Int): Long {
         return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.MONTH, month-1)
            set(Calendar.YEAR, year)
        }.timeInMillis
    }

    private fun setUpMockSystemClock(time: Long) {
        val systemFixedClock = FixedSystemClock(time)
        DateTimeFormatter.setSystemClockForTest(systemFixedClock)
    }
}
