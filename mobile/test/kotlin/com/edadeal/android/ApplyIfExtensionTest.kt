package com.edadeal.android

import org.junit.Test
import kotlin.test.assertEquals

class ApplyIfExtensionTest {

    private val string = "some_text"
    private val stringUpperCase = string.toUpperCase()

    @Test
    fun `should apply block if condition equals true`() {
        val result = string.applyIf(true) { toUpperCase() }
        assertEquals(stringUpperCase, result)
    }

    @Test
    fun `should not apply block if condition equals false`() {
        val result = string.applyIf(false) { toUpperCase() }
        assertEquals(string, result)
    }
}
