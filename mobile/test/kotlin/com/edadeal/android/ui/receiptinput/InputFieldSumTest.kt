package com.edadeal.android.ui.receiptinput

import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InputFieldSumTest {

    @Test
    fun `input with float part 2 chars length should not return error`() {
        assertNoError("1.23", InputField.Sum::getErrorStringId)
    }

    @Test
    fun `should accept comma as separator`() {
        assertNoError("1,23", InputField.Sum::getErrorStringId)
    }

    @Test
    fun `sum with only zero digits should return error`() {
        assertHasError("000", InputField.Sum::getErrorStringId)
    }

    @Test
    fun `empty sum should return error`() {
        assertHasError("", InputField.Sum::getErrorStringId)
    }

    @Test
    fun `sum with few separators should return error`() {
        assertHasError("1.2.", InputField.Sum::getErrorStringId)
    }

    @Test
    fun `sum with only float part should return error`() {
        assertHasError(".33", InputField.Sum::getErrorStringId)
    }

    @Test
    fun `if sum float part length not equals 2, should return error`() {
        assertHasError("1.1", InputField.Sum::getErrorStringId)
        assertHasError("1.123", InputField.Sum::getErrorStringId)
        assertHasError("1.12345", InputField.Sum::getErrorStringId)
    }

    private fun assertHasError(input: String, onGetError: (String) -> Int?) {
        assertNotNull(onGetError(input))
    }

    private fun assertNoError(input: String, onGetError: (String) -> Int?) {
        assertNull(onGetError(input))
    }
}
