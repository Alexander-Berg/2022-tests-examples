package com.edadeal.android.util

import org.junit.Test
import java.util.LinkedList
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SkippableListTest {

    @Test
    fun testSkippableList() {
        val items = IntArray(11) { it }.asList()
        val orderedSkippablePositions = listOf(0, 1, 3, 4, 8, 9)
        val l = SkippableList(items, LinkedList(orderedSkippablePositions))

        assertEquals(listOf(2, 5, 6, 7, 10), l)
        assertEquals(1, l.unSkip(4))
        assertEquals(5, l.unSkip(9))
        assertEquals(listOf(2, 4, 5, 6, 7, 9, 10), l)
        assertEquals(0, l.unSkip(0))
        assertEquals(listOf(0, 2, 4, 5, 6, 7, 9, 10), l)
        assertNull(l.unSkip(9))
        assertEquals(1, l.unSkip(1))
        assertEquals(listOf(0, 1, 2, 4, 5, 6, 7, 9, 10), l)

        l.skip(4)
        l.skip(3)
        assertEquals(listOf(0, 1, 2, 5, 6, 7, 9, 10), l)
        l.skip(0)
        assertEquals(listOf(1, 2, 5, 6, 7, 9, 10), l)
        assertEquals(2, l.unSkip(4))
        assertEquals(listOf(1, 2, 4, 5, 6, 7, 9, 10), l)
        l.skip(6)
        assertEquals(listOf(1, 2, 4, 5, 7, 9, 10), l)
    }
}
