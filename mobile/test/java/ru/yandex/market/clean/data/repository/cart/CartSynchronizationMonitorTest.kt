package ru.yandex.market.clean.data.repository.cart

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.common.schedulers.DataSchedulers
import ru.yandex.market.utils.advanceTimeBy
import ru.yandex.market.utils.seconds
import ru.yandex.market.clean.data.WebViewCartEventsMonitor
import ru.yandex.market.clean.data.model.WebCartEvent
import ru.yandex.market.clean.data.model.webCartEvent_OnErrorTestInstance
import ru.yandex.market.clean.data.model.webCartEvent_OnStartTestInstance
import ru.yandex.market.clean.data.model.webCartEvent_OnSuccessTestInstance

class CartSynchronizationMonitorTest {

    private val webViewEvents = PublishSubject.create<WebCartEvent>()
    private val webViewCartEventsMonitor = mock<WebViewCartEventsMonitor> {
        on { getCartEventsStream() } doReturn webViewEvents.hide()
    }
    private val schedulers = mock<DataSchedulers> {
        on { localSingleThread } doReturn Schedulers.trampoline()
        on { worker } doReturn Schedulers.trampoline()
    }
    private val configuration = CartSynchronizationMonitor.Configuration(waitingDuration = DURATION)
    private val monitor = CartSynchronizationMonitor(webViewCartEventsMonitor, schedulers, configuration)

    @Test
    fun `Shares same observable for multiple subscribers`() {
        monitor.getCartSynchronizationEventsStream().test()
        monitor.getCartSynchronizationEventsStream().test()

        verify(webViewCartEventsMonitor).getCartEventsStream()
    }

    @Test
    fun `Cleanup resources after last subscriber is gone`() {
        val firstObserver = monitor.getCartSynchronizationEventsStream().test()
        val secondObserver = monitor.getCartSynchronizationEventsStream().test()

        webViewEvents.onNext(webCartEvent_OnStartTestInstance("1"))
        webViewEvents.onNext(webCartEvent_OnStartTestInstance("2"))

        assertThat(monitor.waitingDisposablesCount).isNotEqualTo(0)

        firstObserver.dispose()
        secondObserver.dispose()

        assertThat(monitor.waitingDisposablesCount).isEqualTo(0)
    }

    @Test
    fun `Subscribing to cleared observable again resumes monitor work`() {
        monitor.getCartSynchronizationEventsStream().test().dispose()
        val observer = monitor.getCartSynchronizationEventsStream().test()

        webViewEvents.onNext(webCartEvent_OnSuccessTestInstance())

        observer.assertNoErrors()
            .assertNotComplete()
            .assertValueCount(1)
    }

    @Test
    fun `Starts waiting for result on start event`() {
        val testScheduler = TestScheduler()
        whenever(schedulers.worker) doReturn testScheduler

        val observer = monitor.getCartSynchronizationEventsStream().test()
        webViewEvents.onNext(webCartEvent_OnStartTestInstance())

        observer.assertNoErrors()
            .assertNoValues()
            .assertNotTerminated()
        assertThat(monitor.waitingDisposablesCount).isEqualTo(1)
    }

    @Test
    fun `Stops waiting for result on success event`() {
        monitor.getCartSynchronizationEventsStream().test()
        webViewEvents.onNext(webCartEvent_OnStartTestInstance("1"))
        webViewEvents.onNext(webCartEvent_OnSuccessTestInstance("1"))

        assertThat(monitor.waitingDisposablesCount).isEqualTo(0)
    }

    @Test
    fun `Stops waiting for result on error event`() {
        monitor.getCartSynchronizationEventsStream().test()
        webViewEvents.onNext(webCartEvent_OnStartTestInstance("1"))
        webViewEvents.onNext(webCartEvent_OnErrorTestInstance("1"))

        assertThat(monitor.waitingDisposablesCount).isEqualTo(0)
    }

    @Test
    fun `Report cart synchronization event on success event`() {
        val observer = monitor.getCartSynchronizationEventsStream().test()
        webViewEvents.onNext(webCartEvent_OnSuccessTestInstance())

        observer.assertNoErrors()
            .assertValueCount(1)
            .assertNotComplete()
    }

    @Test
    fun `Do not report cart synchronization event on error event`() {
        val observer = monitor.getCartSynchronizationEventsStream().test()
        webViewEvents.onNext(webCartEvent_OnErrorTestInstance())

        observer.assertNoErrors()
            .assertValueCount(0)
            .assertNotComplete()
    }

    @Test
    fun `Report cart synchronization event after waiting duration is passed`() {
        val testScheduler = TestScheduler()
        whenever(schedulers.worker) doReturn testScheduler

        val observer = monitor.getCartSynchronizationEventsStream().test()
        webViewEvents.onNext(webCartEvent_OnStartTestInstance())

        observer.assertNoErrors()
            .assertValueCount(0)
            .assertNotComplete()

        testScheduler.advanceTimeBy(DURATION)

        observer.assertNoErrors()
            .assertValueCount(1)
            .assertNotComplete()
    }

    companion object {
        private val DURATION = 5.seconds
    }
}