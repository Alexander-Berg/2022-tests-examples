package com.edadeal.android.model.webapp.payment

import com.edadeal.android.model.webapp.handler.payment.PaymentError
import com.edadeal.android.model.webapp.handler.payment.PaymentMethodData
import com.edadeal.android.model.webapp.handler.payment.PaymentSettingsBuilder
import com.yandex.payment.sdk.model.data.CardBinValidationConfig
import com.yandex.payment.sdk.model.data.CardExpirationDateValidationConfig
import com.yandex.payment.sdk.model.data.ResultScreenClosing.Companion.HIDE_FINAL_STATE
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class PaymentSettingsBuilderTest {

    @Test
    fun `should build correct settings`() {
        val settings = PaymentSettingsBuilder(PaymentMethodData.Data(hideStatus = true))
            .withExpirationDate(
                PaymentMethodData.Validation.ExpirationDate(
                    minYear = 2099,
                    minMonth = 12
                )
            )
            .withBinRanges(
                PaymentMethodData.Validation.BinRanges(
                    ranges = listOf("22000000-22049999")
                )
            )
            .build()

        assertTrue(settings.useBindingV2)
        assertFalse(settings.resultScreenClosing.showButton)
        assertEquals(HIDE_FINAL_STATE, settings.resultScreenClosing.delayToAutoHide)
        assertNotSame(CardBinValidationConfig.Default, settings.cardValidationConfig.binConfig)
        assertNotSame(CardExpirationDateValidationConfig.Default, settings.cardValidationConfig.expirationDateConfig)
    }

    @Test
    fun `should throw error if validation bin ranges are incorrect`() {
        val data = PaymentMethodData.Data()
        val incorrectRanges = PaymentMethodData.Validation.BinRanges(ranges = listOf("111"))

        assertFailsWith<PaymentError> { PaymentSettingsBuilder(data).withBinRanges(incorrectRanges) }
    }

    @Test
    fun `should throw error if validation expiration date is missing or incorrect`() {
        val data = PaymentMethodData.Data()
        val missingDate = PaymentMethodData.Validation.ExpirationDate(minYear = null, minMonth = null)
        val incorrectDate = PaymentMethodData.Validation.ExpirationDate(minYear = 219, minMonth = 123)

        assertFailsWith<PaymentError> { PaymentSettingsBuilder(data).withExpirationDate(missingDate) }
        assertFailsWith<PaymentError> { PaymentSettingsBuilder(data).withExpirationDate(incorrectDate) }
    }
}
