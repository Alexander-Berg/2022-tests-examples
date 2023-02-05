package ru.yandex.market.clean.domain.usecase.bnpl

import com.annimon.stream.Optional
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.checkout.CheckoutFlowStateRepository

class GetCheckoutBnplSwitchedUseCaseTest {
    private val bnplEnabledStream: PublishSubject<Optional<Boolean>> = PublishSubject.create()
    private val checkoutFlowStateRepository = mock<CheckoutFlowStateRepository> {
        on { getBnplEnabledStream() } doReturn bnplEnabledStream
    }
    private val isBnplEnabledUseCase = mock<IsBnplEnabledUseCase> {
        on { execute() } doReturn Single.just(true)
    }

    private val useCase = GetCheckoutBnplSwitchedUseCase(
        checkoutFlowStateRepository = checkoutFlowStateRepository,
        isBnplEnabledUseCase = isBnplEnabledUseCase
    )

    @Test
    fun `return false if toggle off`() {
        whenever(isBnplEnabledUseCase.execute()) doReturn Single.just(false)

        useCase.execute().test().assertValue(false)
    }

    @Test
    fun `return false if value undefind`() {
        val testObserver = useCase.execute().test()
        bnplEnabledStream.onNext(Optional.empty())

        testObserver.assertValue(false)
    }

    @Test
    fun `observe value`() {
        val testObserver = useCase.execute().test()
        bnplEnabledStream.onNext(Optional.of(true))
        bnplEnabledStream.onNext(Optional.of(false))

        testObserver.assertValues(true, false)
    }
}