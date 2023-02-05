package ru.yandex.yandexmaps.multiplatform.uitesting.internal

import ru.yandex.yandexmaps.multiplatform.uitesting.api.Application
import ru.yandex.yandexmaps.multiplatform.uitesting.api.ApplicationInteractor
import ru.yandex.yandexmaps.multiplatform.uitesting.api.AssertionProvider
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.TestRunnerReporter

internal class TestCaseDsl(
    application: Application,
    private val reporter: TestRunnerReporter,
    private val assertionProvider: AssertionProvider,
    private val screenshotManager: TestCaseScreenshotManager,
) {

    val pages = PageObjectsAdapter(pageObjects = application)
    val interactor: ApplicationInteractor = application

    fun perform(name: String, action: () -> Unit) {
        val trimmedName = name.trimIndent()
        reporter.step(trimmedName) {
            screenshotManager.runAttachingScreenshotsOnFailure(trimmedName) {
                action()
                screenshotManager.captureScreenshot(trimmedName)
            }
        }
    }

    fun assert(name: String, assert: AssertionProvider.() -> Unit) {
        val trimmedName = name.trimIndent()
        reporter.step("Assertion: $trimmedName") {
            screenshotManager.runAttachingScreenshotsOnFailure(trimmedName) {
                assertionProvider.assert()
            }
        }
    }
}
