package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.steps.closeDrawer
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.has
import com.yandex.xplat.testopithecus.CustomMailServiceLogin
import com.yandex.xplat.testopithecus.common.UserAccount
import io.qameta.allure.kotlin.Allure

class CustomMailServiceLoginImpl(private val device: UiDevice) : CustomMailServiceLogin {
    override fun loginWithCustomMailServiceAccount(account: UserAccount) {
        Allure.step("Логинимся IMAP аккаунтом ${account.login}") {
            device.find("list_other").click()
            device.find("edit_login").text = account.login
            device.find("button_sign_in").click()
            device.find("edit_password", timeout = 5000).text = account.password
            device.findByText("Sign in").click()

            if (device.has("account_switcher_gallery")) {
                closeDrawer()
            } else {
                device.find("close_button", timeout = 60000).click()
            }
            device.has("ads_content_container", timeout = 10000)
        }
    }
}
