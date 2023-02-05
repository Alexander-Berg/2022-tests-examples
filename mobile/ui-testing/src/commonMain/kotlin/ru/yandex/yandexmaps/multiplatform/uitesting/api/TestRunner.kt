package ru.yandex.yandexmaps.multiplatform.uitesting.api

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseVideoManager
import ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api.TestRunnerReporter

public class TestRunner(
    private val test: Test,
    private val application: Application,
    private val assertionProvider: AssertionProvider,
    private val reporter: TestRunnerReporter,
    private val screenshotCapturer: ScreenshotCapturer,
    videoCapturer: VideoCapturer,
    private val tryIndex: Int,
) {
    private val videoManager = TestCaseVideoManager(videoCapturer, reporter)

    @Throws(Throwable::class)
    public fun runTest() {
        videoManager.runCapturingVideo {
            test.run(application, tryIndex, assertionProvider, reporter, screenshotCapturer)
        }
    }
}
