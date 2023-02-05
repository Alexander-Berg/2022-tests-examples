package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.closeDrawer
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.has
import com.yandex.xplat.testopithecus.YandexLogin
import com.yandex.xplat.testopithecus.common.UserAccount
import io.qameta.allure.kotlin.Allure

class YandexLoginImpl(private val device: UiDevice) : YandexLogin {
    override fun loginWithYandexAccount(account: UserAccount) {
        Allure.step("Логинимся в аккаунте ${account.login}") {
            onView(withId(R.id.list_yandex)).perform(ViewActions.click())
            device.find("edit_login").text = account.login
            device.find("button_next").click()
            device.find("edit_password", timeout = 5000).text = account.password
            device.find("button_next").click()

            if (device.has("account_switcher_gallery")) {
                closeDrawer()
            } else {
                device.find("close_button", timeout = 60000).click()
            }
            device.has("ads_content_container", timeout = 10000)
        }
    }
}
