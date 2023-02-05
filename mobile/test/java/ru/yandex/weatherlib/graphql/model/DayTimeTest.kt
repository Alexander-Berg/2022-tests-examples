package ru.yandex.weatherlib.graphql.model

import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import ru.yandex.weatherlib.graphql.api.model.type.Daytime
import ru.yandex.weatherlib.graphql.model.enums.DayTime

/**
 * Unit tests for [ru.yandex.weatherlib.graphql.model.DayTime].
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [DayTimeTest.ZonedDateTimeShadow::class])
class DayTimeTest {

    @Test
    fun dayValueSunsetSunriseTest() {
        Assert.assertEquals(DayTime.DAY, test())
    }

    @Test
    fun morningValueSunsetSunriseTest() {
        Assert.assertEquals(DayTime.MORNING, test("05:05"))
    }

    @Test
    fun eveningValueSunsetSunriseTest() {
        Assert.assertEquals(DayTime.EVENING, test("22:05"))
    }

    @Test
    fun nightValueFromSunsetSunriseTest() {
        Assert.assertEquals(DayTime.NIGHT, test("02:00"))
    }

    @Test
    fun polarDayValueFromSunsetSunriseTest() {
        Assert.assertEquals(DayTime.DAY, test(polar = Daytime.DAY))
    }

    @Test
    fun polarNightValueFromSunsetSunriseTest() {
        Assert.assertEquals(DayTime.NIGHT, test(polar = Daytime.NIGHT))
    }

    @Test
    fun polarUnknownValueFromSunsetSunriseTest() {
        Assert.assertEquals(DayTime.DAY, test(polar = Daytime.UNKNOWN__))
    }

    private fun test(currentTime: String = "13:00", polar: Daytime? = null): DayTime {
        timeUnderTest = currentTime
        val timeZoneTime = "2021-08-11T00:00:00+03:00"
        return DayTime.valueFromSunsetSunrise(timeZoneTime, sunriseBegin, sunriseEnd, sunsetStart, sunsetEnd, polar)
    }

    companion object {
        var timeUnderTest = "13:00"
        val sunriseBegin = "05:00"
        val sunriseEnd = "05:10"
        val sunsetStart = "22:00"
        val sunsetEnd = "22:10"
    }

    @Implements(ZonedDateTime::class)
    class ZonedDateTimeShadow {
        companion object {
            private val zonedDateTimeMock: ZonedDateTime = mock {
                on { toLocalTime() } doAnswer { createLocalTime() }
            }

            @Implementation
            @JvmStatic
            fun now(zone: ZoneId): ZonedDateTime {
                return zonedDateTimeMock
            }

            private fun createLocalTime() = LocalTime.parse(timeUnderTest)
        }
    }
}
