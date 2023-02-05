package ru.yandex.yandexmaps.multiplatform.uitesting.internal

import ru.yandex.yandexmaps.multiplatform.debug.panel.experiments.KnownExperimentKey
import ru.yandex.yandexmaps.multiplatform.uitesting.api.ExperimentInfo

internal data class ExperimentsRepository(public val experiments: List<ExperimentInfo<out Any?>>) {

    fun <T> getValue(key: KnownExperimentKey<T>): T? {
        return experiments.mapNotNull {
            @Suppress("UNCHECKED_CAST")
            it as? ExperimentInfo<T>
        }.firstOrNull {
            it.key == key
        }?.value
    }
}
