package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.clickOnRecyclerItemByText
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.getElementInListByText
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.xplat.testopithecus.AboutSettings
import io.qameta.allure.kotlin.Allure
import java.time.LocalDateTime

class AboutSettingsImpl(private val device: UiDevice) : AboutSettings {
    private val PATTERN_VERSION = "^\\d+(\\.\\d+)(\\.\\d+)\$".toRegex()
    private val VALID_DATA_COPYRIGHT = "© 2001–${LocalDateTime.now().year} «Yandex»"

    override fun openAboutSettings() {
        Allure.step("Открыть страницу About") {
            device.clickOnRecyclerItemByText(getTextFromResources(R.string.entry_settings_about))
        }
    }

    override fun closeAboutSettings() {
        Allure.step("Закрыть страницу About") {
            device.pressBack()
        }
    }

    override fun isAppVersionValid(): Boolean {
        return Allure.step("Проверка номера версии на валидность (версия состоит только из цифр разделенных точками)") {
            val stringVersion = device.find("version_info").text.split(" ")
            return@step PATTERN_VERSION.matches(stringVersion[1])
        }
    }

    override fun isCopyrightValid(): Boolean {
        return Allure.step("Проверка копирайтера на валидность (отобаржается текущий год)") {
            val list_about = device.find("container")
            return@step try {
                getElementInListByText(list_about.children, VALID_DATA_COPYRIGHT)
                true
            } catch (ex: AssertionError) {
                false
            }
        }
    }
}
