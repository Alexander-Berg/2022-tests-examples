package ru.yandex.yandexmaps.multiplatform.uitesting.api

import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.TestRunnerReporter

public interface Test {
    public val retryCount: Int get() = 3

    @Throws(Throwable::class)
    public fun run(
        application: Application,
        tryIndex: Int,
        assertionProvider: AssertionProvider,
        reporter: TestRunnerReporter,
        screenshotCapturer: ScreenshotCapturer,
    )

    public fun getDescription(): String
    public fun getRequiredExperiments(): List<ExperimentInfo<out Any?>>
}

public val Test.maxRunCount: Int get() = retryCount + 1
