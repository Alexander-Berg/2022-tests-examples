package com.yandex.xplat.xmail

import com.yandex.xplat.common.*
import org.junit.Assert.*
import org.junit.Test

class YSArrayTests {
    private val sampleData: YSArray<Int> = mutableListOf(1, 2, 3, 4)

    @Test
    fun shouldCorrectlySliceArrays() {
        assertEquals(sampleData.slice(), mutableListOf(1, 2, 3, 4))
        assertEquals(sampleData.slice(1), mutableListOf(2, 3, 4))
        assertEquals(sampleData.slice(5), mutableListOf<Int>())
        assertEquals(sampleData.slice(1, 3), mutableListOf(2, 3))
        assertEquals(sampleData.slice(3, 2), mutableListOf<Int>())
        assertEquals(sampleData.slice(-2), mutableListOf(3, 4))
        assertEquals(sampleData.slice(-5), mutableListOf(1, 2, 3, 4))
        assertEquals(sampleData.slice(0, -2), mutableListOf(1, 2))
        assertEquals(sampleData.slice(0, -5), mutableListOf<Int>())
    }

    @Test
    fun shouldCorrectlySortArrays() {
        assertEquals(mutableListOf(1, 2, 3, 4, 5).sort { a, b -> a - b }, mutableListOf(1, 2, 3, 4, 5))
        assertEquals(mutableListOf(4, 2, 5, 1, 3).sort { a, b -> a - b }, mutableListOf(1, 2, 3, 4, 5))
        assertEquals(mutableListOf(2, 2, 1, 1, 3).sort { a, b -> a - b }, mutableListOf(1, 1, 2, 2, 3))
        assertEquals(mutableListOf<Int>().sort { a, b -> a - b }, mutableListOf<Int>())
    }

    @Test
    fun shouldCorrectlySpliceArrays() {
        var a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(0), mutableListOf(1, 2, 3, 4, 5))
        assertEquals(a, mutableListOf<Int>())

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(1), mutableListOf(2, 3, 4, 5))
        assertEquals(a, mutableListOf(1))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(-2), mutableListOf(4, 5))
        assertEquals(a, mutableListOf(1, 2, 3))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(-5), mutableListOf(1, 2, 3, 4, 5))
        assertEquals(a, mutableListOf<Int>())

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(-6), mutableListOf(1, 2, 3, 4, 5))
        assertEquals(a, mutableListOf<Int>())

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(0, 0), mutableListOf<Int>())
        assertEquals(a, mutableListOf(1, 2, 3, 4, 5))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(1, -1), mutableListOf<Int>())
        assertEquals(a, mutableListOf(1, 2, 3, 4, 5))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(1, 1), mutableListOf(2))
        assertEquals(a, mutableListOf(1, 3, 4, 5))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(-2, 1), mutableListOf(4))
        assertEquals(a, mutableListOf(1, 2, 3, 5))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(-4, 0), mutableListOf<Int>())
        assertEquals(a, mutableListOf(1, 2, 3, 4, 5))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(-4, 2), mutableListOf(2, 3))
        assertEquals(a, mutableListOf(1, 4, 5))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(-5, 5), mutableListOf(1, 2, 3, 4, 5))
        assertEquals(a, mutableListOf<Int>())

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(-6, 4), mutableListOf(1, 2, 3, 4))
        assertEquals(a, mutableListOf(5))

        a = mutableListOf(1, 2, 3, 4, 5)
        assertEquals(a.splice(2, 3), mutableListOf(3, 4, 5))
        assertEquals(a, mutableListOf(1, 2))

        a = mutableListOf()
        assertEquals(a.splice(0), mutableListOf<Int>())
        assertEquals(a, mutableListOf<Int>())
    }
}
