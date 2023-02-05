package com.yandex.mail.testopithecus.feature.impl

import android.os.Build.VERSION.SDK_INT
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.pages.SettingsPage
import com.yandex.mail.testopithecus.steps.clickOnRecyclerItemByText
import com.yandex.mail.testopithecus.steps.clickOnRecyclerItemByTextInUiObject2
import com.yandex.mail.testopithecus.steps.find
import com.yandex.mail.testopithecus.steps.findByText
import com.yandex.mail.testopithecus.steps.getTextFromResources
import com.yandex.mail.testopithecus.steps.scrollToObjectIfNeeded
import com.yandex.xplat.testopithecus.ActionOnSwipe
import com.yandex.xplat.testopithecus.AndroidGeneralSettings
import com.yandex.xplat.testopithecus.CancelSendingOption
import com.yandex.xplat.testopithecus.GeneralSettings
import com.yandex.xplat.testopithecus.Language
import io.qameta.allure.kotlin.Allure

class GeneralSettingsImpl(private val device: UiDevice) : GeneralSettings, AndroidGeneralSettings {
    private val settingsPage: SettingsPage = SettingsPage()

    override fun openGeneralSettings() {
        return
    }

    override fun clearCache() {
        Allure.step("Очистить кэш") {
            device.clickOnRecyclerItemByText(getTextFromResources(R.string.pref_caching_clear_title))
            device.findByText(getTextFromResources(R.string.alert_dialog_caching_clear_positive_button_text).toUpperCase()).click()
            device.find("recycler_view", timeout = 300000)
        }
    }

    override fun tapToClearCacheAndCancel() {
        Allure.step("Отмена очистки кэша") {
            device.clickOnRecyclerItemByText(getTextFromResources(R.string.pref_caching_clear_title))
            device.findByText(getTextFromResources(android.R.string.cancel).toUpperCase()).click()
            device.find("recycler_view", timeout = 300000)
        }
    }

    override fun setCancelSendingEmail(option: CancelSendingOption) {
        scrollToObjectIfNeeded(getTextFromResources(R.string.pref_sending_delay_title))
        device
            .find("recycler_view")
            .findObject(By.text(getTextFromResources(R.string.pref_sending_delay_title))).click()
        device.clickOnRecyclerItemByTextInUiObject2("popup_preference_layout_recycler_view", option.toString())
    }

    override fun getCancelSendingEmail(): CancelSendingOption {
        val elementName = getTextFromResources(R.string.pref_sending_delay_title)
        scrollToObjectIfNeeded(elementName)

        val timeout = device.findByText(elementName)
            .parent
            .find("pref_text_textview")?.text

        return SettingsPage.optionCancelSendingEmail.get(timeout.toString())!!
    }

    override fun closeGeneralSettings() {
        return
    }

    override fun setActionOnSwipe(action: ActionOnSwipe) {
        Allure.step("Установить действие по свайпу на ${action.value}") {
            device.clickOnRecyclerItemByText(getTextFromResources(R.string.pref_swipe_action_title))

            when (action) {
                ActionOnSwipe.archive -> device.findByText(getTextFromResources(R.string.entry_settings_swipe_archive)).click()
                ActionOnSwipe.delete -> device.findByText(getTextFromResources(R.string.entry_settings_swipe_delete)).click()
            }
        }
    }

    override fun getActionOnSwipe(): ActionOnSwipe {
        return Allure.step("Взять текущее действие по свайпу}") {
            val elementName = getTextFromResources(R.string.pref_swipe_action_title)
            scrollToObjectIfNeeded(elementName)

            val action = device.findByText(elementName)
                .parent
                .find("pref_text_textview")?.text

            return@step SettingsPage.actionOnSwipe[action.toString()]!!
        }
    }

