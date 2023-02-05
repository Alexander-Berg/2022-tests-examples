package ru.yandex.yandexmaps.multiplatform.uitesting.api

import ru.yandex.yandexmaps.multiplatform.debug.panel.experiments.KnownExperimentKey

public data class ExperimentInfo<T>(public val key: KnownExperimentKey<T>, public val value: T?)
