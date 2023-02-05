package ru.yandex.yandexbus.perf.tests

import com.yandex.perftests.tests.TestBaseImpl
import java.util.concurrent.TimeUnit

open class BaseTest(testClass: Class<*>) : TestBaseImpl(testClass) {

    private val timeout = TimeUnit.SECONDS.toMillis(30)

    override fun prepare() {
        utils().removeAccounts()
        utils().startMainActivity()
        utils().device.waitForWindowUpdate(PACKAGE, timeout)
        utils().forceStop()
    }

    override fun start() {
        super.start()
        utils().findObject("fragment_container", timeout)
        utils().device.waitForIdle()
    }
}
