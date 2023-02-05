package ru.yandex.yandexbus.inhouse.utils.network

import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import rx.Observable
import rx.Single
import rx.observers.AssertableSubscriber
import rx.schedulers.Schedulers
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Assumed:
 * - OkHttp is used, which responds with IOException on server unavailability
 * - Retrofit is used, which responds with HttpException on non-200 response code
 */
class RetryWhenNetworkIsUpTest {

    private lateinit var http400Error: Exception
    private lateinit var http500Error: Exception
    private lateinit var unavailableServerError: Exception
    private lateinit var clientCodeError: Exception

    @Before
    fun setUp() {
        val body = ResponseBody.create(MediaType.get("text/plain"), "")
        http400Error = HttpException(Response.error<String>(400, body))
        http500Error = HttpException(Response.error<String>(500, body))
        unavailableServerError = IOException()
        clientCodeError = Exception()
    }

    @Test
    fun skipNonNetworkError() {
        failureAndThenSuccess(clientCodeError, "whatever")
            .testRetryWithTimeoutWhileConnected()
            .assertError(clientCodeError)
    }

    @Test
    fun retryUnavailableServer() {
        val expectedResponse = "success"

        failureAndThenSuccess(unavailableServerError, expectedResponse)
            .testRetryWithTimeoutWhileConnected()
            .assertValues(expectedResponse)
    }

    @Test
    fun doNotRetryClientError() {
        failureAndThenSuccess(http400Error, "whatever")
            .testRetryWithTimeoutWhileConnected()
            .assertError(http400Error)
    }

    @Test
    fun noInfiniteClientErrorsRetries() {
        failingServer(http400Error)
            .testRetryWithTimeoutWhileConnected()
            .assertError(http400Error)
    }

    @Test
    fun noInfiniteServerErrorsRetries() {
        failingServer(http500Error)
            .testRetryWithTimeoutWhileConnected()
            .assertError(http500Error)
    }

    @Ignore // is it RetryWhenNetworkIsUp responsibility at all to give up?
    @Test
    fun noInfiniteUnavailableServerRetries() {
        failingServer(unavailableServerError)
            .testRetryWithTimeoutWhileConnected()
            .assertError(unavailableServerError)
    }

    private fun <T> Single<T>.testRetryWithTimeoutWhileConnected(): AssertableSubscriber<T> {
        return retryWhen(RetryWhenNetworkIsUp(Observable.just(NetworkInfoProvider.Event.CONNECTED_OR_CONNECTING)))
            .test()
            .awaitTerminalEventAndUnsubscribeOnTimeout(1, TimeUnit.SECONDS)
    }

    private fun <T> failureAndThenSuccess(failure: Exception, success: T): Single<T> {
        val responsesCount = AtomicInteger(0)

        return Single.create<T> { subscriber ->
            if (responsesCount.incrementAndGet() > 1) {
                subscriber.onSuccess(success)
            } else {
                subscriber.onError(failure)
            }
        }.subscribeOn(Schedulers.io())
    }

    private fun failingServer(issue: Exception) = Single.error<Any>(issue).subscribeOn(Schedulers.io())
}