package com.yandex.mail.testopithecus.feature.impl

import androidx.test.uiautomator.UiDevice
import com.yandex.mail.testopithecus.pages.FolderListPage
import com.yandex.mail.testopithecus.steps.SHORT_TIMEOUT
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findMany
import com.yandex.xplat.testopithecus.FolderName
import com.yandex.xplat.testopithecus.Tabs
import io.qameta.allure.kotlin.Allure

class TabsImpl(private val device: UiDevice) : Tabs {
    val tabsAplicationName = mutableMapOf(
        "Mailing lists" to FolderListPage.SUBSCRIPTIONS,
        "Social networks" to FolderListPage.SOCIALMEDIA
    )

    override fun SwitchOffTabs() {
        Allure.step("Включить табы у аккаунта") {
            if (AccountSettingsImpl(device).isGroupBySubjectEnabled()) {
                AccountSettingsImpl(device).switchSortingEmailsByCategory()
            }
        }
    }

    override fun SwitchOnTabs() {
        Allure.step("Выключить табы у аккаунта") {
            if (!AccountSettingsImpl(device).isGroupBySubjectEnabled()) {
                AccountSettingsImpl(device).switchSortingEmailsByCategory()
            }
        }
    }

    override fun isEnableTabs(): Boolean {
        return Allure.step("Проверка на активность настройки табов") {
            return@step AccountSettingsImpl(device).isGroupBySubjectEnabled()
        }
    }

    override fun isDisplayNotificationTabs(tabsName: FolderName): Boolean {
        return Allure.step("Проверяем отображение плашки о новых входящих письмах в таб \"$tabsAplicationName[tabsName]\"") {
            val listTabsContainer = device.findMany("email_list_tab_header", SHORT_TIMEOUT)
            for (item in listTabsContainer) {
                if (item.text == tabsAplicationName[tabsName]) {
                    return@step true
                }
            }
            return@step false
        }
    }

    override fun isUnreadNotificationTabs(tabsName: FolderName): Boolean {
        return Allure.step("Проверяем, является ли плашка таба  \"$tabsAplicationName[tabsName]\" непрочитанной") {
            val listTabsContainer = device.findMany("email_list_tab_header", SHORT_TIMEOUT)
            for (item in listTabsContainer) {
                if (item.text == tabsAplicationName[tabsName]) {
                    return@step (item.parent.find("email_list_tab_unread_status") != null)
                }
            }
            return@step false
        }
    }

    override fun getPositionTabsNotification(tabsName: FolderName): Int {
        return Allure.step("Берем номер позиции плашки \"$tabsAplicationName[tabsName]\" в списке писем Inbox") {
            val emailList = device.find("email_list_recycler").children
            if (emailList[0].className.endsWith("FrameLayout")) { // Delete object with ads
                emailList.remove(emailList[0])
            }
            var position = 0
            for (item in emailList) {
                if (item.resourceName != null) {
                    if (item.resourceName.endsWith("email_list_time_bucket_header")) {
                        continue
                    }
                    if (item.resourceName.endsWith("email_list_tab_container") &&
                        item.find("email_list_tab_header")!!.text == tabsAplicationName[tabsName]
                    ) {
                        return@step position
                    }
                }
                position++
            }
            return@step position
        }
    }

    override fun goToTabByNotification(tabsName: FolderName) {
        return Allure.step("Перейти в таб \"$tabsAplicationName[tabsName]\" через плашку") {
            val listTabsContainer = device.findMany("email_list_tab_header", SHORT_TIMEOUT)
            for (item in listTabsContainer) {
                if (item.text == tabsAplicationName[tabsName]) {
                    item.click()
                }
            }
        }
    }
}
