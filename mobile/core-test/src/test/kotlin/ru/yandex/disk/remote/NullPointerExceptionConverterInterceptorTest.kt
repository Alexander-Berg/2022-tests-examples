package ru.yandex.disk.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import ru.yandex.disk.test.TestCase2
import java.io.IOException
import java.lang.NullPointerException

class NullPointerExceptionConverterInterceptorTest : TestCase2() {
    private lateinit var http: OkHttpClient

    private lateinit var fakeOkHttpInterceptor: FakeOkHttpInterceptor
    private lateinit var fakeOkHttpBuilder: OkHttpClient.Builder

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        fakeOkHttpInterceptor = FakeOkHttpInterceptor()
        fakeOkHttpBuilder = OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .addInterceptor(DisableHttpInterceptor(fakeOkHttpInterceptor))

        fakeOkHttpBuilder.addInterceptor(NullPointerExceptionConverterInterceptor())

        fakeOkHttpBuilder.addNetworkInterceptor({
            throw NullPointerException("test")
        })

        http = fakeOkHttpBuilder.build()
    }

    @Test(expected = IOException::class)
    fun `should convert NPE to IOException`() {
        http.newCall(Request.Builder().url("https://test").build()).execute()
    }

}