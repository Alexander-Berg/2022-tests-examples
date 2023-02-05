package ru.yandex.supercheck.presentation.common.barcode

import org.junit.Assert.assertFalse
import org.junit.Test

class BarcodeValidatorTest {

    @Test
    fun barcodeValidator_testValid() {
        val barcodeValidator = BarcodeValidator()
        assert(barcodeValidator.validate("9330071314999"))
        assert(barcodeValidator.validate("46147202"))
        assert(barcodeValidator.validate("4601662000634"))
        assertFalse(barcodeValidator.validate(""))
    }
}