    override fun isLoginUsingPasswordEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_pin_enter)
        scrollToObjectIfNeeded(elementName)

        return SettingsPage().getToggle(device, elementName).isChecked
    }

    override fun switchCompactMode() {
        val elementName = getTextFromResources(R.string.entry_settings_compact_mode)
        scrollToObjectIfNeeded(elementName)

        settingsPage.getToggle(device, elementName).click()
    }

    override fun isCompactModeEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_compact_mode)
        scrollToObjectIfNeeded(elementName)

        return settingsPage.getToggle(device, elementName).isChecked
    }

    override fun switchDarkTheme() {
        if (SDK_INT < 29) {
            val elementName = getTextFromResources(R.string.entry_settings_dark_theme)
            scrollToObjectIfNeeded(elementName)
            device.findByText(elementName).parent.parent.find("switchWidget")?.click()
        } else {
            val elementName = getTextFromResources(R.string.entry_settings_app_theme)
            val light = getTextFromResources(R.string.entry_settings_app_theme_light)
            val dark = getTextFromResources(R.string.entry_settings_app_theme_dark)

            scrollToObjectIfNeeded(elementName)
            val currentTheme = device.findByText(elementName).parent.find("pref_text_textview")?.text
            val needTheme = if (currentTheme == dark) light else dark
            device.findByText(elementName).parent.find("pref_text_textview")?.click()
            device.clickOnRecyclerItemByTextInUiObject2("popup_preference_layout_recycler_view", needTheme)
        }
    }

    override fun isDarkThemeEnabled(): Boolean {
        if (SDK_INT < 29) {
            val elementName = getTextFromResources(R.string.entry_settings_dark_theme)
            scrollToObjectIfNeeded(elementName)
            return device.findByText(elementName).parent.parent.find("switchWidget")!!.isChecked
        } else {
            val elementName = getTextFromResources(R.string.entry_settings_app_theme)
            scrollToObjectIfNeeded(elementName)
            val theme = device.findByText(elementName).parent.find("pref_text_textview")?.text
            return theme == getTextFromResources(R.string.entry_settings_app_theme_dark)
        }
    }

    override fun switchVoiceControl() {
        val elementName = getTextFromResources(R.string.entry_settings_voice_control)
        scrollToObjectIfNeeded(elementName)
        device.findByText(elementName).parent.parent.find("switchWidget")?.click()
    }

    override fun isVoiceControlEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_voice_control)
        scrollToObjectIfNeeded(elementName)

        return settingsPage.getToggle(device, elementName).isChecked
    }

    fun switchVoiceActivation() {
        val elementName = getTextFromResources(R.string.entry_settings_voice_activation)
        scrollToObjectIfNeeded(elementName)
        device.findByText(elementName).parent.parent.find("switchWidget")?.click()
    }

    fun isVoiceActivationEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_voice_activation)
        scrollToObjectIfNeeded(elementName)

        return settingsPage.getToggle(device, elementName).isChecked
    }

    override fun setVoiceControlLanguage(language: Language) {
        val elementName = getTextFromResources(R.string.entry_settings_voice_control_language)
        scrollToObjectIfNeeded(elementName)

        device
            .findByText(elementName)
            .parent
            .find("pref_text_textview")?.click()

        device.clickOnRecyclerItemByTextInUiObject2("popup_preference_layout_recycler_view", language.toString())
    }

    override fun getVoiceControlLanguage(): Language {
        val elementName = getTextFromResources(R.string.entry_settings_voice_control_language)
        scrollToObjectIfNeeded(elementName)

        val language = device
            .findByText(elementName)
            .parent
            .find("pref_text_textview")?.text

        return Language.valueOf(language.toString().decapitalize())
    }

    override fun switchSmartReplies() {
        val elementName = getTextFromResources(R.string.entry_settings_smart_replies)
        scrollToObjectIfNeeded(elementName)

        settingsPage.getToggle(device, elementName).click()
    }

    override fun isSmartRepliesEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_smart_replies)
        scrollToObjectIfNeeded(elementName)

        return settingsPage.getToggle(device, elementName).isChecked
    }

    override fun switchAds() {
        val elementName = getTextFromResources(R.string.entry_settings_show_adds)
        scrollToObjectIfNeeded(elementName)

        settingsPage.getToggle(device, elementName).click()
    }

    override fun isAdsEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_show_adds)
        scrollToObjectIfNeeded(elementName)

        return settingsPage.getToggle(device, elementName).isChecked
    }

    override fun isDoNotDisturbModeEnabled(): Boolean {
        val elementName = getTextFromResources(R.string.entry_settings_do_not_disturb_enabled)
        scrollToObjectIfNeeded(elementName)

        return settingsPage.getToggle(device, elementName).isChecked
    }

    override fun switchDoNotDisturbMode() {
        val elementName = getTextFromResources(R.string.entry_settings_do_not_disturb_enabled)
        scrollToObjectIfNeeded(elementName)

        settingsPage.getToggle(device, elementName).click()
    }
}
