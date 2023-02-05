package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public interface SettingsPage {
    public fun openLogin()
    public fun openSettings()
    public fun closeSettings()
    public fun getLoginName(): String?
}
