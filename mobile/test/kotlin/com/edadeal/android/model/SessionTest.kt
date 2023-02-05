package com.edadeal.android.model

import com.edadeal.android.data.Prefs
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import kotlin.test.assertEquals

class SessionTest {

    @Test
    fun `session should do onStart action if inactive time is greater than session timeout`() {
        val time = spy(Time())
        val prefs = mock<Prefs>()
        whenever(prefs.pauseTime).thenReturn(0L, 2000L, 4000L, 6000L)
        whenever(time.nowMillis()).thenReturn(1000L, 3000L, 4500L, 6501L)
        var log = ""
        val session = Session(500L, time, prefs, { log += "!" })

        repeat(4) {
            log += "."
            session.resume()
        }

        assertEquals(".!.!..!", log)
    }
}
