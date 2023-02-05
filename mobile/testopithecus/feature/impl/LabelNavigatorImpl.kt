package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.yandex.mail.R
import com.yandex.mail.testopithecus.pages.DrawerViewPage
import com.yandex.mail.testopithecus.pages.DrawerViewPage.Companion.FOLDER_LIST_ITEM_TEXT_ID
import com.yandex.mail.testopithecus.steps.OPTIMAL_SWIPE_SPEED_IN_STEPS
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.scrollDrawerToTop
import com.yandex.mail.testopithecus.whileMax
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.LabelName
import com.yandex.xplat.testopithecus.LabelNavigator
import org.hamcrest.Matchers.allOf

class LabelNavigatorImpl(private val device: UiDevice) : LabelNavigator {

    private val drawerViewPage = DrawerViewPage(device)

    override fun getLabelList(): YSArray<LabelName> {
        val labelList: YSArray<LabelName> = mutableListOf()

        var item = 0 // start from 1 because Folder[0] = 'FOLDERS'
        var nameLabel = drawerViewPage.getFolderListItem(item).text

        whileMax({ nameLabel != LABEL_HEADER }) {
            item += 1
            val item = drawerViewPage.getFolderListItem(item)
            nameLabel = item.text
            scrollToItem(item)
        }
        nameLabel = drawerViewPage.getFolderListItem(item).text

        whileMax({ nameLabel != NEXT_TO_LABELS_ITEM_NAME }) {
            labelList.add(nameLabel)
            item += 1
            val item = drawerViewPage.getFolderListItem(item)
            nameLabel = item.text
            scrollToItem(item)
        }
        labelList.remove(LABEL_HEADER)

        return labelList
    }

    override fun goToLabel(labelName: String) {
        device.waitForIdle(3000)
        device.scrollDrawerToTop()
        scrollToItem(device.findMany(FOLDER_LIST_ITEM_TEXT_ID).last())
        onView(allOf(withParent(withId(R.id.folder_list_item_container_info)), withText(labelName))).perform(click())
    }

    private fun scrollToItem(item: UiObject2) {
        val positionY = item.visibleCenter.y
        device.swipe(10, positionY, 10, 10, OPTIMAL_SWIPE_SPEED_IN_STEPS)
    }

    companion object {
        private const val LABEL_HEADER = "LABELS"
        private const val NEXT_TO_LABELS_ITEM_NAME = "Manage subscriptions"
    }
}
