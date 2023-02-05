package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.By.res
import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.yandex.mail.R
import com.yandex.mail.testopithecus.pages.SettingsPage
import com.yandex.mail.testopithecus.steps.clickOnRecyclerItemByText
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.findManyAndroid
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.mail.testopithecus.steps.isMatchesAssertion
import com.yandex.mail.testopithecus.steps.scrollDrawerToTop
import com.yandex.mail.testopithecus.steps.scrollToObjectIfNeeded
import com.yandex.xplat.common.YSMap
import com.yandex.xplat.mapi.SignaturePlace
import com.yandex.xplat.testopithecus.AccountSettings
import com.yandex.xplat.testopithecus.AndroidAccountSettings
import com.yandex.xplat.testopithecus.FolderName
import com.yandex.xplat.testopithecus.NotificationOption
import io.qameta.allure.kotlin.Allure
import org.hamcrest.Matchers.allOf

class AccountSettingsImpl(private val device: UiDevice) : AccountSettings, AndroidAccountSettings {
    private val settingsPage: SettingsPage = SettingsPage()

    val placeSignature = mapOf(
        getTextFromResources(R.string.account_settings_place_for_signature_none) to SignaturePlace.none,
        getTextFromResources(R.string.account_settings_place_for_signature_below) to SignaturePlace.atEnd,
        getTextFromResources(R.string.account_settings_place_for_signature_above) to SignaturePlace.afterReply
    )

    override fun openAccountSettings(accountIndex: Int) {
//        device.find("recycler_view").children[accountIndex + 1].click()
        Allure.step("Открыть настройки аккаунта #$accountIndex") {
            // Number Account == id + 1 (#0 - the title 'ACCOUNTS')
            val list_title = device.findManyAndroid("title")
            list_title[accountIndex + 1].click()
        }
    }

    override fun closeAccountSettings() {
        Allure.step("Закрыть настройки аккаунта") {
            device.pressBack()
        }
    }

