package ru.auto.test.common.model

import ru.auto.settings.provider.SettingEntity
import ru.auto.settings.provider.SettingGroupId
import ru.auto.settings.provider.SettingId
import ru.auto.settings.provider.SettingValue

data class SettingsStore(
    val settings: Map<SettingGroupId, Map<SettingId, SettingEntity>>,
    val settingsValues: Map<SettingId, SettingValue>,
    val isFirstLaunch: Boolean,
)

fun SettingsStore.updateSettingsValue(setting: SettingEntity, value: SettingValue?): SettingsStore {
    val settingValues = if (value?.takeIf { it != setting.defaultValue } != null) {
        settingsValues.plus(setting.id to value)
    } else {
        settingsValues.minus(setting.id)
    }
    val filteredSettingValues = settingValues.filterKeys { settings[setting.group]?.get(setting.id) != null }
    return copy(settingsValues = filteredSettingValues)
}
