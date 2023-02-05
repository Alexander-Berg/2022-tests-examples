package com.edadeal.android.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtilsTest {

    @Test
    fun `isEquals should correctly round results`() {
        assertTrue(Utils.isEquals(10 / 3f, 10 / 3f))
        assertTrue(Utils.isEquals(10 / 3.0, 10 / 3.0))
        assertFalse(Utils.isEquals(10 / 3.0, 10 / 3.1))
        assertFalse(Utils.isEquals(10 / 3f, 10 / 3.1f))
        assertEquals("14.9", 14.9f.toString().toDouble().toString())
    }
}
