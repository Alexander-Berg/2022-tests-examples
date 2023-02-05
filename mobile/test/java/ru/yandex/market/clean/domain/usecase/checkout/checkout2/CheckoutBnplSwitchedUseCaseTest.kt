package ru.yandex.market.clean.domain.usecase.checkout.checkout2

import io.reactivex.Completable
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.data.repository.checkout.CheckoutFlowStateRepository

class CheckoutBnplSwitchedUseCaseTest {
    private val checkoutFlowStateRepository = mock<CheckoutFlowStateRepository> {
        on { setBnplSwitched(any()) } doReturn Completable.complete()
    }
    private val actualizeCheckoutOrderSummaryUseCase = mock<ActualizeCheckoutOrderSummaryUseCase> {
        on { updateOrderSummaryForCurrentSplits(invalidateCurrent = true) } doReturn Completable.complete()
    }

    private val useCase = CheckoutBnplSwitchedUseCase(
        checkoutFlowStateRepository = checkoutFlowStateRepository,
        actualizeCheckoutOrderSummaryUseCase = actualizeCheckoutOrderSummaryUseCase
    )

    @Test
    fun `actualize summary after value set`() {
        useCase.setBnplSwitched(true).test().assertComplete()

        verify(actualizeCheckoutOrderSummaryUseCase).updateOrderSummaryForCurrentSplits(invalidateCurrent = true)
    }
}