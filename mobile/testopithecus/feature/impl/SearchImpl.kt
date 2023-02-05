package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.has
import com.yandex.xplat.testopithecus.Search
import io.qameta.allure.kotlin.Allure

class SearchImpl(private val device: UiDevice) : Search {

    override fun searchAllMessages() {
        searchByQuery("yandex")
    }

    override fun closeSearch() {
        Allure.step("Закрываем поиск") {
            onView(ViewMatchers.withContentDescription("Navigate up")).perform(click())
        }
    }

    override fun clearTextField() {
        Allure.step("Очищаем инпут поиска") {
            device.find("query_clear_button").click()
        }
    }

    override fun isInSearch(): Boolean {
        return device.has("query")
    }

    override fun isSearchedForMessages(): Boolean {
        return device.has("search_filter")
    }

    override fun openSearch() {
        Allure.step("Открываем поиск") {
            device.find("menu_search").click()
        }
    }

    override fun searchByQuery(query: String) {
        Allure.step("Ищем по тексту $query") {
            device.find("query").text = query
            device.pressSearch()
        }
    }
}
