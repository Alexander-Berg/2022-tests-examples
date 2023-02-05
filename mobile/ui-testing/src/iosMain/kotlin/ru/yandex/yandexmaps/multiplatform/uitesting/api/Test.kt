package ru.yandex.yandexmaps.multiplatform.uitesting.api

public data class PlatformExperimentInfo(public val key: String, public val value: String?)

public class PlatformTestExperiments(test: Test) {
    public val experiments: List<PlatformExperimentInfo> = test.getRequiredExperiments().map { PlatformExperimentInfo(it.key.name, it.value?.toString()) }
}
