package ru.yandex.yandexmaps.common.network

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Rule
import org.junit.Test
import ru.yandex.yandexmaps.common.network.okhttp.MonitoringInterceptor
import ru.yandex.yandexmaps.multiplatform.core.monitoring.MonitoringTracker
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class OkHttpTests {

    @get:Rule
    val server = MockWebServer()

    @Test
    fun `Sends redirect url to monitoring if redirect fails`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", server.url("/redirected/here"))
        )
        server.enqueue(
            MockResponse().setResponseCode(404).setBody("Not found")
        )

        val monitoringTracker = mock<MonitoringTracker> {}
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MonitoringInterceptor(monitoringTracker))
            .build()

        okHttpClient.newCall(Request.Builder().url(server.url("/")).build()).execute()

        verify(monitoringTracker).httpError(server.url("/redirected/here").toString(), 404)
        verifyNoMoreInteractions(monitoringTracker)
    }

    @Test
    fun `Sends timeout errors to monitorings`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setSocketPolicy(SocketPolicy.NO_RESPONSE)
                .setBody("Hello world")
        )

        val monitoringTracker = mock<MonitoringTracker> {}
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.MILLISECONDS)
            .addInterceptor(MonitoringInterceptor(monitoringTracker))
            .build()

        lateinit var exception: SocketTimeoutException
        try {
            okHttpClient.newCall(Request.Builder().url(server.url("/")).build()).execute()
        } catch (error: SocketTimeoutException) {
            exception = error
        }

        verify(monitoringTracker).networkError(
            server.url("/").toString(),
            "${exception::class.java.name}: ${exception.message}"
        )
        verifyNoMoreInteractions(monitoringTracker)
    }

    @Test
    fun `Sends disconnection errors to monitorings`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                .setBody("Hello world")
        )

        val monitoringTracker = mock<MonitoringTracker> {}
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MonitoringInterceptor(monitoringTracker))
            .build()

        lateinit var exception: IOException
        try {
            okHttpClient.newCall(Request.Builder().url(server.url("/")).build()).execute()
        } catch (error: IOException) {
            exception = error
        }

        verify(monitoringTracker).networkError(
            server.url("/").toString(),
            "${exception::class.java.name}: ${exception.message}"
        )
        verifyNoMoreInteractions(monitoringTracker)
    }
}
