package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.steps.ActionList
import com.yandex.mail.testopithecus.pages.MessageViewPage
import com.yandex.mail.testopithecus.steps.ShortSwipeBaseActionsImpl
import com.yandex.mail.testopithecus.steps.acceptDialog
import com.yandex.mail.testopithecus.steps.clickAtActionMenu
import com.yandex.mail.testopithecus.steps.declineDialog
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.has
import com.yandex.mail.testopithecus.steps.isDialogShown
import com.yandex.mail.testopithecus.steps.isMatchesAssertion
import com.yandex.mail.testopithecus.steps.isTablet
import com.yandex.mail.testopithecus.steps.scrollToBottom
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.ContextMenu
import com.yandex.xplat.testopithecus.MessageActionName
import com.yandex.xplat.testopithecus.ShortSwipe
import io.qameta.allure.kotlin.Allure
import org.hamcrest.CoreMatchers.allOf

class ShortSwipeMenuImpl(private val device: UiDevice) : ContextMenu, ShortSwipe {
    val shortSwipeBaseActions = ShortSwipeBaseActionsImpl(device)
    private val messageViewPage: MessageViewPage = MessageViewPage()

    override fun openFromShortSwipe(order: Int) {
        Allure.step("Открытие меню действий с письмом через short swipe для $order письма") {
            shortSwipeBaseActions.openShortSwipeMenu(order)
        }
    }

    override fun openFromMessageView() {
        Allure.step("Открытие меню действий с письмом из просмотра письма") {
            messageViewPage.goToActionMenu()
            device.has("button2")
        }
    }

    override fun close() {
        Allure.step("Закрытие меню действий с письмом") {
            device.declineDialog()
        }
    }

    override fun getAvailableActions(): YSArray<MessageActionName> {
        return Allure.step("Получаем список доступных действий с письмом") {
            val actions = device.findMany("message_action_text_view").map { it.text }.toMutableList()
            return@step formatMessageActionName(actions)
        }
    }

    private fun formatMessageActionName(actions: YSArray<MessageActionName>): YSArray<MessageActionName> {
        val formattedActionNames = mutableListOf<MessageActionName>()
        actions.forEach {
            when (it) {
                "Reply to all" -> formattedActionNames.add(ActionList.REPLAY_ALL.actionName)
                "Mark as read" -> formattedActionNames.add(ActionList.READ.actionName)
                "Mark as unread" -> formattedActionNames.add(ActionList.UNREAD.actionName)
                "Mark as important" -> formattedActionNames.add(ActionList.MARK_AS_IMPORTANT.actionName)
                "Unmark as important" -> formattedActionNames.add(ActionList.UNMARK_AS_IMPORTANT.actionName)
                "Not spam" -> formattedActionNames.add(ActionList.NOT_SPAM.actionName)
//                "Labels" -> formattedActionNames.add(ActionList.LABELS.actionName)  after hardcode experiment with new icon in both clients
                "Labels" -> formattedActionNames.add("Apply label")
                "Spam" -> formattedActionNames.add("Spam!")
                else -> if ((it != "Message properties") && (it != "Эмулировать пуш")) formattedActionNames.add(it)
            }
        }
        return formattedActionNames
    }

    override fun openReplyCompose() {
        Allure.step("Открываем написание ответа") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.REPLAY)
        }
    }

    override fun openReplyAllCompose() {
        Allure.step("Открываем написание ответа всем") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.REPLAY_ALL)
        }
    }

    override fun openForwardCompose() {
        Allure.step("Открываем пересылку письма") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.FORWARD)
        }
    }

    override fun deleteMessage() {
        Allure.step("Удаляем письмо") {
            val isTrashFolder: Boolean = onView(allOf(withParent(withId(R.id.toolbar)), withText("Trash")))
                .isMatchesAssertion(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.DELETE)
            if (isTrashFolder) {
                onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(ViewActions.click())
            }
        }
    }

    override fun markAsRead() {
        Allure.step("Помечаем письмо прочитанным") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.READ)
        }
    }

    override fun markAsUnread() {
        Allure.step("Помечаем письмо непрочитанным") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.UNREAD)
        }
    }

    override fun markAsImportant() {
        Allure.step("Помечаем письмо важным") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.MARK_AS_IMPORTANT)
        }
    }

    override fun markAsUnimportant() {
        Allure.step("Помечаем письмо неважным") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.UNMARK_AS_IMPORTANT)
        }
    }

    override fun markAsSpam() {
        Allure.step("Помечаем письмо спамом") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.SPAM)
        }
    }

    override fun markAsNotSpam() {
        Allure.step("Помечаем письмо неспамом") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.NOT_SPAM)
        }
    }

    override fun openApplyLabelsScreen() {
        Allure.step("Открытие экрана выбора метки") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.LABELS)
        }
    }

    override fun openMoveToFolderScreen() {
        Allure.step("Открытие экрана выбора папки для перемещения письма") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.MOVE_TO_FOLDER)
        }
    }

    override fun archive() {
        Allure.step("Архивируем письмо") {
            shortSwipeBaseActions.performShortSwipeMenuAction(ActionList.ARCHIVE)
        }
    }

    override fun showTranslator() {
        Allure.step("Тап на Показать переводчик") {
            clickAtActionMenu(R.string.show_translator)
        }
    }

    override fun deleteMessageByShortSwipe(order: Int) {
        Allure.step("Удаяем письмо под номером $order из свайп меню кликом") {
            device.find("sender", order)
            if (device.displayRotation == 1) {
                if (!isTablet()) scrollToBottom()
                shortSwipeBaseActions.shortSwipeLeftLandscape(order)
            } else {
                shortSwipeBaseActions.shortSwipeLeft(order)
            }
            device.find("swipe_action_dismiss_container").click()
            if (device.isDialogShown())
                acceptAndWaitForUndoToDisappear()
        }
    }

    override fun archiveMessageByShortSwipe(order: Int) {
        Allure.step("Архивируем письмо под номером $order из свайп меню кликом") {
            shortSwipeBaseActions.shortSwipeLeft(order)
            device.find("swipe_action_dismiss_container").click()
        }
    }

    override fun markAsRead(order: Int) {
        Allure.step("Отмечаем письмо номер $order как прочитанное") {
            shortSwipeBaseActions.shortSwipeRight(order)
        }
    }

    override fun markAsUnread(order: Int) {
        Allure.step("Отмечаем письмо номер $order как непрочитанное") {
            shortSwipeBaseActions.shortSwipeRight(order)
        }
    }

    fun acceptAndWaitForUndoToDisappear() {
        Allure.step("Подтверждаем удаление и ждём, пока пропадёт Undo") {
            device.acceptDialog()
            Thread.sleep(5000)
        }
    }
}
