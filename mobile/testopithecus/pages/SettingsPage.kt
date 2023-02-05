package com.yandex.mail.testopithecus.pages

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.yandex.mail.testopithecus.steps.find
import com.yandex.xplat.testopithecus.ActionOnSwipe
import com.yandex.xplat.testopithecus.CancelSendingOption

class SettingsPage {
    companion object {
        val actionOnSwipe = mutableMapOf(
            "Deletion" to ActionOnSwipe.delete,
            "Archiving" to ActionOnSwipe.archive
        )

        val optionCancelSendingEmail = mutableMapOf(
            "Turn off" to CancelSendingOption.turnOff,
            "3 seconds" to CancelSendingOption.threeSeconds,
            "5 seconds" to CancelSendingOption.fiveSeconds,
            "10 seconds" to CancelSendingOption.tenSeconds
        )
    }

    fun getToggle(device: UiDevice, elementName: String): UiObject2 {
        return device
            .find("recycler_view")
            .findObject(By.text(elementName))
            .parent
            .parent
            .find("switchWidget")!!
    }
}
