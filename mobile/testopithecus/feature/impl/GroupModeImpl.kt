package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.steps.ActionList
import com.yandex.mail.testopithecus.steps.SHORT_TIMEOUT
import com.yandex.mail.testopithecus.steps.clickAtActionMenu
import com.yandex.mail.testopithecus.steps.clickOn
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.has
import com.yandex.mail.testopithecus.steps.isDialogShown
import com.yandex.xplat.common.YSSet
import com.yandex.xplat.testopithecus.GroupMode
import io.qameta.allure.kotlin.Allure

class GroupModeImpl(private val device: UiDevice) : GroupMode {
    private val shortSwipeActions = ShortSwipeMenuImpl(device)

    override fun selectMessage(byOrder: Int) {
        Allure.step("Выбираем сообщение с порядковым номером $byOrder") {
            device.find("message_icon", byOrder).click()
        }
    }

    override fun selectAllMessages() {
        Allure.step("Выбираем все сообщения") {
            clickOn(withContentDescription(ActionList.MORE_OPTIONS.actionName))
            clickAtActionMenu(ActionList.SELECT_ALL.actionName)
        }
    }

    override fun initialMessageSelect(byOrder: Int) {
        Allure.step("Выбираем сообщение с индексом $byOrder длинным тапом") {
            selectMessage(byOrder)
        }
    }

    override fun getSelectedMessages(): YSSet<Int> {
        val output: YSSet<Int> = YSSet()
        val rowCount = device.findMany("message_icon").size

        for (i: Int in 0 until rowCount) {
            if (device.find("message_icon", i).isSelected()) {
                output.add(i)
            }
        }
        return output
    }

    override fun markAsRead() {
        Allure.step("Помечаем выбранные сообщения прочитаннми") {
            clickOn(withId(R.id.mark_read))
        }
    }

    override fun markAsUnread() {
        Allure.step("Помечаем выбранные сообщения непрочитаннми") {
            clickOn(withId(R.id.mark_unread))
        }
    }

    override fun delete() {
        Allure.step("Удаляем выбранные сообщения") {
            clickOn(withId(R.id.delete))
            if (device.isDialogShown()) shortSwipeActions.acceptAndWaitForUndoToDisappear()
        }
    }

    override fun markAsImportant() {
        Allure.step("Помечаем выбранные сообщения важными") {
            clickOn(withContentDescription(ActionList.MORE_OPTIONS.actionName))
            clickAtActionMenu(ActionList.MARK_AS_IMPORTANT.actionName)
        }
    }

    override fun markAsUnimportant() {
        Allure.step("Помечаем выбранные сообщения неважными") {
            clickOn(withContentDescription(ActionList.MORE_OPTIONS.actionName))
            clickAtActionMenu(ActionList.UNMARK_AS_IMPORTANT.actionName)
        }
    }

    override fun markAsSpam() {
        Allure.step("Помечаем выбранные сообщения спамом") {
            clickOn(withId(R.id.mark_as_spam))
        }
    }

    override fun markAsNotSpam() {
        Allure.step("Помечаем выбранные сообщения неспамом") {
            clickOn(withId(R.id.mark_not_spam))
        }
    }

    override fun openMoveToFolderScreen() {
        Allure.step("Открываем экран перемещения в папку") {
            clickOn(withContentDescription(ActionList.MORE_OPTIONS.actionName))
            clickAtActionMenu(ActionList.MOVE_TO_FOLDER.actionName)
        }
    }

    override fun archive() {
        Allure.step("Перемещаем выбранные сообщения в архив") {
            clickOn(withContentDescription(ActionList.MORE_OPTIONS.actionName))
            clickAtActionMenu(ActionList.ARCHIVE.actionName)
        }
    }

    override fun unselectMessage(byOrder: Int) {
        Allure.step("Снимаем выдиление с письма под номером $byOrder") {
            device.find("message_icon", byOrder).click()
        }
    }

    override fun unselectAllMessages() {
        Allure.step("Снимаем выделение со всех выбранных писем") {
            clickOn(withContentDescription(ActionList.MORE_OPTIONS.actionName))
            clickAtActionMenu(ActionList.CANCEL_SELECTION.actionName)
        }
    }

    override fun getNumberOfSelectedMessages(): Int {
        return Allure.step("Получение количества выделенных писем") {
            return@step device.find("layout_action_mode_title").text.toInt()
        }
    }

    override fun isInGroupMode(): Boolean {
        return device.has("action_mode_close_button", SHORT_TIMEOUT) && device.has("layout_action_mode_title", SHORT_TIMEOUT)
    }

    override fun openApplyLabelsScreen() {
        Allure.step("Открываем экран добавления метки") {
            clickOn(withContentDescription(ActionList.MORE_OPTIONS.actionName))
            clickAtActionMenu(ActionList.LABELS.actionName)
        }
    }
}
