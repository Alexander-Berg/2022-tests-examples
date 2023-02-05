package ru.yandex.yandexmaps.multiplatform.uitesting.internal

import ru.yandex.yandexmaps.multiplatform.uitesting.api.VideoCapturer
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.TestRunnerReporter

internal class TestCaseVideoManager(
    private val videoCapturer: VideoCapturer,
    private val reporter: TestRunnerReporter,
) {
    fun runCapturingVideo(block: () -> Unit) {
        val videoCaptureSession = videoCapturer.startCapture()

        try {
            block()
            videoCaptureSession.stop()
        } catch (t: Throwable) {
            videoCaptureSession.stop()?.run {
                reporter.attachment(
                    name = "Test run",
                    data = data,
                    mimeType = mimeType,
                    fileExtension = fileExtension
                )
            }
            throw t
        }
    }
}
