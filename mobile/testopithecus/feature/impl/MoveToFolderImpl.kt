package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.formatIfTabName
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.FolderName
import com.yandex.xplat.testopithecus.MoveToFolder
import io.qameta.allure.kotlin.Allure
import org.hamcrest.CoreMatchers.allOf

class MoveToFolderImpl(private val device: UiDevice) : MoveToFolder {
    override fun tapOnFolder(folderName: FolderName) {
        Allure.step("Перемещаем письмо в папку $folderName") {
            onView(allOf(ViewMatchers.withId(R.id.container_dialog_text), ViewMatchers.withText(formatIfTabName(folderName))))
                .perform(ViewActions.scrollTo(), ViewActions.click())
        }
    }

    override fun tapOnCreateFolder() {
        Allure.step("Открываем экран создания папки") {
            device.find("item_label_new_container").click()
            device.find("add_menu_action_add").click()
        }
    }

    override fun getFolderList(): YSArray<FolderName> {
        return Allure.step("Получаем список меток") {
            return@step device.findMany("item_label_dialog_text").map { it.text }.toMutableList()
        }
    }
}
