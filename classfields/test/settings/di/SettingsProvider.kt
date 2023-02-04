package ru.auto.test.settings.di

import ru.auto.core_ui.tea.TeaFeatureSimpleSet
import ru.auto.core_ui.tea.wrapWithEffectReplay
import ru.auto.test.common.data.SettingsRepository
import ru.auto.test.settings.presentation.Settings
import ru.auto.test.settings.presentation.SettingsFeature
import ru.auto.test.settings.presentation.wrapWithSettingsEffectHandler

fun provideSettingsFeature(deps: SettingsFeatureDependencies): SettingsFeature =
    TeaFeatureSimpleSet(
        initialState = Settings.State(),
        reducer = Settings::reduce
    ).wrapWithSettingsEffectHandler(
        deps.settingsRepository
    ).wrapWithEffectReplay()

interface SettingsFeatureDependencies {
    val settingsRepository: SettingsRepository
}
