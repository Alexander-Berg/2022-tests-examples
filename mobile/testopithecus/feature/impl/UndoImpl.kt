package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.yandex.mail.testopithecus.steps.SHORT_TIMEOUT
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.has
import com.yandex.xplat.testopithecus.Undo
import com.yandex.xplat.testopithecus.UndoState
import io.qameta.allure.kotlin.Allure

class UndoImpl(private val device: UiDevice) : Undo {
    private fun undo() {
        Allure.step("Жмём на Отменить") {
            device.find("snackbar_action").click()
        }
    }

    private fun undoShown(): UndoState {
        if (device.has("snackbar_action", SHORT_TIMEOUT)) {
            return UndoState.shown
        }
        return UndoState.notShown
    }

    override fun undoDelete() {
        undo()
    }

    override fun undoArchive() {
        undo()
    }

    override fun undoSpam() {
        undo()
    }

    override fun undoSending() {
        undo()
    }

    override fun isUndoDeleteToastShown(): UndoState {
        if ((undoShown() == UndoState.shown) && (getSnackBar().text.contains("delete", true))) {
            return UndoState.shown
        }
        return UndoState.notShown
    }

    override fun isUndoArchiveToastShown(): UndoState {
        if ((undoShown() == UndoState.shown) && (getSnackBar().text.contains("archive", true))) {
            return UndoState.shown
        }
        return UndoState.notShown
    }

    override fun isUndoSpamToastShown(): UndoState {
        if ((undoShown() == UndoState.shown) && (getSnackBar().text.contains("spam", true))) {
            return UndoState.shown
        }
        return UndoState.notShown
    }

    override fun isUndoSendingToastShown(): UndoState {
        if ((undoShown() == UndoState.shown) && (getSnackBar().text.contains("send", true))) {
            return UndoState.shown
        }
        return UndoState.notShown
    }

    private fun getSnackBar(): UiObject2 {
        return device.find("snackbar_text", 0, SHORT_TIMEOUT)
    }
}
