package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.debug.DebugUtils.sleep
import com.yandex.mail.testopithecus.pages.FolderListPage
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.getRecyclerItemByText
import com.yandex.xplat.testopithecus.ClearFolderInFolderList
import org.hamcrest.CoreMatchers.allOf

class ClearFolderImpl(private val device: UiDevice) : ClearFolderInFolderList {
    override fun clearSpam(confirmDeletionIfNeeded: Boolean) {
        device.getRecyclerItemByText(FolderListPage.SPAM).parent.find("folder_list_item_clear")!!.click()
        dealWithConfirmationPopup(confirmDeletionIfNeeded)
    }

    override fun doesClearSpamButtonExist(): Boolean {
        return device.getRecyclerItemByText(FolderListPage.SPAM).parent.find("folder_list_item_clear") != null
    }

    override fun clearTrash(confirmDeletionIfNeeded: Boolean) {
        device.getRecyclerItemByText(FolderListPage.TRASH).parent.find("folder_list_item_clear")?.click()
        dealWithConfirmationPopup(confirmDeletionIfNeeded)
    }

    override fun doesClearTrashButtonExist(): Boolean {
        return device.getRecyclerItemByText(FolderListPage.TRASH).parent.find("folder_list_item_clear") != null
    }

    private fun dealWithConfirmationPopup(confirmDeletionIfNeeded: Boolean) {
        if (confirmDeletionIfNeeded) {
            onView(allOf(withId(android.R.id.button1), withText("CLEAR"))).perform(ViewActions.click())
            sleep(2) // Там начинается какая-то гонка в приложении и список писем вообще не догружается
        } else {
            onView(allOf(withId(android.R.id.button2), withText("CANCEL"))).perform(ViewActions.click())
        }
    }
}
