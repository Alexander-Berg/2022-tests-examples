package ru.yandex.market.clean.domain.usecase.cashback

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.checkout.checkoutCommonUserInputTestInstance
import ru.yandex.market.clean.domain.usecase.bnpl.IsBnplEnabledUseCase

class GetCheckoutBnplSwitchedUseCaseReduxTest {

    private val isBnplEnabledUseCase = mock<IsBnplEnabledUseCase> {
        on { execute() } doReturn Single.just(true)
    }

    private val useCase = GetCheckoutBnplSwitchedUseCaseRedux(isBnplEnabledUseCase = isBnplEnabledUseCase)

    @Test
    fun `return false if toggle off`() {
        whenever(isBnplEnabledUseCase.execute()) doReturn Single.just(false)

        useCase.execute(checkoutCommonUserInputTestInstance(isBnplSwitched = true)).test().assertValue(false)
    }

    @Test
    fun `return false if value from state is null`() {
        useCase.execute(checkoutCommonUserInputTestInstance(isBnplSwitched = null)).test().assertValue(false)
    }

    @Test
    fun `return false if value from state is false`() {
        useCase.execute(checkoutCommonUserInputTestInstance(isBnplSwitched = false)).test().assertValue(false)
    }

    @Test
    fun `return true if value from state is true`() {
        useCase.execute(checkoutCommonUserInputTestInstance(isBnplSwitched = true)).test().assertValue(true)
    }
}