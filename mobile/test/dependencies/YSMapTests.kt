package com.yandex.xplat.xmail

import com.yandex.xplat.common.*
import org.junit.Assert.*
import org.junit.Test

class YSMapTests {
    private val dictionary: YSMap<Int, String> = mutableMapOf(1 to "1", 2 to "2", 3 to "3")

    @Test
    fun shouldCorrectlyIterateMap() {
        var count = 0
        dictionary.__forEach { value, key ->
            assertEquals(value, dictionary.get(key))
            count++
        }
        assertEquals(dictionary.size, count)
    }
}
