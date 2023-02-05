package ru.yandex.market.mocks.local.fapi

class TestClickDaemonTransport {

    private val calledUrls = mutableSetOf<String>()

    @Synchronized
    fun registerUrlCalls(urls: List<String>) {
        calledUrls.addAll(urls)
    }

    @Synchronized
    fun checkUrlCalled(url: String?): Boolean {
        val urlWithPrefix = CLICK_DAEMON_URL_PREFIX + url
        return calledUrls.contains(url) || calledUrls.contains(urlWithPrefix)
    }

    @Synchronized
    fun clearUrlCalls() {
        calledUrls.clear()
    }

    companion object {
        private const val CLICK_DAEMON_URL_PREFIX = "https://market-click2.yandex.ru/"
    }
}