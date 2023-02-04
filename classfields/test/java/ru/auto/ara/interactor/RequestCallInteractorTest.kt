package ru.auto.ara.interactor

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
 import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.ara.RxTest
import ru.auto.ara.data.repository.RequestCallRepository
import ru.auto.ara.network.ConnectionHelper
import ru.auto.ara.network.api.error.common.ServerClientException
import ru.auto.data.network.scala.response.BaseResponse
import rx.Single
import rx.observers.TestSubscriber
import java.net.UnknownHostException


/**
 * @author aleien on 07.06.17.
 */
@RunWith(AllureRunner::class) class RequestCallInteractorTest : RxTest() {
    private lateinit var interactor: RequestCallInteractor
    private val api: RequestCallRepository = mock()
    private val connector: ConnectionHelper = mock()
    private val request: PhoneCallbackRequest = mock()

    @Before
    fun setUp() {
        interactor = RequestCallInteractor(api, connector)
    }

    @Test
    fun `if no internet should send server offline exception`() {
        whenever(connector.isOnline()).thenReturn(false)
        whenever(api.requestCall(any<PhoneCallbackRequest>())).thenReturn(Single.error(UnknownHostException()))

        val sub = TestSubscriber.create<PhoneCallbackRequest>()
        interactor.requestCall(request)
            .subscribe(sub)

        sub.assertError(ServerClientException::class.java)
        val error = sub.onErrorEvents[0] as ServerClientException
        assertThat(error.errorCode).isEqualTo(ServerClientException.ERROR_OFFLINE)
    }

    @Test
    fun `if unexpected error should send unknown error`() {
        whenever(connector.isOnline()).thenReturn(true)
        whenever(api.requestCall(any<PhoneCallbackRequest>())).thenReturn(Single.error(RuntimeException()))

        val sub = TestSubscriber.create<PhoneCallbackRequest>()
        interactor.requestCall(request)
            .subscribe(sub)

        sub.assertError(RuntimeException::class.java)
    }

    @Test
    fun `if no errors should return call response`() {
        whenever(connector.isOnline()).thenReturn(true)
        whenever(api.requestCall(any<PhoneCallbackRequest>())).thenReturn(Single.just(BaseResponse()))

        val sub = TestSubscriber.create<Any>()
        interactor.requestCall(request)
            .subscribe(sub)

        sub.assertNoErrors()
        sub.assertCompleted()
        sub.assertUnsubscribed()
        sub.assertTerminalEvent()
    }
}
