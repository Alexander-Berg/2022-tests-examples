package com.yandex.mail.testopithecus.pages

import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.yandex.mail.testopithecus.steps.find

class DrawerViewPage(private val device: UiDevice) {

    fun getFolderListItem(order: Int): UiObject2 {
        return device.find(FOLDER_LIST_ITEM_TEXT_ID, order)
    }

    companion object {
        const val FOLDER_LIST_ITEM_TEXT_ID = "folder_list_item_text"
    }
}
