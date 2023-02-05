package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.OPTIMAL_SWIPE_SPEED_IN_STEPS
import com.yandex.mail.testopithecus.steps.count
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.mail.testopithecus.steps.formatEmail
import com.yandex.mail.testopithecus.steps.has
import com.yandex.mail.testopithecus.steps.scrollDrawerToTop
import com.yandex.mail.testopithecus.whileMax
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.testopithecus.Login
import com.yandex.xplat.testopithecus.MultiAccount
import io.qameta.allure.kotlin.Allure
import org.hamcrest.core.AllOf.allOf

class MultiAccountImpl(private val device: UiDevice) : MultiAccount {

    override fun switchToAccount(login: Login) {
        Allure.step("Переключаемся в аккаунт c логином $login") {
            toFirstAccount()

            var i = 0

            while (getCurrentAccount() != login && i < getNumberOfAccounts()) {
                device.findMany("account_switcher_item_icon").last().click()
                i++
            }
        }
    }

    override fun getCurrentAccount(): Login {
        return Allure.step("Получаем логин активного аккаунта") {
            device.scrollDrawerToTop()
            return@step formatEmail(device.find("account_switcher_subtitle").text)
        }
    }

    override fun getNumberOfAccounts(): Int {
        return Allure.step("Получаем число залогиненых аккаунтов") {
            return@step device.findMany("account_switcher_item_icon").count()
        }
    }

    override fun addNewAccount() {
        Allure.step("Переходим на страницу добавления нового аккаунта") {
            onView(allOf(withId(R.id.account_switcher_item_icon_add))).perform(scrollTo(), click())
        }
    }

    override fun getLoggedInAccountsList(): YSArray<Login> {
        return Allure.step("Получаем список залогиненых аккаунтов") {
            toFirstAccount()

            val account = mutableListOf<Login>()

            account.add(getCurrentAccount())

            whileMax({ !device.has("account_switcher_item_icon_add") }) {
                device.findMany("account_switcher_item_icon").last().click()
                account.add(getCurrentAccount())
            }

            return@step account
        }
    }

    override fun logoutFromAccount(login: Login) {
        Allure.step("Выходим из аккаунта $login") {
            switchToAccount(login)
            device.scrollDrawerToTop()
            scrollToItem(device.findMany("Log out").first())
            Thread.sleep(1000)
            device.findMany("Log out").first().click()
        }
    }

    private fun toFirstAccount() {
        Allure.step("Пролистываем аккаунты до самого первого") {
            var tries = 15

            while (
                (
                    (device.count("account_switcher_item_icon") == 2 && device.has("account_switcher_item_icon_add")) ||
                        device.count("account_switcher_item_icon") == 3
                    ) && tries > 0
            ) {
                device.find("account_switcher_item_icon", order = 0).click()
                tries -= 1
            }
        }
    }

    private fun scrollToItem(item: UiObject2) {
        val positionY = item.visibleCenter.y
        device.swipe(10, positionY, 10, 10, OPTIMAL_SWIPE_SPEED_IN_STEPS)
    }
}
