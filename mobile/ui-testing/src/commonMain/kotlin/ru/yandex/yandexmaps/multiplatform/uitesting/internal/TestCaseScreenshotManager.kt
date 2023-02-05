package ru.yandex.yandexmaps.multiplatform.uitesting.internal

import ru.yandex.yandexmaps.multiplatform.core.utils.extensions.collections.addNonNull
import ru.yandex.yandexmaps.multiplatform.uitesting.api.MediaCapture
import ru.yandex.yandexmaps.multiplatform.uitesting.api.ScreenshotCapturer
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.TestRunnerReporter

internal class TestCaseScreenshotManager(
    private val screenshotCapturer: ScreenshotCapturer,
    private val reporter: TestRunnerReporter,
) {

    private val pendingScreenshots = mutableListOf<NamedScreenshot>()

    fun captureScreenshot(name: String) {
        pendingScreenshots.addNonNull(screenshotCapturer.capture()?.let { NamedScreenshot(name = name, data = it) })
    }

    fun runAttachingScreenshotsOnFailure(failureName: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            captureScreenshot(failureName)
            attachScreenshotsLeadingToFailure()
            throw t
        }
    }

    private fun attachScreenshotsLeadingToFailure() {
        pendingScreenshots.forEach(::attach)
    }

    private fun attach(screenshot: NamedScreenshot) {
        reporter.attachment(
            name = screenshot.name,
            data = screenshot.data.data,
            mimeType = screenshot.data.mimeType,
            fileExtension = screenshot.data.fileExtension
        )
    }

    private class NamedScreenshot(
        val name: String,
        val data: MediaCapture,
    )
}
