package ru.yandex.yandexbus.inhouse.utils.util

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.yandexbus.inhouse.utils.datetime.DateFormat
import ru.yandex.yandexbus.inhouse.utils.datetime.floorTo5Min

@RunWith(Parameterized::class)
class DateTimeUtilsFloorTo5MinTest(private val originalDate: String, private val expectedDate: String) {

    @Test
    fun florTo5Min() {
        val result = testDateFormat.parse(originalDate).floorTo5Min()
        Assert.assertEquals(expectedDate, testDateFormat.format(result))
    }

    companion object {
        private val testDateFormat = DateFormat("dd.MM.yyyy HH:mm")

        @JvmStatic
        @Parameterized.Parameters
        fun testData(): Collection<Array<String>> = listOf(
            arrayOf("23.07.1997 15:20", "23.07.1997 15:20"),
            arrayOf("23.07.1997 15:25", "23.07.1997 15:25"),
            arrayOf("23.07.1997 18:49", "23.07.1997 18:45"),
            arrayOf("23.07.1997 18:42", "23.07.1997 18:40"),
            arrayOf("23.07.1997 00:00", "23.07.1997 00:00"),
            arrayOf("23.07.1997 00:01", "23.07.1997 00:00"),
            arrayOf("01.01.1997 00:05", "01.01.1997 00:05"),
            arrayOf("01.01.1997 00:06", "01.01.1997 00:05")
        )
    }
}