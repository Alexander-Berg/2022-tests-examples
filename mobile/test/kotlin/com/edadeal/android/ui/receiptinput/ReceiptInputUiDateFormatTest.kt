package com.edadeal.android.ui.receiptinput

import androidx.annotation.StringRes
import com.edadeal.android.model.Time
import com.edadeal.android.model.addDays
import com.nhaarman.mockito_kotlin.whenever

interface ReceiptInputUiDateFormatTest {

    val getStringMock: (Int) -> String

    fun initMockString(@StringRes stringId: Int) {
        whenever(getStringMock(stringId)).thenReturn("")
    }

    fun formatDateWithChosenDifferentFromCurrentBy(days: Int): String? {
        val year = 2018
        val month = 8
        val day = 14

        val time = Time()
        val chosenCalendar = time.new(year, month, day).addDays(days)
        val todayCalendar = time.new(year, month, day)

        return ReceiptInputUi.formatDate(getStringMock, chosenCalendar, todayCalendar)
    }
}
