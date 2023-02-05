package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.PermissionActions
import com.yandex.mail.testopithecus.steps.find
import com.yandex.xplat.testopithecus.Compose

class ComposeImpl(private val device: UiDevice) : Compose {
    val permission = PermissionActions(device)

    override fun openCompose() {
        device.find("fab").click()
        if (permission.hasPermissionWindow()) {
            permission.pressAllow()
        }
    }

    override fun isComposeOpened(): Boolean {
        return true // TODO: Сделать нормально
    }

    override fun closeCompose(saveDraft: Boolean) {
        device.pressBack() // TODO: Сделать нормально
    }

    override fun sendMessage() {
        device.find("menu_send").click()
    }

    override fun isSendButtonEnabled(): Boolean {
        return device.find("menu_send").isEnabled // TODO: Сделать нормально
    }
}
