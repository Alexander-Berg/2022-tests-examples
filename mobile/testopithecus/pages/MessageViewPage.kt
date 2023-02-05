package com.yandex.mail.testopithecus.pages

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import com.yandex.xplat.common.YSSet
import com.yandex.xplat.testopithecus.FolderName
import com.yandex.xplat.testopithecus.FullMessage
import com.yandex.xplat.testopithecus.Message
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MessageViewPage {
    private val webViewPage: WebViewPage = WebViewPage("react_mail_view")

    private val openedMessageSelector: String = "//div[contains(@class, 'message-snippet__expand')]"

    fun goToActionMenu() {
        webViewPage.clickByXpath("//span[@data-action='more']")
    }

    fun expandMessageDetails() {
        Web.onWebView(ViewMatchers.withResourceName("react_mail_view"))
            .withElement(DriverAtoms.findElement(Locator.XPATH, "//*[contains(@class, 'message-from__details')]"))
            .perform(DriverAtoms.webClick())
    }

    fun getMessage(): FullMessage {
        val from: String = webViewPage.getText("$openedMessageSelector//div[@class='message-from']//span[@class='message-details__yabble__text']")
        val subject: String = webViewPage.getText("//h1[@class='thread-subject__text']")
        val timestamp: Long = getTimestamp()
        val threadCounter: Int? = getThreadCounter()
        val read: Boolean = isRead()
        val important: Boolean = isImportant()
        val body: String = webViewPage.getText("//*[contains(@class,' message-body ')]")
        val to: YSSet<String> = getTo()
        val head = Message(from, subject, timestamp, body, threadCounter, read, important)
        return FullMessage(head, to, body)
    }

    fun isImportant(): Boolean {
        return webViewPage.isElementExists("$openedMessageSelector//*[contains(@class,'important')]")
    }

    fun isTranslatorShown(): Boolean {
        return webViewPage.isElementExists("$openedMessageSelector//*[contains(@class, 'translator')]")
    }

    fun clickOnMessageDetailsLabelInHeader(name: String) {
        webViewPage.clickNextSpan(name)
    }

    private fun getThreadCounter(): Int? {
        if (webViewPage.isElementExists("//span[@class='thread-subject__counts-total']")) {
            val counterString = webViewPage.getHtmlData("//span[@class='thread-subject__counts-total']")
            val pattern = "[0-9]+".toRegex()
            if (pattern.find(counterString) != null) {
                val counter = pattern.find(counterString)!!.groupValues[0].toInt()
                if (counter != 1) {
                    return counter
                }
            }
        }
        return null
    }

    fun getTimestamp(): Long {
        val date = webViewPage.getText("$openedMessageSelector//*[@aria-label='YOAI_ReactMessageHeaderDate']//span/text()")
        return LocalDateTime.parse(
            date,
            DateTimeFormatter.ofPattern("dd MMM yyyy',' HH:mm", Locale.ENGLISH)
        ).toEpochSecond(OffsetDateTime.now().offset) * 1000
    }

    fun getTo(): YSSet<String> {
        return webViewPage.getTextFromListOfElements("$openedMessageSelector//*[@class='message-details']//*[@class='message-details__yabble__text']")
    }

    fun getFolder(): FolderName {
        return webViewPage.getText("//*[@class='message-details__folder']")
    }

    fun isRead(): Boolean {
        return !webViewPage.isElementExists("$openedMessageSelector//span[@class='unread_ico']")
    }
}
