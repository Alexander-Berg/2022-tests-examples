package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.steps.ActionList
import com.yandex.mail.testopithecus.steps.ShortSwipeBaseActionsImpl
import com.yandex.xplat.testopithecus.ArchiveMessage
import io.qameta.allure.kotlin.Allure

class ArchiveMessageShortSwipeMenuImpl(private val device: UiDevice) : ArchiveMessage {
    val shortSwipeBaseActions = ShortSwipeBaseActionsImpl(device)

    override fun archiveMessage(order: Int) {
        Allure.step("Архивируем сообщение с порядковым номером $order") {
            shortSwipeBaseActions.shortSwipeMenuAction(order, ActionList.ARCHIVE)
        }
    }
}
