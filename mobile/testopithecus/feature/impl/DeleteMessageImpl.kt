package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.LongSwipeBaseActionsImpl
import com.yandex.xplat.testopithecus.DeleteMessage
import io.qameta.allure.kotlin.Allure

class DeleteMessageImpl(private val device: UiDevice) : DeleteMessage {
    private val swipeBaseActions = LongSwipeBaseActionsImpl(device)

    override fun deleteMessage(order: Int) {
        Allure.step("Удаляем сообщение с порядковым номером $order при помощи long swipe") {
            swipeBaseActions.longSwipeLeft(order)
        }
    }
}
