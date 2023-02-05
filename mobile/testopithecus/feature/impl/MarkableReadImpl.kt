package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.OPTIMAL_SWIPE_SPEED_IN_PIXELS
import com.yandex.mail.testopithecus.steps.find
import com.yandex.xplat.testopithecus.MarkableRead
import io.qameta.allure.kotlin.Allure

class MarkableReadImpl(private val device: UiDevice) : MarkableRead {
    override fun markAsRead(order: Int) {
        Allure.step("Отмечаем сообщение с порядковым номером $order прочитанным") {
            swipeRight(order)
        }
    }

    override fun markAsUnread(order: Int) {
        Allure.step("Отмечаем сообщение с порядковым номером $order непрочитанным") {
            swipeRight(order)
        }
    }

    private fun swipeRight(order: Int) {
        device.find("subject", order).swipe(Direction.RIGHT, 1.0f, OPTIMAL_SWIPE_SPEED_IN_PIXELS)
    }
}
