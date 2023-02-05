package ru.yandex.disk.remote

import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import okhttp3.Interceptor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import java.io.IOException

@RunWith(BlockJUnit4ClassRunner::class)
class MissingNetworkPermissionExceptionConverterInterceptorTest {

    private val interceptor = MissingNetworkPermissionExceptionConverterInterceptor()
    private val chain = mock<Interceptor.Chain>()


    @Test(expected = IOException::class)
    fun `should convert missing permission exception`() {
        whenever(chain.proceed(anyOrNull()))
                .thenThrow(SecurityException("Permission denied (missing INTERNET permission?)"))

        interceptor.intercept(chain)
    }

    @Test(expected = SecurityException::class)
    fun `should not convert security exception without message`() {
        whenever(chain.proceed(anyOrNull())).thenThrow(SecurityException())

        interceptor.intercept(chain)
    }

    @Test(expected = RuntimeException::class)
    fun `should not convert not security exception`() {
        whenever(chain.proceed(anyOrNull())).thenThrow(RuntimeException())

        interceptor.intercept(chain)
    }
}
