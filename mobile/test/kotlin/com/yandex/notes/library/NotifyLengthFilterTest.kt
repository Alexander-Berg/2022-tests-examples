package com.yandex.notes.library

import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.SpannedString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.yandex.notes.library.editor.NotifyLengthFilter
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val MAX_LENGTH = 5

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NotifyLengthFilterTest {

    private val onFiltered = mock<NotifyLengthFilter.OnTextFilteredCallback>()
    private val filter = NotifyLengthFilter(MAX_LENGTH, onFiltered)

    @Test
    fun `should not notify if text was not filtered`() {
        filter.filter("12345", 0, 4, SpannedString(""), 0, 4)
        verifyNoMoreInteractions(onFiltered)
    }

    @Test
    fun `should notify on text filtered if input text is to long`() {
        filter.filter("123456", 0, 6, SpannedString(""), 0, 0)
        verify(onFiltered).onFiltered()
    }

    @Test
    fun `should notify on text filtered adding text in dest end if dest filled`() {
        filter.filter("6", 0, 1, SpannedString("12345"), 5, 5)
        verify(onFiltered).onFiltered()
    }

    @Test
    fun `should notify on text filtered adding text in dest center if dest filled`() {
        filter.filter("6", 0, 1, SpannedString("12345"), 3, 3)
        verify(onFiltered).onFiltered()
    }

    // copy of http://androidxref.com/9.0.0_r3/xref/cts/tests/tests/text/src/android/text/cts/InputFilter_LengthFilterTest.java
    // added to prove length filter base functionality
    @Test
    fun `should have base length filter functionality`() {
        // Define the variables
        val source: CharSequence
        var dest: SpannableStringBuilder
        // Constructor to create a LengthFilter
        val lengthFilter = NotifyLengthFilter(10, onFiltered)
        val filters = arrayOf<InputFilter>(lengthFilter)

        // filter() implicitly invoked. If the total length > filter length, the filter will
        // cut off the source CharSequence from beginning to fit the filter length.
        source = "abc"
        dest = SpannableStringBuilder("abcdefgh")
        dest.filters = filters

        dest.insert(1, source)
        val expectedString1 = "aabbcdefgh"
        assertThat(expectedString1, equalTo(dest.toString()))

        dest.replace(5, 8, source)
        val expectedString2 = "aabbcabcgh"
        assertThat(expectedString2, equalTo(dest.toString()))

        dest.delete(1, 3)
        val expectedString3 = "abcabcgh"
        assertThat(expectedString3, equalTo(dest.toString()))

        // filter() explicitly invoked
        dest = SpannableStringBuilder("abcdefgh")
        val beforeFilterSource = "TestLengthFilter"
        val expectedAfterFilter = "TestLength"
        val actualAfterFilter = lengthFilter.filter(beforeFilterSource, 0,
            beforeFilterSource.length, dest, 0, dest.length)
        assertThat(expectedAfterFilter, equalTo(actualAfterFilter))
    }
}
