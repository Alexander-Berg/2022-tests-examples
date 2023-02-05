package ru.yandex.yandexbus.inhouse

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class TestsRunner(klass: Class<*>) : RobolectricTestRunner(klass) {

    override fun buildGlobalConfig(): Config {
        return Config.Builder()
                .setSdk(21)
                .setApplication(TestBusApplication::class.java)
                .build()
    }
}