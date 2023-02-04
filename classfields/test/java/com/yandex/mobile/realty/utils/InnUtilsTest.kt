package com.yandex.mobile.realty.utils

import com.yandex.mobile.realty.domain.model.InnValidation
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author andrey-bgm on 12/10/2021.
 */
class InnUtilsTest {

    @Test
    fun validateNaturalInnWhenValid() {
        assertEquals(InnValidation.Ok, validateNaturalInn("500100732259"))
    }

    @Test
    fun validateNaturalInnWhenEmpty() {
        assertEquals(
            InnValidation.Error(InnValidation.ErrorReason.EMPTY),
            validateNaturalInn("")
        )
    }

    @Test
    fun validateNaturalInnWhenZeros() {
        assertEquals(
            InnValidation.Error(InnValidation.ErrorReason.WRONG_INN),
            validateNaturalInn("000000000000")
        )
    }

    @Test
    fun validateNaturalInnWhenWrongControlNumbers() {
        assertEquals(
            InnValidation.Error(InnValidation.ErrorReason.WRONG_INN),
            validateNaturalInn("012345678912")
        )
    }

    @Test
    fun validateNaturalInnWhenTooShort() {
        assertEquals(
            InnValidation.Error(InnValidation.ErrorReason.WRONG_INN),
            validateNaturalInn("7830002293")
        )
    }

    @Test
    fun validateNaturalInnWhenTooLong() {
        assertEquals(
            InnValidation.Error(InnValidation.ErrorReason.WRONG_INN),
            validateNaturalInn("5001007322590")
        )
    }

    @Test
    fun validateNaturalInnWhenNotOnlyDigits() {
        assertEquals(
            InnValidation.Error(InnValidation.ErrorReason.WRONG_INN),
            validateNaturalInn("50010073225w")
        )
    }
}