    override fun isGroupBySubjectEnabled(): Boolean {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.pref_thread_mode_title))
        )

        return settingsPage.getToggle(device, getTextFromResources(R.string.pref_thread_mode_title)).isChecked
    }

    override fun switchGroupBySubject() {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.pref_thread_mode_title))
        )

        settingsPage.getToggle(device, getTextFromResources(R.string.pref_thread_mode_title)).click()
    }

    override fun switchSortingEmailsByCategory() {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.entry_settings_tabs))
        )

        settingsPage.getToggle(device, getTextFromResources(R.string.entry_settings_tabs)).click()
    }

    override fun isSortingEmailsByCategoryEnabled(): Boolean {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.entry_settings_tabs))
        )
        return settingsPage.getToggle(device, getTextFromResources(R.string.entry_settings_tabs)).isChecked
    }

    override fun getSignature(): String {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.pref_signature))
        )

        return device
            .find("recycler_view")
            .findObject(text(getTextFromResources(R.string.pref_signature)))
            .parent
            .findObject(res("android:id/summary")).text
    }

    override fun setPlaceForSignature(place: SignaturePlace) {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.pref_signature_place))
        )
        device
            .find("recycler_view")
            .findObject(text(getTextFromResources(R.string.pref_signature_place))).click()

        for ((namePlace, idPlace) in placeSignature) {
            if (idPlace == place) {
                onView(allOf(ViewMatchers.withText(namePlace))).perform(click())
                return
            }
        }
    }

    override fun getPlaceForSignature(): SignaturePlace {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.pref_signature_place))
        )
        return placeSignature[
            device
                .find("recycler_view")
                .findObject(text(getTextFromResources(R.string.pref_signature_place)))
                .parent
                .findObject(res("android:id/summary")).text
        ]!!
    }

    override fun isSyncCalendarEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.pref_calendar_sync_enable)
        scrollToObjectIfNeeded(elementName)

        return settingsPage.getToggle(device, elementName).isChecked
    }

    override fun switchSyncCalendar() {
        val elementName = getTextFromResources(R.string.pref_calendar_sync_enable)
        scrollToObjectIfNeeded(elementName)

        settingsPage.getToggle(device, elementName).click()
    }

    override fun isAccountUsingEnabled(): Boolean {
        return try {
            settingsPage.getToggle(device, getTextFromResources(R.string.pref_mail_usage)).isChecked
        } catch (ex: AssertionError) {
            true
        }
    }

    override fun getFolderToNotificationOption(): YSMap<FolderName, NotificationOption> {
        val notificationOptionByFolder: YSMap <FolderName, NotificationOption> = mutableMapOf()

        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.pref_push_notifications_enabled))
        )

        while (! onView(ViewMatchers.withText("Archive")).isMatchesAssertion(ViewAssertions.matches(ViewMatchers.isDisplayed()))) {
            for (item in device.find("recycler_view").findObjects(text(getTextFromResources(R.string.sync_settings_entry_sync_and_notify)))) {
                notificationOptionByFolder[item.parent.findObject(res("android:id/title"))!!.text] = NotificationOption.syncAndNotifyMe
            }

            UiScrollable(
                UiSelector().scrollable(true)
            ).scrollForward()
        }

        for (item in device.find("recycler_view").findObjects(text(getTextFromResources(R.string.sync_settings_entry_sync_and_notify)))) {
            notificationOptionByFolder[item.parent.findObject(res("android:id/title"))!!.text] = NotificationOption.syncAndNotifyMe
        }

        return notificationOptionByFolder
    }

    override fun getNotificationOptionForFolder(folder: FolderName): NotificationOption {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(folder)
        )
        return NotificationOption.valueOf(device.findByText(folder).parent.findObject(res("android:id/summary"))!!.text)
    }

    override fun openMailingListsManager() {
        device
            .find("recycler_view")
            .findObject(text(getTextFromResources(R.string.account_settings_unsubscribe)))
            .click()
    }

    override fun openFilters() {
        // TODO: Not yet implemented
    }

    override fun changeSignature(newSignature: String) {
        device
            .find("recycler_view")
            .findObject(text(getTextFromResources(R.string.pref_signature)))
            .parent
            .findObject(res("android:id/summary")).click()

        device.find("settings_fragment_signature_text").text = newSignature
        device.find("settings_fragment_signature_ok_button").click()
    }

    override fun switchTheme() {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.entry_settings_mail_design_enabled))
        )

        settingsPage.getToggle(device, getTextFromResources(R.string.entry_settings_mail_design_enabled)).click()
    }

    override fun isThemeEnabled(): Boolean {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.entry_settings_mail_design_enabled))
        )

        return settingsPage.getToggle(device, getTextFromResources(R.string.entry_settings_mail_design_enabled)).isChecked
    }

    override fun setNotificationOptionForFolder(folder: FolderName, option: NotificationOption) {
        device.scrollDrawerToTop()
        device.clickOnRecyclerItemByText(folder)
        onView(allOf(ViewMatchers.withText(option.toString()))).perform(click())
    }

    override fun switchUseAccountSetting() {
        settingsPage.getToggle(device, getTextFromResources(R.string.pref_mail_usage)).click()
    }

    override fun openFolderManager() {
        device
            .find("recycler_view")
            .findObject(text(getTextFromResources(R.string.pref_manage_folders_title)))
            .click()
    }

    override fun openLabelManager() {
        device
            .find("recycler_view")
            .findObject(text(getTextFromResources(R.string.pref_manage_labels_title)))
            .click()
    }

    override fun openPassport() {
        UiScrollable(
            UiSelector().scrollable(true)
        ).scrollIntoView(
            UiSelector().text(getTextFromResources(R.string.account_settings_manage_account))
        )
        device
            .find("recycler_view")
            .findObject(text(getTextFromResources(R.string.account_settings_manage_account)))
            .click()
    }
}
