package com.edadeal.android.ui.div

import com.edadeal.android.model.Time
import com.edadeal.android.ui.div.custom.DivCountdownTimer.TimeFormat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class DivCountdownTimeFormatTest(
    private val date: String,
    private val dateEnd: String,
    private val timeFormat: TimeFormat,
    private val expectedFormat: String,
    private val expectedRemaining: Boolean
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "2022-01-07T20:59:59+03:00",
                "2022-01-08T00:00:00+03:00",
                TimeFormat.HoursMinutesSeconds,
                "03:00:01",
                true
            ),
            arrayOf(
                "2022-01-07T18:59:59+03:00",
                "2022-01-08T00:00:00+03:00",
                TimeFormat.MinutesSeconds,
                "300:01",
                true
            ),
            arrayOf(
                "2022-01-08T00:59:59+07:00",
                "2022-01-08T00:00:00+03:00",
                TimeFormat.HoursMinutes,
                "03:00",
                true
            ),
            arrayOf(
                "2022-01-05T23:59:59+03:00",
                "2022-01-08T00:00:00+03:00",
                TimeFormat.HoursMinutesSeconds,
                "48:00:01",
                true
            ),
            arrayOf(
                "2022-01-08T04:59:59+07:00",
                "2022-01-08T00:00:00+03:00",
                TimeFormat.HoursMinutesSeconds,
                "00:00:00",
                false
            ),
            arrayOf(
                "2022-01-08T04:59:59+07:00",
                "2022-01-08T00:00:00+03:00",
                TimeFormat.MinutesSeconds,
                "00:00",
                false
            ),
            arrayOf(
                "2022-01-08T04:59:59+07:00",
                "2022-01-08T00:00:00+03:00",
                TimeFormat.HoursMinutes,
                "00:00",
                false
            )
        )
    }

    private val time = Time()

    @Test
    fun `should return the remaining time in the expected format`() {
        val timestamp = time.getTimestampFromRfc3339(date)
        val endTimestamp = time.getTimestampFromRfc3339(dateEnd)

        assertEquals(expectedFormat, timeFormat.formatRemainingTime(timestamp, endTimestamp))
        assertEquals(expectedRemaining, timeFormat.hasRemainingTime(timestamp, endTimestamp))
    }
}
