package ru.yandex.yandexmaps.multiplatform.uitesting.internal

import ru.yandex.yandexmaps.multiplatform.debug.panel.experiments.KnownExperiments
import ru.yandex.yandexmaps.multiplatform.uitesting.api.Application
import ru.yandex.yandexmaps.multiplatform.uitesting.api.AssertionProvider
import ru.yandex.yandexmaps.multiplatform.uitesting.api.ExperimentInfo
import ru.yandex.yandexmaps.multiplatform.uitesting.api.ScreenshotCapturer
import ru.yandex.yandexmaps.multiplatform.uitesting.api.Test
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.ReporterLink
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.TestRunnerReporter

internal class TestCaseLink(val name: String, val url: String)

internal enum class Status {
    STABLE,
    UNSTABLE,
    BROKEN
}

internal enum class Scope {
    PR,
    NIGHT,
    RELEASE
}

internal abstract class TestCaseBasedTest internal constructor(
    private val name: String,
    private val link: TestCaseLink? = null,
    private val experiments: List<ExperimentInfo<out Any?>> = emptyList(),
) : Test {

    override fun run(
        application: Application,
        tryIndex: Int,
        assertionProvider: AssertionProvider,
        reporter: TestRunnerReporter,
        screenshotCapturer: ScreenshotCapturer,
    ) {
        val reporterLink = link?.let { ReporterLink(it.name, it.url) }
        reporter.case(name, tryIndex, retryCount, reporterLink) {
            val screenshotManager = TestCaseScreenshotManager(screenshotCapturer, reporter)
            TestCaseDsl(application, reporter, assertionProvider, screenshotManager).run()
        }
    }

    public abstract fun TestCaseDsl.run()

    public abstract fun status(): Status

    public abstract fun scopes(): List<Scope>

    override fun getDescription(): String {
        return name
    }

    override fun getRequiredExperiments(): List<ExperimentInfo<out Any?>> {
        return experiments.filter { KnownExperiments.keys.contains(it.key) }
    }
}
