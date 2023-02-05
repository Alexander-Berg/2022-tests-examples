package ru.yandex.yandexbus.inhouse.utils.datetime

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.TimeZone

@RunWith(Parameterized::class)
class DateFormatTest(private val timeZone: TimeZone) {

    @Test
    fun `parsing does not change DateTime`() {
        val dateFormat = DateFormat("dd.MM.yyyy HH:mm", timeZone)
        val dateTime = DateTime(0, timeZone)

        val formatted = dateFormat.format(dateTime)
        val restored = dateFormat.parse(formatted)

        assertEquals(dateTime, restored)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun testData() = listOf(
            TimeZone.getTimeZone("UTC"),
            TimeZone.getTimeZone("Europe/Moscow"),
            TimeZone.getTimeZone("America/Chicago")
        )
    }
}
