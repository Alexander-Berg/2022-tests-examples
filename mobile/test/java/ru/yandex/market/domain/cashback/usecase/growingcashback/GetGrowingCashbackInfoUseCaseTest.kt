package ru.yandex.market.domain.cashback.usecase.growingcashback

import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionInfo
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionState
import ru.yandex.market.domain.cashback.model.growingCashbackActionInfoTestInstance
import ru.yandex.market.domain.cashback.model.growingCashbackActionStateTestInstance
import ru.yandex.market.domain.cashback.repository.GrowingCashbackRepository

class GetGrowingCashbackInfoUseCaseTest {

    private val infoSubject: SingleSubject<GrowingCashbackActionInfo> = SingleSubject.create()
    private val repository = mock<GrowingCashbackRepository> {
        on { getDetailedActionInfo() } doReturn infoSubject
    }
    private val useCase = GetGrowingCashbackInfoUseCase(repository)

    @Test
    fun `return action info fro repository`() {
        val expected = growingCashbackActionInfoTestInstance()
        infoSubject.onSuccess(expected)
        useCase.execute()
            .test()
            .assertValue(expected)
    }

    @Test
    fun `do not handle errors from repository`() {
        val expected = IllegalArgumentException()
        infoSubject.onError(expected)
        useCase.execute()
            .test()
            .assertError(expected)
    }

    @Test
    fun `return error if action is not ended and has empty ordersReward`() {
        infoSubject.onSuccess(
            growingCashbackActionInfoTestInstance(
                actionState = growingCashbackActionStateTestInstance(state = GrowingCashbackActionState.State.ACTIVE),
                ordersReward = emptyList()
            )
        )
        useCase.execute()
            .test()
            .assertError(IllegalStateException::class.java)
    }

    @Test
    fun `return value action has empty ordersReward and action empty`() {
        val expected = growingCashbackActionInfoTestInstance(
            actionState = growingCashbackActionStateTestInstance(state = GrowingCashbackActionState.State.END),
            ordersReward = emptyList()
        )
        infoSubject.onSuccess(expected)
        useCase.execute()
            .test()
            .assertValue(expected)
    }
}