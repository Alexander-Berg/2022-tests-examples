package ru.auto.test.common.di

import android.content.Context
import ru.auto.test.common.data.SettingsValuesContentProvider
import ru.auto.test.experiments.di.provideExperimentsFeature
import ru.auto.test.settings.di.provideSettingsFeature
import ru.auto.test.testid.di.provideAddTestIdFeature

class ComponentManager(
    context: Context
) {
    private val mainModule = MainModule(context)

    fun settingsFeature() = provideSettingsFeature(mainModule)
    fun experimentsFeature() = provideExperimentsFeature(mainModule)
    fun addTestIdFeature() = provideAddTestIdFeature(mainModule)

    fun settingsValuesContentProviderDependencies(): SettingsValuesContentProvider.Dependencies = mainModule

}
