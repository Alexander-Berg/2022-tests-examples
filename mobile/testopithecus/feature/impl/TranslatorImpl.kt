package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.mail.testopithecus.steps.isMatchesAssertion
import com.yandex.mail.testopithecus.steps.scrollToObjectIfNeeded
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.LanguageName
import com.yandex.xplat.testopithecus.TranslatorSettings

class TranslatorImpl(private val device: UiDevice) : TranslatorSettings {
    override fun switchTranslator() {
        val elementName = getTextFromResources(R.string.entry_settings_translator)
        scrollToObjectIfNeeded(elementName)

        device
            .find("recycler_view")
            .findObject(By.text(elementName))
            .parent
            .parent
            .find("switchWidget")
            ?.click()
    }

    override fun isTranslatorEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_translator)
        scrollToObjectIfNeeded(elementName)

        return device
            .find("recycler_view")
            .findObject(By.text(elementName))
            .parent
            .parent
            .find("switchWidget")
            ?.isChecked!!
    }

    override fun isIgnoredLanguageCellShown(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_disabled_languages)
        return onView(ViewMatchers.withText(elementName)).isMatchesAssertion(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    override fun openIgnoredTranslationLanguageList() {
        val elementName = getTextFromResources(R.string.entry_settings_disabled_languages)
        scrollToObjectIfNeeded(elementName)
        device.findByText(elementName).click()
    }

    override fun removeTranslationLanguageFromIgnored(language: LanguageName) {
        device
            .find("language_chooser_list")
            .findObject(By.text(language))
            .parent
            .parent
            .find("language_delete_button")
            ?.click()
    }

    override fun getIgnoredTranslationLanguages(): YSArray<LanguageName> {
        return device.findMany("language_name")
            .map { it.text }
            .toMutableList()
    }

    override fun closeIgnoredTranslationLanguageList() {
        val elementName = getTextFromResources(R.string.entry_settings_translator)
        if (!onView(ViewMatchers.withText(elementName)).isMatchesAssertion(ViewAssertions.matches(ViewMatchers.isDisplayed()))) {
            device.pressBack()
        }
    }

    override fun openDefaultTranslationLanguageList() {
        val elementName = getTextFromResources(R.string.entry_settings_default_target_language)
        scrollToObjectIfNeeded(elementName)
        device.findByText(elementName).click()
    }

    override fun setDefaultTranslationLanguage(language: LanguageName) {
        device.find("query").text = language
        device.findByText(language, 1).click()
    }

    override fun getDefaultTranslationLanguage(): LanguageName {
        val elementName = getTextFromResources(R.string.entry_settings_default_target_language)
        scrollToObjectIfNeeded(elementName)

        return device
            .find("recycler_view")
            .findObject(By.text(elementName))
            .parent
            .parent
            .find("pref_text_textview")?.text!!
    }

    override fun getDefaultTranslationLanguageFromGeneralSettingsPage(): LanguageName {
        val elementName = getTextFromResources(R.string.entry_settings_default_target_language)
        scrollToObjectIfNeeded(elementName)

        return device
            .find("recycler_view")
            .findObject(By.text(elementName))
            .parent
            .parent
            .find("pref_text_textview")?.text!!
    }

    override fun closeDefaultTranslationLanguageList() {
        val elementName = getTextFromResources(R.string.entry_settings_translator)
        if (!onView(ViewMatchers.withText(elementName)).isMatchesAssertion(ViewAssertions.matches(ViewMatchers.isDisplayed()))) {
            device.pressBack()
        }
    }
}
