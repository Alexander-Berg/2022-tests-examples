package ru.yandex.yandexmaps.tools.bumpmapkit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TicketTest {

    @Test
    fun `ensures MapKit release name extraction correctness`() {
        val tests = arrayOf(
            "Maps mobile release 2021110317 Ноябрь 2" to "Ноябрь 2",
        )
        for (test in tests) {
            val summary = test.first
            val expected = test.second
            val result = Ticket.getMapkitReleaseName(summary)
            assertEquals(expected, result)
        }
    }

    @Test
    fun `invalid MapKit release tickets`() {
        val tests = arrayOf(
            "Maps mobile release 2021110317 Ноябрь",
            "Maps mobile release 2021110317 Ноябрь-2",
            "Maps mobile release 2021110317 November 2",
            "",
            null,
        )

        for (summary in tests) {
            assertFailsWith<IllegalStateException> {
                Ticket.getMapkitReleaseName(summary)
            }
        }
    }
}
