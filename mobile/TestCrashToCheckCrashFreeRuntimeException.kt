package ru.yandex.market.exception

class TestCrashToCheckCrashFreeRuntimeException() :
    RuntimeException() {

    companion object {
        private const val serialVersionUID = 1L
    }
}