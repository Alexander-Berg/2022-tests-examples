package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.pages.MessageViewPage
import com.yandex.mail.testopithecus.pages.WebViewPage
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.xplat.testopithecus.LanguageName
import com.yandex.xplat.testopithecus.TranslatorBar

class TranslatorBarImpl(private val device: UiDevice) : TranslatorBar {
    private val messageViewPage: MessageViewPage = MessageViewPage()
    private val webViewPage: WebViewPage = WebViewPage("react_mail_view")

    override fun isTranslatorBarShown(): Boolean {
        return messageViewPage.isTranslatorShown()
    }

    override fun isMessageTranslated(): Boolean {
        val button_text = webViewPage.getText(XPATH_TRANSLATE_BUTTON)
        return button_text == getTextFromResources(R.string.translator_revert_translation)
    }

    override fun getSourceLanguage(): LanguageName {
        return webViewPage.getText("$XPATH_SOURCE_LANG//span/text()")
    }

    override fun tapOnSourceLanguage() {
        Web.onWebView(ViewMatchers.withResourceName("react_mail_view"))
            .withElement(DriverAtoms.findElement(Locator.XPATH, XPATH_SOURCE_LANG))
            .perform(DriverAtoms.webClick())
    }

    override fun getTargetLanguage(): LanguageName {
        return webViewPage.getText("$XPATH_TARGET_LANG//span/text()")
    }

    override fun tapOnTargetLanguage() {
        Web.onWebView(ViewMatchers.withResourceName("react_mail_view"))
            .withElement(DriverAtoms.findElement(Locator.XPATH, XPATH_TARGET_LANG))
            .perform(DriverAtoms.webClick())
    }

    override fun tapOnTranslateButton() {
        Web.onWebView(ViewMatchers.withResourceName("react_mail_view"))
            .withElement(DriverAtoms.findElement(Locator.XPATH, XPATH_TRANSLATE_BUTTON))
            .perform(DriverAtoms.webClick())
    }

    override fun tapOnRevertButton() {
        Web.onWebView(ViewMatchers.withResourceName("react_mail_view"))
            .withElement(DriverAtoms.findElement(Locator.XPATH, XPATH_TRANSLATE_BUTTON))
            .perform(DriverAtoms.webClick())
    }

    override fun getSubmitButtonLabel(): String {
        return webViewPage.getText(XPATH_TRANSLATE_BUTTON)
    }

    override fun tapOnCloseBarButton(hideTranslatorForThisLanguage: Boolean) {
        Web.onWebView(ViewMatchers.withResourceName("react_mail_view"))
            .withElement(DriverAtoms.findElement(Locator.XPATH, XPATH_TRANSLATE_CLOSE))
            .perform(DriverAtoms.webClick())
        if (hideTranslatorForThisLanguage) {
            device.findByText(getTextFromResources(R.string.translator_disable_dialog_disable_all).toUpperCase()).click()
        } else
            device.findByText(getTextFromResources(R.string.translator_disable_dialog_dismiss).toUpperCase()).click()
    }

    companion object {

        /**
         * translate button and close bar
         */
        private const val XPATH_CONTROLS = "//*[@aria-label='YOAI_ReactTranslatorControls']"

        private const val XPATH_TRANSLATE_BUTTON = "$XPATH_CONTROLS/button[1]"

        private const val XPATH_TRANSLATE_CLOSE = "$XPATH_CONTROLS/button[2]"

        /**
         * source and target langs
         */
        private const val XPATH_LANG_SELECTORS = "//*[@aria-label='YOAI_ReactTranslatorLangSelector']"

        private const val XPATH_SOURCE_LANG = "$XPATH_LANG_SELECTORS/button[1]"

        private const val XPATH_TARGET_LANG = "$XPATH_LANG_SELECTORS/button[2]"

    }
}
