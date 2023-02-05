package ru.yandex.yandexmaps.app.di.modules

import dagger.Module
import dagger.Provides
import ru.yandex.yandexmaps.multiplatform.debug.panel.DebugPanelService
import ru.yandex.yandexmaps.multiplatform.debug.panel.api.ExperimentManager
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class InitialExperiments

/**
 * Enhanced replacement for [ru.yandex.yandexmaps.app.di.modules.ExperimentManagerModule].
 */
@Module
object TestExperimentManagerModule {

    @Provides
    @Singleton
    fun experimentManager(@InitialExperiments initialExperiments: Map<String, String?>, debugPanelService: DebugPanelService): ExperimentManager {
        return debugPanelService.experimentManager.also { experimentManager ->
            initialExperiments.forEach {
                experimentManager[null, it.key] = it.value
            }
        }
    }
}
