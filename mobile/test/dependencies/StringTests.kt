package com.yandex.xplat.xmail

import com.yandex.xplat.common.*
import org.junit.Assert.*
import org.junit.Test

class StringTests {
    @Test
    fun shouldCorrectlySliceStrings() {
        val str = "The quick brown fox jumps over the lazy dog."

        assertEquals(str.slice(31), "the lazy dog.")
        assertEquals(str.slice(4, 19), "quick brown fox")
        assertEquals(str.slice(-4), "dog.")
        assertEquals(str.slice(-9, -5), "lazy")
        assertEquals(str.slice(31, -5), "the lazy")
        assertEquals(str.slice(300), "")
        assertEquals(str.slice(31, 26), "")
        assertEquals(str.slice(), "The quick brown fox jumps over the lazy dog.")
        assertEquals("".slice(), "")
        assertEquals("".slice(5, 10), "")
    }

    @Test
    fun shouldCorrectlySubstringStrings() {
        val str = "Mozilla"

        assertEquals(str.substring(0, 1), "M")
        assertEquals(str.substring(1, 0), "M")
        assertEquals(str.substring(0, 6), "Mozill")
        assertEquals(str.substring(-1, 6), "Mozill")
        assertEquals(str.substring(4), "lla")
        assertEquals(str.substring(4, 7), "lla")
        assertEquals(str.substring(7, 4), "lla")
        assertEquals(str.substring(-5, 2), "Mo")
        assertEquals(str.substring(-5, -2), "")
        assertEquals(str.substring(10), "")
        assertEquals(str.substring(10, 12), "")
        assertEquals(str.substring(10, -12), "Mozilla")
        assertEquals("".substring(0), "")
        assertEquals("".substring(5, 10), "")
    }

    @Test
    fun shouldCorrectlySubstrStrings() {
        val str = "Mozilla"

        assertEquals(str.substr(0, 1), "M")
        assertEquals(str.substr(1, 0), "")
        assertEquals(str.substr(-1, 1), "a")
        assertEquals(str.substr(1, -1), "")
        assertEquals(str.substr(-3), "lla")
        assertEquals(str.substr(1), "ozilla")
        assertEquals(str.substr(-20, 2), "Mo")
        assertEquals(str.substr(20, 2), "")
        assertEquals("".substr(0), "")
        assertEquals("".substr(5, 10), "")
    }

    @Test
    fun testShouldFindSubstringByRegex() {
        val str = "Quick brown fox jumps over the lazy dog"
        val regex = "^([^@]+)@(ya(?:ndex\\-team|money)\\.(?:ru|com(\\.(tr|ua))?))$"

        assertEquals(str.search("o"), 8)
        assertEquals(str.search("abc"), -1)
        assertNotEquals("comfly@yandex-team.ru".search(regex), -1)
    }

    @Test
    fun testShouldFindIndexOfSubstring() {
        val str = "Quick brown fox jumps over the lazy dog"
        assertEquals(str.indexOf("brown"), 6)
        assertEquals(str.indexOf("brown", 6), 6)
        assertEquals(str.indexOf("brown", 7), -1)

        assertEquals(str.indexOf("abc"), -1)
        assertEquals(str.indexOf("abc", str.length - 2), -1)
        assertEquals(str.indexOf("abc", str.length + 2), -1)

        assertEquals(str.indexOf(""), 0)
        assertEquals(str.indexOf("", 2), 2)
        assertEquals(str.indexOf("", str.length + 1), str.length)

        assertEquals("".indexOf(""), 0)
        assertEquals("".indexOf("", 2), 0)
        assertEquals("".indexOf("a"), -1)
        assertEquals("".indexOf("a", 8), -1)
    }
}
