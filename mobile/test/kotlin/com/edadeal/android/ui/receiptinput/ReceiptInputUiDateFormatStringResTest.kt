package com.edadeal.android.ui.receiptinput

import com.edadeal.android.R
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test

class ReceiptInputUiDateFormatStringResTest : ReceiptInputUiDateFormatTest {

    companion object {
        private const val TODAY_STRING_ID = R.string.receiptInputDateToday
        private const val YESTERDAY_STRING_ID = R.string.receiptInputDateYesterday
    }

    override val getStringMock: (Int) -> String = mock()

    @Test
    fun `when chosen date is one day before current, should return yesterday string`() {
        initMockString(YESTERDAY_STRING_ID)
        formatDateWithChosenDifferentFromCurrentBy(-1)
        verify(getStringMock).invoke(YESTERDAY_STRING_ID)
    }

    @Test
    fun `when chosen date is same as current, should return today string`() {
        initMockString(TODAY_STRING_ID)
        formatDateWithChosenDifferentFromCurrentBy(0)
        verify(getStringMock).invoke(TODAY_STRING_ID)
    }
}
