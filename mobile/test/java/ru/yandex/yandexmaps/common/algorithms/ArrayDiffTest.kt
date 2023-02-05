package ru.yandex.yandexmaps.common.algorithms

import org.junit.Assert
import org.junit.Test

class ArrayDiffTest {

    @Test
    fun emptyTest() {
        val testData = "".toList()
        with(ArrayDiff(testData, testData)) {
            Assert.assertTrue(addedElementsPositions.isEmpty())
            Assert.assertTrue(removedElementsPositions.isEmpty())
        }
    }

    @Test
    fun equalTest() {
        val testData = "test string".toList()
        with(ArrayDiff(testData, testData)) {
            Assert.assertTrue(addedElementsPositions.isEmpty())
            Assert.assertTrue(removedElementsPositions.isEmpty())
        }
    }

    @Test
    fun addToEmptyTest() {
        with(ArrayDiff("".toList(), "abc".toList())) {
            Assert.assertEquals(listOf(0, 1, 2), addedElementsPositions)
            Assert.assertTrue(removedElementsPositions.isEmpty())
        }
    }

    @Test
    fun removeAllTest() {
        with(ArrayDiff("zyz".toList(), "".toList())) {
            Assert.assertEquals(listOf(0, 1, 2), removedElementsPositions)
            Assert.assertTrue(addedElementsPositions.isEmpty())
        }
    }

    @Test
    fun addAndRemoveTest() {
        with(ArrayDiff("улца ленено".toList(), "улица ленина".toList())) {
            Assert.assertEquals(listOf(2, 9, 11 /* и, и, а */), addedElementsPositions)
            Assert.assertEquals(listOf(8, 10 /* е, о */), removedElementsPositions)
        }
    }
}
