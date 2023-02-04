package ru.auto.test.common.di

import android.content.Context
import com.google.gson.Gson
import ru.auto.settings.provider.ExperimentsContentResolver
import ru.auto.settings.provider.SettingsContentResolver
import ru.auto.test.common.data.ExperimentsRepository
import ru.auto.test.common.data.PreferencesStorage
import ru.auto.test.common.data.SettingsRepository
import ru.auto.test.common.data.SettingsValuesContentProvider
import ru.auto.test.common.utils.isBuildTypeDebug
import ru.auto.test.experiments.di.ExperimentsFeatureDependencies
import ru.auto.test.settings.di.SettingsFeatureDependencies
import ru.auto.test.testid.di.AddTestIdFeatureDependencies

class MainModule(
    context: Context,
) : SettingsFeatureDependencies,
    ExperimentsFeatureDependencies,
    AddTestIdFeatureDependencies,
    SettingsValuesContentProvider.Dependencies {

    override val preferencesStorage by lazy { PreferencesStorage(gson = Gson(), context = context) }
    private val settingsContentResolver by lazy { SettingsContentResolver(isDebug = isBuildTypeDebug, context = context) }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            preferencesStorage = preferencesStorage,
            settingsContentResolver = settingsContentResolver
        )
    }

    override val experimentsRepository: ExperimentsRepository by lazy {
        ExperimentsRepository(
            preferencesStorage = preferencesStorage,
            experimentsContentResolver = ExperimentsContentResolver(context = context),
            settingsContentResolver = settingsContentResolver
        )
    }

}
