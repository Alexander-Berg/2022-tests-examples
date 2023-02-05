package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.clickOnRecyclerItemByText
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.findManyAndroid
import com.yandex.mail.testopithecus.steps.formatEmail
import com.yandex.mail.testopithecus.steps.getRecyclerItemByText
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.mail.testopithecus.steps.scrollToTop
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.AndroidRootSettings
import com.yandex.xplat.testopithecus.RootSettings
import io.qameta.allure.kotlin.Allure

class RootSettingsImpl(private val device: UiDevice) : RootSettings {

    override fun openRootSettings() {
        Allure.step("Открываем настройки") {
            device.clickOnRecyclerItemByText(getTextFromResources(R.string.action_settings))
        }
    }

    override fun isAboutCellExists(): Boolean {
        return Allure.step("Проверяем, есть ли пункт О приложении") {
            return@step device.getRecyclerItemByText(getTextFromResources(R.string.entry_settings_about)).isEnabled
        }
    }

    override fun isHelpAndFeedbackCellExists(): Boolean {
        return Allure.step("Проверяем, есть ли пункт Помощь") {
            return@step device.getRecyclerItemByText(getTextFromResources(R.string.entry_settings_support)).isEnabled
        }
    }

    override fun getAccounts(): YSArray<String> {
        return Allure.step("Получаем список аккаунтов") {
            scrollToTop()
            return@step device.findManyAndroid("title")
                .map { it.text }
                .filter { it.contains("@") }
                .map { formatEmail(it) }
                .toMutableList()
        }
    }

    override fun getTitle(): String {
        return Allure.step("Получаем заголовок") {
            return@step device.findByText(getTextFromResources(R.string.settings)).text
        }
    }

    override fun closeRootSettings() {
        Allure.step("Закрываем настройки") {
            device.pressBack()
        }
    }
}

class AndroidRootSettingsImpl(private val device: UiDevice) : AndroidRootSettings {
    override fun addAccount() {
        Allure.step("Тап на Добавить аккаунт") {
            device.clickOnRecyclerItemByText(getTextFromResources(R.string.entry_settings_add_account))
        }
    }

    override fun isAddAccountCellExists(): Boolean {
        return Allure.step("Проверяем, есть ли кнопка Добавить аккаунт") {
            return@step device.getRecyclerItemByText(getTextFromResources(R.string.entry_settings_add_account)).isEnabled
        }
    }
}
