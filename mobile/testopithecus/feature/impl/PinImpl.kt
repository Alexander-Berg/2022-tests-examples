package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.pages.SettingsPage
import com.yandex.mail.testopithecus.steps.acceptDialog
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.mail.testopithecus.steps.scrollToObjectIfNeeded
import com.yandex.xplat.testopithecus.Pin
import io.qameta.allure.kotlin.Allure

class PinImpl(private val device: UiDevice) : Pin {
    private val settingsPage: SettingsPage = SettingsPage()
    private val TIME_SLEEP = 70000
    private val INVALID_PIN = "0000"

    val numbers = mapOf(
        '0' to "btn_zero",
        '1' to "btn_one",
        '2' to "btn_two",
        '3' to "btn_three",
        '4' to "btn_four",
        '5' to "btn_five",
        '6' to "btn_six",
        '7' to "btn_seven",
        '8' to "btn_eight",
        '9' to "btn_nine",
    )

    override fun turnOnLoginUsingPassword(password: String) {
        Allure.step("Включение настройки использования пин-кода") {
            val elementName = getTextFromResources(R.string.entry_settings_pin_enter)
            scrollToObjectIfNeeded(elementName)

            settingsPage.getToggle(device, elementName).click()

            enterPassword(password)
            enterPassword(password)
        }
    }

    override fun turnOffLoginUsingPassword() {
        Allure.step("Выключение  настройки использования пин-кода") {
            val elementName = getTextFromResources(R.string.entry_settings_pin_enter)
            scrollToObjectIfNeeded(elementName)

            settingsPage.getToggle(device, elementName).click()
        }
    }

    override fun changePassword(newPassword: String) {
        Allure.step("Изменение пин-кода") {
            turnOffLoginUsingPassword()
            turnOnLoginUsingPassword(newPassword)
        }
    }

    override fun resetPassword() {
        Allure.step("Сброс пин-кода") {
            enterPassword(INVALID_PIN)
            device.find("pin_info").click()
            device.acceptDialog()
        }
    }

    override fun enterPassword(password: String) {
        Allure.step("Ввод пин-кода") {
            for (digit in password) {
                device.find(numbers[digit].toString()).click()
            }
        }
    }

    override fun isLoginUsingPasswordEnabled(): Boolean {
        return Allure.step("Проверка активации настройки пин-кода") {
            val elementName = getTextFromResources(R.string.entry_settings_pin_enter)
            scrollToObjectIfNeeded(elementName)

            return@step settingsPage.getToggle(device, elementName).isChecked
        }
    }

    override fun waitForPinToTrigger() {
        Allure.step("Ждем, пока пройдет тайминг и активируется окно запроса пин-кода для входа в приложение") {
            Thread.sleep(TIME_SLEEP.toLong())
        }
    }
}
