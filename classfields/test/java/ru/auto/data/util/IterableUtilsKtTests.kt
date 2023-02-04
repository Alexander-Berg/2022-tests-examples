package ru.auto.data.util

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AllureRunner::class) class IterableUtilsKtTests {

    @Test
    fun `contains subsequence`() {
        val sequence = listOf(1, 2, 3, 4, 4, 5)
        val subsequence = listOf(4, 5)
        assertTrue(sequence.containsSubsequence(subsequence))
    }

    @Test
    fun `does not contain subsequence`() {
        val sequence = listOf(1, 2, 3, 4, 4, 5)
        val subsequence = listOf(4, 3)
        assertFalse(sequence.containsSubsequence(subsequence))
    }

    @Test
    fun `contains empty subsequence`() {
        val sequence = listOf(1, 2, 3, 4, 4, 5)
        val subsequence = listOf<Int>()
        assertTrue(sequence.containsSubsequence(subsequence))
    }

    @Test
    fun `does not contain larger subsequence`() {
        val sequence = listOf(1, 2)
        val subsequence = listOf(1, 2, 3, 4, 4, 5)
        assertFalse(sequence.containsSubsequence(subsequence))
    }

    @Test
    fun `contain exact sequence`() {
        val sequence = listOf(1, 2, 3)
        val subsequence = listOf(1, 2, 3)
        assertTrue(sequence.containsSubsequence(subsequence))
    }
}
