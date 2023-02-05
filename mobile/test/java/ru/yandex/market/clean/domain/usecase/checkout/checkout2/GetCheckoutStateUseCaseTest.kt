package ru.yandex.market.clean.domain.usecase.checkout.checkout2

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import ru.yandex.market.clean.data.repository.checkout.CheckoutFlowStateRepository
import ru.yandex.market.clean.domain.model.checkout.CheckoutFlowState
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.test.extensions.asObservable
import java.util.concurrent.TimeUnit

class GetCheckoutStateUseCaseTest {

    private val timerScheduler = TestScheduler()

    private val checkoutFlowRepository = mock<CheckoutFlowStateRepository>()
    private val presentationSchedulers = presentationSchedulersMock {
        on { timer } doReturn timerScheduler
    }

    private val useCase = GetCheckoutStateUseCase(checkoutFlowRepository, presentationSchedulers)

    @Test
    fun `First emit without debounce`() {
        val value = mock<CheckoutFlowState>()
        whenever(checkoutFlowRepository.getFlowStateStream()).thenReturn(value.asObservable())

        useCase.getCheckoutStateStream()
            .test()
            .assertValue(value)
    }

    @Test
    fun `First emit does not repeat`() {
        val value = mock<CheckoutFlowState>()
        whenever(checkoutFlowRepository.getFlowStateStream()).thenReturn(value.asObservable())

        val testSubscriber = useCase.getCheckoutStateStream().test()
        timerScheduler.triggerActions()
        testSubscriber.assertValue(value)
    }

    @Test
    fun `Second emit with debounce`() {
        val value1 = mock<CheckoutFlowState>()
        val value2 = mock<CheckoutFlowState>()
        val subject = PublishSubject.create<CheckoutFlowState>()

        whenever(checkoutFlowRepository.getFlowStateStream()).thenReturn(subject)

        val testSubscriber = useCase.getCheckoutStateStream().test()

        subject.onNext(value1) // will be skipped
        subject.onNext(value1)
        testSubscriber.assertValues(value1)

        subject.onNext(value2)
        testSubscriber.assertValues(value1)

        timerScheduler.advanceTimeTo(200, TimeUnit.MILLISECONDS)
        testSubscriber.assertValues(value1, value2)
    }

}