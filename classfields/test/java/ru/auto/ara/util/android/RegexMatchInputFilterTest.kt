package ru.auto.ara.util.android

import android.text.SpannedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.RobolectricTest
import ru.auto.test.runner.AllureRobolectricRunner

/**
 * @author themishkun on 09/11/2017.
 */
@RunWith(AllureRobolectricRunner::class) class RegexMatchInputFilterTest : RobolectricTest() {

    val regexMatchInputFilter = RegexMatchInputFilter("[a-zA-Z]+".toRegex())

    @Test fun `regexMatchInputFilter should pass these strings (return null by InputField contract)`() {
        assertNull(regexMatchInputFilter.filter("a", 0, 1, SpannedString("abcd"), 0, 1))
        assertNull(regexMatchInputFilter.filter("F", 0, 1, SpannedString("abcd"), 0, 1))
    }

    @Test fun `regexMatchInputFilter should disallow these strings (return "" by contract)`() {
        assertDisallowed(regexMatchInputFilter.filter("7", 0, 1, SpannedString("abcd"), 0, 1))
        assertDisallowed(regexMatchInputFilter.filter(" ", 0, 1, SpannedString("abcd"), 0, 1))
        assertDisallowed(regexMatchInputFilter.filter("\n", 0, 1, SpannedString("abcd"), 0, 1))
    }

    private fun assertDisallowed(value: CharSequence?) = assertEquals("", value?.toString())

}
