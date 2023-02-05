package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.steps.ActionList
import com.yandex.mail.testopithecus.steps.ShortSwipeBaseActionsImpl
import com.yandex.xplat.testopithecus.Spamable
import io.qameta.allure.kotlin.Allure

class SpamMessageShortSwipeMenuImpl(private val device: UiDevice) : Spamable {
    val shortSwipeBaseActions = ShortSwipeBaseActionsImpl(device)

    override fun moveToSpam(order: Int) {
        Allure.step("Отправляем сообщение с порядковым номером $order в спам") {
            shortSwipeBaseActions.shortSwipeMenuAction(order, ActionList.SPAM)
        }
    }

    override fun moveFromSpam(order: Int) {
        Allure.step("Отмечаем сообщение с порядковым номером $order как не спам") {
            shortSwipeBaseActions.shortSwipeMenuAction(order, ActionList.NOT_SPAM)
        }
    }
}
