package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.pages.MessageViewPage
import com.yandex.mail.testopithecus.pages.WebViewPage
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.has
import com.yandex.mail.testopithecus.steps.isMatchesAssertion
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.YSSet
import com.yandex.xplat.testopithecus.DefaultFolderName
import com.yandex.xplat.testopithecus.FullMessageView
import com.yandex.xplat.testopithecus.LabelName
import com.yandex.xplat.testopithecus.MessageViewer
import io.qameta.allure.kotlin.Allure

class MessageViewerImpl(private val device: UiDevice) : MessageViewer {

    private val messageViewPage: MessageViewPage = MessageViewPage()
    private val webViewPage: WebViewPage = WebViewPage("react_mail_view")

    override fun openMessage(order: Int) {
        Allure.step("Открываем сообщение с порядковым номером $order") {
            device.find("subject", order).click()
            Thread.sleep(3000)
        }
    }

    override fun isMessageOpened(): Boolean {
        return device.has("react_root")
    }

    override fun closeMessage() {
        Allure.step("Закрываем открытое сообщение") {
            if (onView(withContentDescription("Go back")).isMatchesAssertion(ViewAssertions.matches(ViewMatchers.isDisplayed()))) {
                onView(withContentDescription("Go back")).perform(click())
            } else onView(withContentDescription("Navigate up")).perform(click())
        }
    }

    override fun getOpenedMessage(): FullMessageView {
        return messageViewPage.getMessage()
    }

    override fun checkIfRead(): Boolean {
        return messageViewPage.isRead()
    }

    override fun checkIfSpam(): Boolean {
        return messageViewPage.getFolder() == DefaultFolderName.spam
    }

    override fun checkIfImportant(): Boolean {
        return messageViewPage.isImportant()
    }

    override fun getLabels(): YSSet<String> {
        var labelsList = webViewPage.getTextFromListOfElements("//*[@class='message-details__label']")
        if (labelsList.has("Important")) {
            labelsList.delete("Important")
        }
        return labelsList
    }

    override fun deleteLabelsFromHeader(labels: YSArray<LabelName>) {
        Allure.step("Снимаем метки $labels из шапки письма") {
            messageViewPage.expandMessageDetails()
            for (label in labels) {
                messageViewPage.clickOnMessageDetailsLabelInHeader(label)
            }
        }
    }

    override fun markAsUnimportantFromHeader() {
        Allure.step("Помечаем сообщение неважным через шапку письма") {
            messageViewPage.expandMessageDetails()
            messageViewPage.clickOnMessageDetailsLabelInHeader("Important")
        }
    }

    override fun arrowDownClick() {
        onView(ViewMatchers.withId((R.id.action_down))).perform(click())
    }

    override fun arrowUpClick() {
        onView(ViewMatchers.withId((R.id.action_up))).perform(click())
    }
}
