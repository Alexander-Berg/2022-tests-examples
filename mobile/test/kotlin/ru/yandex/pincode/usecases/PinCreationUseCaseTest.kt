package ru.yandex.pincode.usecases

import android.os.Bundle
import android.view.View
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import ru.yandex.pincode.R

class PinCreationUseCaseTest {

    private val pinCode = "1111"

    private val incorrectPinCode = "1234"

    private lateinit var useCase: PinCreationUseCase

    private lateinit var view: PinCodeView

    @Before
    fun runBeforeEachTest() {
        view = mock()
        useCase = PinCreationUseCase(view)
    }

    @Test
    fun useCase_shouldSetupView_onInit() {
        useCase.init()
        verify(view).setForgotPinButtonVisibility(View.GONE)
        verify(view).showDots(0, 4)
        verify(view).showTitle(R.string.enter_new_pin_code)
    }

    @Test
    fun useCase_shouldAskConfirmation_afterFirstTimeInput() {
        useCase.init()
        useCase.processInput(pinCode)
        verify(view, times(2)).showDots(0, pinCode.length)
        verify(view).showTitle(R.string.confirm_pin_code)
        verify(view, never()).alert()
        verify(view, never()).finishWithResult(anyOrNull())
    }

    @Test
    fun useCase_shouldFinishView_afterCorrectSecondTimeInput() {
        useCase.init()
        useCase.processInput(pinCode)
        useCase.processInput(pinCode)
        verify(view).finishWithResult(pinCode)
    }

    @Test
    fun useCase_shouldDoAlert_afterIncorrectSecondTimeInput() {
        useCase.init()
        useCase.processInput(pinCode)
        useCase.processInput(incorrectPinCode)
        verify(view).alert()
    }

    @Test
    fun useCase_shouldRemoveCharCorrectly_onFirstTimeInput() {
        useCase.init()
        verify(view).showDots(0, 4)
        useCase.processInput("12")
        verify(view).showDots(2, 4)
        useCase.undoInput()
        verify(view).showDots(1, 4)
        useCase.processInput("111")
        verify(view, times(2)).showDots(0, 4)
        useCase.processInput("1111")
        verify(view).finishWithResult("1111")
    }

    @Test
    fun useCase_shouldRemoveCharCorrectly_onSecondTimeInput() {
        useCase.init()
        verify(view).showDots(0, 4)
        useCase.processInput("1111")
        verify(view, times(2)).showDots(0, 4)
        useCase.processInput("112")
        verify(view).showDots(3, 4)
        useCase.undoInput()
        verify(view).showDots(2, 4)
        useCase.processInput("11")
        verify(view).finishWithResult("1111")
    }

    @Test
    fun useCase_shouldRemoveCharCorrectly_atBeginningOfFirstInput() {
        useCase.init()
        for (i in 0..10) {
            useCase.undoInput()
        }
        useCase.processInput(pinCode)
        useCase.processInput(pinCode)
        verify(view).finishWithResult(pinCode)
    }

    @Test
    fun useCase_shouldRemoveCharCorrectly_atBeginningOfSecondInput() {
        useCase.init()
        useCase.processInput(pinCode)
        for (i in 0..10) {
            useCase.undoInput()
        }
        useCase.processInput(pinCode)
        verify(view).finishWithResult(pinCode)
    }

    @Test
    fun useCase_shouldSaveStateCorrectly_atFistTimeInput() {
        useCase.init()
        val input = "12"
        useCase.processInput(input)
        val bundle = mock<Bundle>()

        useCase.saveState(bundle)

        verify(bundle).putString(PinCreationUseCase.STATE_FIRST_PIN, input)
        verify(bundle).putString(PinCreationUseCase.STATE_SECOND_PIN, "")
        verify(bundle).putInt(PinCreationUseCase.STATE_ATTEMPT_NUM, 0)
    }

    @Test
    fun useCase_shouldSaveStateCorrectly_atSecondTimeInput() {
        useCase.init()
        useCase.processInput(pinCode)
        val input = "12"
        useCase.processInput(input)
        val bundle = mock<Bundle>()

        useCase.saveState(bundle)

        verify(bundle).putString(PinCreationUseCase.STATE_FIRST_PIN, pinCode)
        verify(bundle).putString(PinCreationUseCase.STATE_SECOND_PIN, input)
        verify(bundle).putInt(PinCreationUseCase.STATE_ATTEMPT_NUM, 1)
    }

    @Test
    fun useCase_shouldRestoreStateCorrectly_atFirstTimeInput() {
        useCase.init()
        val input = "12"
        val bundle = mock<Bundle> {
            on { getString(PinCreationUseCase.STATE_FIRST_PIN) } doReturn input
            on { getString(PinCreationUseCase.STATE_SECOND_PIN) } doReturn ""
            on { getInt(PinCreationUseCase.STATE_ATTEMPT_NUM) } doReturn 0
        }

        useCase.restoreState(bundle)
        useCase.processInput("12")
        useCase.processInput("1212")
        verify(view).finishWithResult("1212")
    }

    @Test
    fun useCase_shouldRestoreStateCorrectly_atSecondTimeInput() {
        useCase.init()
        val input = pinCode.substring(0..1)
        val bundle = mock<Bundle> {
            on { getString(PinCreationUseCase.STATE_FIRST_PIN) } doReturn pinCode
            on { getString(PinCreationUseCase.STATE_SECOND_PIN) } doReturn input
            on { getInt(PinCreationUseCase.STATE_ATTEMPT_NUM) } doReturn 1
        }

        useCase.restoreState(bundle)
        useCase.processInput(pinCode.substring(2..3))
        verify(view).finishWithResult(pinCode)
    }
}
