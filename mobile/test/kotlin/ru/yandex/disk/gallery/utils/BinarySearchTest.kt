package ru.yandex.disk.gallery.utils

import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.disk.gallery.utils.BinarySearch.countInRange
import ru.yandex.disk.gallery.utils.BinarySearch.lowerBound
import ru.yandex.disk.test.Assert2.assertThat

class BinarySearchTest {

    @Test
    fun `should search lower bound`() {
        assertThat(lowerBound(listOf(), 1), equalTo(0))
        assertThat(lowerBound(listOf(0), -1), equalTo(0))
        assertThat(lowerBound(listOf(0), 1), equalTo(1))

        assertThat(lowerBound(listOf(1, 2), 0), equalTo(0))
        assertThat(lowerBound(listOf(1, 2), 1), equalTo(0))
        assertThat(lowerBound(listOf(1, 2), 2), equalTo(1))
        assertThat(lowerBound(listOf(1, 2), 3), equalTo(2))

        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), -1), equalTo(0))
        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), 0), equalTo(0))
        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), 5), equalTo(1))
        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), 10), equalTo(1))
        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), 15), equalTo(2))
        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), 20), equalTo(2))
        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), 25), equalTo(4))
        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), 30), equalTo(4))
        assertThat(lowerBound(listOf(0, 10, 20, 20, 30, 30, 30), 35), equalTo(7))
    }

    @Test
    fun `should search lower bound by comparator`() {
        val cmp = compareBy<Int> { -it }

        assertThat(lowerBound(listOf(), 1, cmp), equalTo(0))
        assertThat(lowerBound(listOf(0), 1, cmp), equalTo(0))
        assertThat(lowerBound(listOf(0), -1, cmp), equalTo(1))

        assertThat(lowerBound(listOf(2, 1), 3, cmp), equalTo(0))
        assertThat(lowerBound(listOf(2, 1), 2, cmp), equalTo(0))
        assertThat(lowerBound(listOf(2, 1), 1, cmp), equalTo(1))
        assertThat(lowerBound(listOf(2, 1), 0, cmp), equalTo(2))

        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), 35, cmp), equalTo(0))
        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), 30, cmp), equalTo(0))
        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), 25, cmp), equalTo(3))
        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), 20, cmp), equalTo(3))
        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), 15, cmp), equalTo(5))
        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), 10, cmp), equalTo(5))
        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), 5, cmp), equalTo(6))
        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), 0, cmp), equalTo(6))
        assertThat(lowerBound(listOf(30, 30, 30, 20, 20, 10, 0), -1, cmp), equalTo(7))
    }

    @Test
    fun `should count in range`() {

        assertThat(countInRange(listOf(), 1..1), equalTo(0))
        assertThat(countInRange(listOf(0), 1..1), equalTo(0))
        assertThat(countInRange(listOf(1), 1..1), equalTo(1))

        assertThat(countInRange(listOf(1, 2), 0..0), equalTo(0))
        assertThat(countInRange(listOf(1, 2), 0..1), equalTo(1))
        assertThat(countInRange(listOf(1, 2), 0..2), equalTo(2))
        assertThat(countInRange(listOf(1, 2), 0..3), equalTo(2))

        assertThat(countInRange(listOf(1, 2), 1..1), equalTo(1))
        assertThat(countInRange(listOf(1, 2), 1..2), equalTo(2))
        assertThat(countInRange(listOf(1, 2), 1..3), equalTo(2))

        assertThat(countInRange(listOf(1, 2), 2..2), equalTo(1))
        assertThat(countInRange(listOf(1, 2), 2..3), equalTo(1))

        assertThat(countInRange(listOf(0, 10, 20, 20, 30, 30, 30), 0..30), equalTo(7))
        assertThat(countInRange(listOf(0, 10, 20, 20, 30, 30, 30), 5..25), equalTo(3))
        assertThat(countInRange(listOf(0, 10, 20, 20, 30, 30, 30), 15..25), equalTo(2))
        assertThat(countInRange(listOf(0, 10, 20, 20, 30, 30, 30), 15..30), equalTo(5))
        assertThat(countInRange(listOf(0, 10, 20, 20, 30, 30, 30), 25..30), equalTo(3))
        assertThat(countInRange(listOf(0, 10, 20, 20, 30, 30, 30), 25..35), equalTo(3))
        assertThat(countInRange(listOf(0, 10, 20, 20, 30, 30, 30), 30..30), equalTo(3))
        assertThat(countInRange(listOf(0, 10, 20, 20, 30, 30, 30), -1..35), equalTo(7))
    }
}
