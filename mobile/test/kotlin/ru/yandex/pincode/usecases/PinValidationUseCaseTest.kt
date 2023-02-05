package ru.yandex.pincode.usecases

import android.os.Bundle
import android.view.View
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class PinValidationUseCaseTest {

    private val pinCode = "1111"

    private lateinit var useCase: PinValidationUseCase

    private lateinit var view: PinCodeView

    @Before
    fun runBeforeEachTest() {
        view = mock()
        useCase = PinValidationUseCase(view, pinCode)
    }

    @Test
    fun useCase_shouldFinishWithResult_whenPinCodeEnteredCorrectly() {
        useCase.init()
        verify(view).setForgotPinButtonVisibility(View.INVISIBLE)
        verify(view).showDots(0, 4)

        for (i in 1..4) {
            useCase.processInput(pinCode[i - 1].toString())
            verify(view).showDots(i, 4)
        }

        verify(view).finishWithResult(pinCode)
    }

    @Test
    fun useCase_shouldAlertAndShowForgotButton_whenPinCodeEnteredIncorrectly() {
        useCase.init()
        useCase.processInput("2222")

        verify(view).alert()
        verify(view, times(2)).showDots(0, 4)
        verify(view).setForgotPinButtonVisibility(View.VISIBLE)
    }

    @Test
    fun useCase_shouldRemoveLastCharacter_whenBackspaceWasPressed() {
        useCase.init()
        useCase.processInput(pinCode[0].toString())
        verify(view).showDots(1, 4)
        useCase.processInput("2")
        verify(view).showDots(2, 4)
        useCase.undoInput()
        verify(view, times(2)).showDots(1, 4)
        useCase.processInput(pinCode.substring(1..3))
        verify(view).finishWithResult(pinCode)
    }

    @Test
    fun useCase_shouldDoNothing_whenEmptyAndBackspaceWasPressed() {
        useCase.init()
        verify(view, times(1)).showDots(0, 4)
        for (i in 2..10) {
            useCase.undoInput()
            verify(view, times(i)).showDots(0, 4)
        }
        useCase.processInput(pinCode)
        verify(view).finishWithResult(pinCode)
    }

    @Test
    fun useCase_shouldSaveInput_onSaveInstanceState() {
        val input = "123"
        useCase.init()
        useCase.processInput(input)

        val bundle = mock<Bundle>()
        useCase.saveState(bundle)

        verify(bundle).putString(PinValidationUseCase.STATE_USER_INPUT, input)
    }

    @Test
    fun useCase_shouldRestoreInput_onRestoreInstanceState() {
        useCase.init()

        val input = "111"
        val bundle = mock<Bundle> {
            on { getString(PinValidationUseCase.STATE_USER_INPUT) } doReturn input
            on { getBoolean(PinValidationUseCase.STATE_FORGOT_BUTTON_VISIBLE) } doReturn false
        }

        useCase.restoreState(bundle)
        verify(view).showDots(input.length, 4)
        verify(view, times(2)).setForgotPinButtonVisibility(View.INVISIBLE)

        useCase.processInput("1")
        verify(view).finishWithResult(pinCode)
    }

    @Test
    fun useCase_shouldRestoreForgotButtonVisibility_onRestoreInstanceState() {
        useCase.init()

        val input = "111"
        val bundle = mock<Bundle> {
            on { getString(PinValidationUseCase.STATE_USER_INPUT) } doReturn input
            on { getBoolean(PinValidationUseCase.STATE_FORGOT_BUTTON_VISIBLE) } doReturn true
        }

        useCase.restoreState(bundle)
        verify(view).setForgotPinButtonVisibility(View.VISIBLE)
    }
}
