package ru.yandex.yandexmaps.common.retrofit

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import ru.yandex.yandexmaps.common.network.okhttp.MonitoringInterceptor
import ru.yandex.yandexmaps.multiplatform.core.monitoring.MonitoringTracker

@JsonClass(generateAdapter = true)
data class TestModel(val a: Int)

interface Service {
    @GET("/")
    fun root(): Call<TestModel>
}

interface RxService {
    @GET("/")
    fun root(): Single<TestModel>
}

class MonitoringCallAdapterFactoryTests {

    @get:Rule
    val server = MockWebServer()

    @Test
    fun `Unexpected JSON`() {
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))
        val monitoringTracker = mock<MonitoringTracker>()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(MonitoringCallAdapterFactory(monitoringTracker))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(Service::class.java)

        lateinit var error: Throwable
        try {
            service.root().execute()
        } catch (e: Throwable) {
            error = e
        }

        Assert.assertTrue(error::class.java == JsonDataException::class.java)
        verify(monitoringTracker, times(1)).jsonParsingError(
            server.url("/").toString(),
            MonitoringTracker.JsonParsingErrorType.UNEXPECTED_FORMAT,
            "${error::class.java.name}: ${error.message}"
        )
    }

    @Test
    fun `Malformed JSON`() {
        server.enqueue(MockResponse().setBody("{\"a\":}"))
        val monitoringTracker = mock<MonitoringTracker>()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(MonitoringCallAdapterFactory(monitoringTracker))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(Service::class.java)

        lateinit var error: Throwable
        try {
            service.root().execute()
        } catch (e: Throwable) {
            error = e
        }

        Assert.assertTrue(error::class.java == JsonEncodingException::class.java)
        verify(monitoringTracker, times(1)).jsonParsingError(
            server.url("/").toString(),
            MonitoringTracker.JsonParsingErrorType.MALFORMED,
            "${error::class.java.name}: ${error.message}"
        )
    }

    @Test
    fun `RxJava2CallAdapterFactory with unexpected JSON`() {
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))
        val monitoringTracker = mock<MonitoringTracker>()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(MonitoringCallAdapterFactory(monitoringTracker))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(RxService::class.java)

        val observer = service.root().test()
        observer.awaitTerminalEvent()

        observer.assertError(JsonDataException::class.java)
        verify(monitoringTracker, times(1)).jsonParsingError(
            server.url("/").toString(),
            MonitoringTracker.JsonParsingErrorType.UNEXPECTED_FORMAT,
            "${JsonDataException::class.java.name}: ${observer.errors()[0].message}"
        )
    }

    @Test
    fun `Order of CallAdapters matters`() {
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))
        val monitoringTracker = mock<MonitoringTracker>()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addCallAdapterFactory(MonitoringCallAdapterFactory(monitoringTracker))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(RxService::class.java)

        val observer = service.root().test()
        observer.awaitTerminalEvent()

        observer.assertError(JsonDataException::class.java)
        verify(monitoringTracker, never()).jsonParsingError(any(), any(), any())
    }

    @Test
    fun `Multiple Subscriptions are OK`() {
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))
        val monitoringTracker = mock<MonitoringTracker>()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(MonitoringCallAdapterFactory(monitoringTracker))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(RxService::class.java)

        val observable = service.root()

        val observer1 = observable.test()
        observer1.awaitTerminalEvent()

        val observer2 = observable.test()
        observer2.awaitTerminalEvent()

        observer1.assertError(JsonDataException::class.java)
        observer2.assertError(JsonDataException::class.java)
        verify(monitoringTracker, times(2)).jsonParsingError(
            server.url("/").toString(),
            MonitoringTracker.JsonParsingErrorType.UNEXPECTED_FORMAT,
            "${JsonDataException::class.java.name}: ${observer1.errors()[0].message}"
        )
    }

    @Test
    fun `Retries working OK`() {
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))
        val monitoringTracker = mock<MonitoringTracker>()
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(MonitoringCallAdapterFactory(monitoringTracker))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(RxService::class.java)

        val observer = service.root().retry(2).test()

        observer.assertError(JsonDataException::class.java)
        verify(monitoringTracker, times(3)).jsonParsingError(
            server.url("/").toString(),
            MonitoringTracker.JsonParsingErrorType.UNEXPECTED_FORMAT,
            "${JsonDataException::class.java.name}: ${observer.errors()[0].message}"
        )
        verifyNoMoreInteractions(monitoringTracker)
    }

    @Test
    fun `OkHttp monitoring interceptor does not catch JSON errors`() {
        server.enqueue(MockResponse().setBody("{\"a\": \"kek\" }"))

        val monitoringTracker = mock<MonitoringTracker>()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MonitoringInterceptor(monitoringTracker))
            .build()

        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(MonitoringCallAdapterFactory(monitoringTracker))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .client(okHttpClient)
            .build()
            .create(RxService::class.java)

        val observer = service.root().test()

        observer.assertError(JsonDataException::class.java)

        verify(monitoringTracker, times(1)).jsonParsingError(
            server.url("/").toString(),
            MonitoringTracker.JsonParsingErrorType.UNEXPECTED_FORMAT,
            "${JsonDataException::class.java.name}: ${observer.errors()[0].message}"
        )
        verifyNoMoreInteractions(monitoringTracker)
    }

    @Test
    fun `MonitoringCallAdapterFactory does not intercept HTTP errors`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("{\"a\": \"2\" }"))

        val monitoringTracker = mock<MonitoringTracker>()
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(MonitoringInterceptor(monitoringTracker))
            .build()

        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(MonitoringCallAdapterFactory(monitoringTracker))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .client(okHttpClient)
            .build()
            .create(RxService::class.java)

        val observer = service.root().test()

        observer.assertError(HttpException::class.java)

        verify(monitoringTracker, times(1)).httpError(
            server.url("/").toString(),
            500
        )
        verifyNoMoreInteractions(monitoringTracker)
    }
}
