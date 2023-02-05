package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso
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
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.LanguageName
import com.yandex.xplat.testopithecus.TranslatorLanguageList

class TranslatorLanguageListImpl(private val device: UiDevice) : TranslatorLanguageList {
    override fun getAllSourceLanguages(): YSArray<LanguageName> {
        return device
            .find("language_chooser_list")
            .parent
            .findMany("language_name")
            .map { it.text }
            .toMutableList()
    }

    override fun setSourceLanguage(language: LanguageName) {
        device.find("query").text = language
        device.findByText(language, 1).click()
    }

    override fun getCurrentSourceLanguage(): LanguageName? {
        return device
            .find("language_chooser_list")
            .findObject(By.text(getTextFromResources(R.string.translator_language_chooser_chosen_language_label)))
            .parent
            .find("language_name")?.text
    }

    override fun getDeterminedAutomaticallySourceLanguage(): LanguageName {
        return device
            .find("language_chooser_list")
            .findObject(By.text(getTextFromResources(R.string.translator_language_chooser_detected_by_system_label)))
            .parent
            .find("language_name")?.text!!
    }

    override fun getRecentSourceLanguages(): YSArray<LanguageName> {
        val langList = device
            .find("language_chooser_list")
            .findObjects(By.text(getTextFromResources(R.string.translator_language_chooser_recent_label)))

        val recentLangs: YSArray<LanguageName> = mutableListOf()
        for (i in 0..langList.size - 1) {
            recentLangs.add(langList[i].parent.find("language_name")?.text!!)
        }
        return recentLangs
    }

    override fun getAllTargetLanguages(): YSArray<LanguageName> {
        return device
            .find("language_chooser_list")
            .parent
            .findMany("language_name")
            .map { it.text }
            .toMutableList()
    }

    override fun setTargetLanguage(language: LanguageName, addToRecent: Boolean) {
        device.find("query").text = language
        device.findByText(language, 1).click()
    }

    override fun getCurrentTargetLanguage(): LanguageName? {
        val elementName = getTextFromResources(R.string.translator_language_chooser_chosen_language_label)
        if (Espresso.onView(ViewMatchers.withText(elementName)).isMatchesAssertion(ViewAssertions.matches(ViewMatchers.isDisplayed()))) {
            return device
                .find("language_chooser_list")
                .findObject(By.text(getTextFromResources(R.string.translator_language_chooser_chosen_language_label)))
                .parent
                .find("language_name")?.text
        } else
            return null
    }

    override fun getDefaultTargetLanguage(): LanguageName {
        return device
            .find("language_chooser_list")
            .findObject(By.text(getTextFromResources(R.string.translator_language_chooser_default_language_label)))
            .parent
            .find("language_name")?.text!!
    }

    override fun getRecentTargetLanguages(): YSArray<LanguageName> {
        val elementName = getTextFromResources(R.string.translator_language_chooser_recent_label)
        if (Espresso.onView(ViewMatchers.withText(elementName)).isMatchesAssertion(ViewAssertions.matches(ViewMatchers.isDisplayed()))) {
            return device
                .find("language_chooser_list")
                .findObject(By.text(getTextFromResources(R.string.translator_language_chooser_recent_label)))
                .parent
                .findMany("language_name")
                .map { it.text }
                .toMutableList()
        } else
            return mutableListOf()
    }
}
