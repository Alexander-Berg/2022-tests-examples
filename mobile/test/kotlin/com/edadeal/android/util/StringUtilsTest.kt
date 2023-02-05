package com.edadeal.android.util

import com.edadeal.android.util.StringUtils.getTld
import org.junit.Test
import kotlin.test.assertEquals

class StringUtilsTest {
    @Test
    fun `getTld is correct with simple url`() {
        val input = "https://yandex.ru"
        val result = getTld(input)
        assertEquals(result, "ru")
    }
}
