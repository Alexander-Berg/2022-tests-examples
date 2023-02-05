package com.edadeal.android.util

import com.edadeal.android.metrics.LogKitTime
import com.edadeal.android.model.Time
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class LogKitTimeTest(
    private val expectedDate: String,
    private val timeZoneId: String
) {

    companion object {
        private const val INSTANT = 62467200000L

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Any> = listOf(
            arrayOf("1971-12-25T01:00:00+01:00", "GMT+1:00"),
            arrayOf("1971-12-25T00:00:00+00:00", "UTC"),
            arrayOf("1971-12-24T16:00:00-08:00", "GMT-8:00"),
            arrayOf("1971-12-25T00:00:00+00:00", "Europe/London"),
            arrayOf("1971-12-25T03:00:00+03:00", "Europe/Moscow")
        )
    }

    @Test
    fun `should return date with correct time zone offset`() {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId))
        val time = Time { INSTANT }
        assertEquals(expectedDate, LogKitTime.create(time).getIso8601Date())
    }
}
