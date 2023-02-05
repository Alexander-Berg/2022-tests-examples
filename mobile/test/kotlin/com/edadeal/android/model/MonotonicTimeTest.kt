package com.edadeal.android.model

import com.edadeal.android.metrics.MonotonicTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonotonicTimeTest {

    @Test
    fun `should return correct time`() {
        var uptime = 1L
        var epochTime = 1000L
        val monotonicTime = MonotonicTime.create({ epochTime }, { uptime })

        assertEquals(1000L, monotonicTime.nowMillis())
        uptime++
        epochTime--
        assertEquals(1001L, monotonicTime.nowMillis())
    }

    @Test
    fun `should return true if interval time passed`() {
        var uptime = 1L
        val monotonicTime = MonotonicTime.create({ 1000L }, { uptime })
        val startTime = monotonicTime.nowMillis()
        val intervalMillis = 100L

        uptime += intervalMillis + 1
        assertTrue(monotonicTime.isTimePassed(startTime, intervalMillis))
    }
}
