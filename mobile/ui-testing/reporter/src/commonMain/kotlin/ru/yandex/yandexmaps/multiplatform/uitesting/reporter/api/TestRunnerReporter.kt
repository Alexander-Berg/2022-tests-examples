package ru.yandex.yandexmaps.multiplatform.uitesting.reporter.api

public interface TestRunnerReporter {
    public fun suite(name: String, block: () -> Unit): Unit = block()
    public fun case(name: String, tryIndex: Int, retryCount: Int, link: ReporterLink?, block: () -> Unit): Unit = block()
    public fun step(name: String, block: () -> Unit): Unit = block()
    public fun attachment(name: String, data: ByteArray, mimeType: String, fileExtension: String): Unit = Unit
}

public class ReporterLink(public val name: String, public val url: String)

public object PhonyTestRunnerReporter : TestRunnerReporter
