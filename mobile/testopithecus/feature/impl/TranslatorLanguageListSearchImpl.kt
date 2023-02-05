package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.LanguageName
import com.yandex.xplat.testopithecus.TranslatorLanguageListSearch

class TranslatorLanguageListSearchImpl(private val device: UiDevice) : TranslatorLanguageListSearch {
    override fun tapOnSearchTextField() {
        device.find("query").click()
    }

    override fun isSearchTextFieldFocused(): Boolean {
        return device.find("query").isFocused
    }

    override fun tapOnCancelButton() {
        device.find("toolbar").children[0].click()
    }

    override fun enterSearchQuery(query: String) {
        device.find("query").text = query
    }

    override fun getSearchQuery(): String {
        return device.find("query").text
    }

    override fun getSearchedLanguageList(): YSArray<LanguageName> {
        return device
            .find("language_chooser_list")
            .parent
            .findMany("language_name")
            .map { it.text }
            .toMutableList()
    }

    override fun tapOnClearSearchFieldButton() {
        TODO("Not yet implemented")
    }
}
