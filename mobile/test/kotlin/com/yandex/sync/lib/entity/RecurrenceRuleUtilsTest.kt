package com.yandex.sync.lib.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class RecurrenceRuleUtilsTest {

    @Test
    fun `utils check two ways`() {
        assertEquals("FREQ=WEEKLY;INTERVAL=1;BYDAY=TH", recurrenceRule("FREQ=WEEKLY;INTERVAL=1;BYDAY=TH").toICalString())
    }
}