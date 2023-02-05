package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.yandex.mail.testopithecus.steps.OPTIMAL_SWIPE_SPEED_IN_STEPS
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.scrollDrawerToTop
import com.yandex.xplat.testopithecus.FilterNavigator

class FilterNavigatorImpl(private val device: UiDevice) : FilterNavigator {

    private fun scrollToItem(item: UiObject2) {
        val positionY = item.visibleCenter.y
        device.swipe(10, positionY, 10, 10, OPTIMAL_SWIPE_SPEED_IN_STEPS)
    }

    override fun goToFilterImportant() {
        goToFilter("Important")
    }

    private fun goToFilter(labelName: String) {
        Thread.sleep(3000)
        device.scrollDrawerToTop()
        while (
            !device.findMany("container_scroll").first().hasObject(text(labelName)) &&
            !device.findMany("container_scroll").first().hasObject(text("Log out"))
        ) {
            device.findMany("container_scroll").first().scroll(Direction.DOWN, 0.5F)
        }
        device.findMany("container_scroll").first().findObject(text(labelName)).click()
    }

    override fun goToFilterUnread() {
        goToFilter("Unread")
    }

    override fun goToFilterWithAttachments() {
        goToFilter("With attachments")
    }
}
