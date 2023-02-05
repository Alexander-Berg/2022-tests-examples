package ru.yandex.yandexnavi.analytics

class TestReporter(
    private val reportVerifier: (event: String, params: Map<String, Any?>?) -> Unit,
    private val errorVerifier: (throwable: Throwable, message: String?) -> Unit
) : AnalyticsReporter {
    var eventCounter: Int = 0
    var errorCounter: Int = 0

    override fun report(event: String, params: Map<String, Any?>?) {
        reportVerifier(event, params)
        eventCounter++
        println("$event: $params")
    }

    override fun error(throwable: Throwable, message: String?) {
        errorVerifier(throwable, message)
        errorCounter++
        println("$throwable: $message")
    }
}
