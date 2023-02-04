package ru.auto.ara.core

import okhttp3.Interceptor
import okhttp3.Response
import ru.auto.ara.network.interceptor.UidInterceptor

class TestUidInterceptor : UidInterceptor() {

    /**
     * This is crutch for ui tests that want to use offline mode.
     * Rx.await(), which is used in UidInterceptor forcefully wraps any exception in RuntimeException.
     * In almost all cases, we do not want to catch and handle RuntimeException, rather handle real one. In offline tests
     * this problem is prominent, so i decided to mitigate this unnessecary wrapping at all.
     */
    @Suppress("TooGenericExceptionCaught")
    override fun intercept(chain: Interceptor.Chain): Response {
        return try {
            super.intercept(chain)
        } catch (ex: RuntimeException) {
            ex.cause?.let { throw it }
            chain.proceed(chain.request())
        }
    }
}
