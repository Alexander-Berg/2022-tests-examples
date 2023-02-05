package com.edadeal.android.model.ads

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IncludeExcludeTest {

    @Test
    fun `isOk should always return true if include and exclude lists are empty`() {
        with(AdConditions.IncludeExclude<Int>(include = emptySet(), exclude = emptySet())) {
            assertTrue(isOk(1))
            assertTrue(isOk(listOf()))
            assertTrue(isOk(listOf(1)))
        }
    }

    @Test
    fun `isOk should return true for ids, provided in include list, and false, for ids that not in include list`() {
        with(AdConditions.IncludeExclude(include = mutableSetOf(1, 2), exclude = emptySet())) {
            assertTrue(isOk(1))
            assertTrue(isOk(2))
            assertFalse(isOk(3))
            assertFalse(isOk(listOf()))
            assertTrue(isOk(listOf(1)))
            assertTrue(isOk(listOf(1, 2)))
            assertTrue(isOk(listOf(1, 2, 3)))
            assertFalse(isOk(listOf(3)))
        }
    }

    @Test
    fun `isOk should return false for ids, provided in exclude list, and true for other ids, if include list is empty`() {
        with(AdConditions.IncludeExclude(include = emptySet(), exclude = mutableSetOf(1, 2))) {
            assertFalse(isOk(1))
            assertFalse(isOk(2))
            assertTrue(isOk(3))
            assertTrue(isOk(listOf()))
            assertFalse(isOk(listOf(1)))
            assertFalse(isOk(listOf(1, 2)))
            assertFalse(isOk(listOf(1, 2, 3)))
            assertTrue(isOk(listOf(3)))
        }
    }
}
