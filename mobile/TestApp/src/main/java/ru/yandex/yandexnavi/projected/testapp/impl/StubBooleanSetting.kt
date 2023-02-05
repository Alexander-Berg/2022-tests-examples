package ru.yandex.yandexnavi.projected.testapp.impl

import com.yandex.navikit.providers.settings.BooleanSetting
import com.yandex.navikit.providers.settings.common.SettingListener

class StubBooleanSetting : BooleanSetting {
    private var value = false
    private val listeners = mutableListOf<SettingListener>()

    override fun subscribe(settingListener: SettingListener) {
        listeners.add(settingListener)
    }

    override fun unsubscribe(settingListener: SettingListener) {
        listeners.remove(settingListener)
    }

    override fun value(): Boolean {
        return value
    }

    override fun setValue(value: Boolean) {
        this.value = value
        listeners.forEach { it.onSettingChanged() }
    }
}
