package com.yandex.mail.testopithecus.steps

import androidx.test.uiautomator.UiDevice

open class LongSwipeBaseActionsImpl(private val device: UiDevice) {

    fun longSwipeLeft(order: Int) {
        // Иначе будет открываться dev меню
        val content = device.find("content", order, timeout = 10000)
        val centerX = (content.visibleCenter.x * 1.5).toInt()
        val centerY = content.visibleCenter.y
        device.swipe(centerX, centerY, 0, centerY, OPTIMAL_SWIPE_SPEED_IN_STEPS)
    }
}
