package com.edadeal.android.ui.receiptinput

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.anyInt
import kotlin.test.assertFalse

@RunWith(Parameterized::class)
class ReceiptInputUiDateFormatDiffersDateTest(private val days: Int) : ReceiptInputUiDateFormatTest {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Any> = listOf(2, -5, 7, -11, 15, -21, 29, -40, 55, -76)
    }

    override val getStringMock: (Int) -> String = mock()

    @Test
    fun `when selected date differs from the current date by more than 2 days, should not use any strings`() {
        val resultString = formatDateWithChosenDifferentFromCurrentBy(days)
        verify(getStringMock, never()).invoke(anyInt())
        assertFalse(resultString.isNullOrEmpty())
    }
}
