package ru.yandex.yandexbus.inhouse.utils.datetime

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

class DateTimeTest {

    private lateinit var parisTimeZone: TimeZone
    private lateinit var moscowTimeZone: TimeZone

    @Before
    fun setUp() {
        parisTimeZone = TimeZone.getTimeZone("Europe/Paris")
        moscowTimeZone = TimeZone.getTimeZone("Europe/Moscow")
    }

    @Test
    fun `dates ordered chronologically`() {
        val early = DateTime.now()
        val later = early.plusSeconds(2)
        val latest = early.plusSeconds(3)

        assertEquals(
            listOf(early, later, latest),
            listOf(later, early, latest).sorted()
        )
    }

    @Test
    fun `local dates ordered by utc`() {
        val format = "HH:mm"
        val localTime = "01:00"

        val paris = DateFormat(format, parisTimeZone).parse(localTime)
        val moscow = DateFormat(format, moscowTimeZone).parse(localTime)

        assertEquals(
            listOf(moscow, paris),
            listOf(paris, moscow).sorted()
        )
    }
}
