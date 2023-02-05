package ru.yandex.supercheck.core.validators.phone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RussianPhoneNumberValidatorTest {

    private val validator = RussianPhoneNumberValidator()

    @Test
    fun isValid() {
        testOnEmptyInput()
        testOnShortInput()
        testOnSimpleInput()
        testOnFormattedInput()
    }

    private fun testOnEmptyInput() {
        assertFalse(validator.isValid(""))
    }

    private fun testOnShortInput() {
        assertFalse(validator.isValid("+7"))
        assertFalse(validator.isValid("+7 999 999 99"))
    }

    private fun testOnSimpleInput() {
        assertTrue(validator.isValid("1239991199"))
    }

    private fun testOnFormattedInput() {
        assertTrue(validator.isValid("+7 999 999 99 99"))
        assertTrue(validator.isValid("+7    999\n9999999"))
        assertTrue(validator.isValid("+7 (999) 999-99-99"))
        assertTrue(validator.isValid("(999) 999-99-99"))
    }
}