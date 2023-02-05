package ru.yandex.yandexbus.inhouse.route.routesetup

import android.content.res.Resources
import com.yandex.mapkit.Time
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import ru.yandex.yandexbus.inhouse.R
import ru.yandex.yandexbus.inhouse.model.alert.Closed
import ru.yandex.yandexbus.inhouse.utils.datetime.DateTime
import ru.yandex.yandexbus.inhouse.utils.util.AlertUtils
import ru.yandex.yandexbus.inhouse.whenever
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class AlertTextTest {

    private lateinit var resources: Resources
    private lateinit var now: DateTime

    private lateinit var originalLocale: Locale
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        resources = Mockito.mock(Resources::class.java)

        // 01/14/2018 @ 01:00pm
        now = DateTime(1515884400_000L)

        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()

        Locale.setDefault(Locale.ENGLISH)

        val correctTimeZone = TimeZone.getTimeZone(
            TimeZone.getAvailableIDs(TimeUnit.SECONDS.toMillis(TIMEZONE_OFFSET.toLong()).toInt())[0]
        )
        TimeZone.setDefault(correctTimeZone)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun closedToday() {
        fun closedToday(closerTodayText: String) {
            whenever(resources.getString(eq(R.string.alert_closed_today))).thenReturn(closerTodayText)

            // 01/13/2018 @ 11:00pm (UTC) in seconds
            val value = 1515884400L

            // 01/14/2018 @ 01:00pm (UTC+2)
            val time = Time(value, TIMEZONE_OFFSET, "")
            val alert = Closed("unexpected text", time)

            val actual = AlertUtils.getText(alert, resources, now)
            assertEquals(closerTodayText, actual)
        }

        closedToday("Closed today")
        closedToday("Closed today until tomorrow")
    }

    @Test
    fun closedNotToday() {
        val closedText = "Closed"
        val closedDate = "17 Jan"

        whenever(resources.getString(eq(R.string.alert_closed_not_today), any())).thenAnswer {
            val args = it.arguments
            "$closedText ${args[1]}"
        }

        // 01/16/2018 @ 11:00pm (UTC) in seconds
        val value: Long = 1516143600

        // 01/17/2018 @ 01:00pm (UTC+2)
        val time = Time(value, TIMEZONE_OFFSET, "")
        val alert = Closed("unexpected text", time)

        val actual = AlertUtils.getText(alert, resources, now)
        assertEquals("$closedText $closedDate", actual)
    }

    companion object {
        private val TIMEZONE_OFFSET = TimeUnit.HOURS.toSeconds(2).toInt()
    }
}
