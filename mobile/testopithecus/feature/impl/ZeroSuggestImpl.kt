package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.shouldSee
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.ZeroSuggest
import io.qameta.allure.kotlin.Allure.step
import java.util.concurrent.TimeUnit

class ZeroSuggestImpl(private val device: UiDevice) : ZeroSuggest {
    override fun isShown(): Boolean {
        shouldSee(0, TimeUnit.SECONDS, ViewMatchers.withResourceName("item_search_history_text"))
        return true
    }
    override fun getZeroSuggest(): YSArray<String> {
        return device.findMany("item_search_history_text")
            .map { it.text }
            .toMutableList()
    }

    override fun searchByZeroSuggest(query: String) {
        step("Ищем по zero suggest $query") {
            device.find("item_search_history_text").text = query
            device.find("item_search_history_text").click()
        }
    }
}
