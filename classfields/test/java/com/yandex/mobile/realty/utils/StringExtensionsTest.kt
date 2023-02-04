package com.yandex.mobile.realty.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author andrey-bgm on 18/07/2019.
 */
class StringExtensionsTest {

    @Test
    fun capitalizeSiteName() {
        assertEquals("ЖК Морская звезда", "жк Морская звезда".capitalizeSiteName())
        assertEquals("ЖК Морская звезда", "Жк Морская звезда".capitalizeSiteName())
        assertEquals("ЖК Морская звезда", "ЖК Морская звезда".capitalizeSiteName())

        assertEquals("МФК Морская звезда", "мфк Морская звезда".capitalizeSiteName())

        assertEquals("Морская звезда", "морская звезда".capitalizeSiteName())

        assertEquals("", "".capitalizeSiteName())
        assertEquals("Мз", "мз".capitalizeSiteName())
        assertEquals("МФК ", "мфк ".capitalizeSiteName())
    }
}
