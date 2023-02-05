package com.edadeal.android.model

import org.junit.Test
import kotlin.test.assertEquals

class LRUMapTest {

    @Test
    fun `lru map removes oldest entries`() {
        val map = LRUMap<Int, Unit>(2)
        map[0] = Unit
        map[1] = Unit
        map[2] = Unit
        map[3] = Unit
        assertEquals(map.keys, setOf(2, 3))
    }

    @Test
    fun `lru map don't remove elements before max entries reached`() {
        val map = LRUMap<Int, Unit>(10)
        map[0] = Unit
        map[1] = Unit
        map[2] = Unit
        map[3] = Unit
        assertEquals(map.keys, setOf(0, 1, 2, 3))
    }

    @Test
    fun `lru map removes first entries when putAll uses`() {
        val map = LRUMap<Int, Unit>(2)
        map.putAll(setOf(0, 1, 2, 3, 4, 5).map { Pair(it, Unit) })
        map[99] = Unit
        assertEquals(map.keys, setOf(5, 99))
    }

    @Test
    fun `lru is always empty when has maxEntries equals zero`() {
        val map = LRUMap<Int, Unit>(0)
        map.putAll(setOf(0, 1, 2, 3, 4, 5).map { Pair(it, Unit) })
        map[99] = Unit
        assertEquals(map.keys, emptySet<Int>())
    }

    @Test
    fun `lru updates eldest entry on read value`() {
        val map = LRUMap<Int, Unit>(3)
        map.putAll(setOf(0, 1, 2, 3, 4, 5).map { Pair(it, Unit) })
        map[3]
        map[6] = Unit
        assertEquals(map.keys, setOf(3, 5, 6))
    }
}
