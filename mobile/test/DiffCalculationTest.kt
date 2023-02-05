package ru.yandex.yandexmaps.mapobjectsrenderer.test

import ru.yandex.yandexmaps.mapobjectsrenderer.internal.calculateDiff
import kotlin.test.Test
import kotlin.test.assertEquals

typealias Item = Pair<Int, String>

class DiffCalculationTest {

    @Test
    fun firstAddingTest() {
        val oldList = emptyList<Item>()
        val newList = listOf(
            1 to "A",
            2 to "B",
            3 to "C"
        )

        val diff = calculateDiff(oldList, newList) { first }
        assertEquals(diff.insertedItems.toList(), newList)
        assertEquals(diff.removedItems.toList(), emptyList())
        assertEquals(diff.changedItems.toList(), emptyList())
    }

    @Test
    fun removingAllTest() {
        val oldList = listOf(
            1 to "A",
            2 to "B",
            3 to "C"
        )
        val newList = emptyList<Item>()

        val diff = calculateDiff(oldList, newList) { first }
        assertEquals(diff.insertedItems.toList(), emptyList())
        assertEquals(diff.removedItems.toList(), oldList)
        assertEquals(diff.changedItems.toList(), emptyList())
    }

    @Test
    fun changeItemsTest() {
        val oldList = listOf(
            1 to "A",
            2 to "B",
            3 to "C"
        )
        val newList = listOf(
            1 to "A",
            2 to "B+",
            3 to "C+"
        )

        val diff = calculateDiff(oldList, newList) { first }
        assertEquals(diff.insertedItems.toList(), emptyList())
        assertEquals(diff.removedItems.toList(), emptyList())
        assertEquals(
            diff.changedItems.toList(),
            listOf(
                2 to "B+",
                3 to "C+"
            )
        )
    }

    @Test
    fun combinedTest() {
        val oldList = listOf(
            1 to "A",
            2 to "B",
            3 to "C"
        )
        val newList = listOf(
            0 to "A",
            2 to "B+",
            3 to "C+",
            4 to "D"
        )

        val diff = calculateDiff(oldList, newList) { first }
        assertEquals(diff.insertedItems.toList(), listOf(0 to "A", 4 to "D"))
        assertEquals(diff.removedItems.toList(), listOf(1 to "A"))
        assertEquals(
            diff.changedItems.toList(),
            listOf(
                2 to "B+",
                3 to "C+"
            )
        )
    }
}
