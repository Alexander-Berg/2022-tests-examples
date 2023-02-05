package ru.yandex.supercheck.core.validators.email

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleEmailValidatorTest {

    private val validator = SimpleEmailValidator()

    @Test
    fun isValid() {
        testOnCorrectEmails()
        testOnIncorrectEmails()
    }

    private fun testOnCorrectEmails() {
        assertTrue(validator.isValid("aaa@bbb.ccc"))
        assertTrue(validator.isValid("foo.bar@aaa.bbb.ccc"))
        assertTrue(validator.isValid("my.email123@почта.рф"))
    }

    private fun testOnIncorrectEmails() {
        assertFalse(validator.isValid("aaa"))
        assertFalse(validator.isValid("@"))
        assertFalse(validator.isValid("@."))
        assertFalse(validator.isValid("aaa@."))
        assertFalse(validator.isValid("@aa.com"))
        assertFalse(validator.isValid("a@a."))
    }
}