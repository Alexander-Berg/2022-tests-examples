package ru.yandex.market.clean.domain.usecase.cms

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.cms.CmsGrowingCashbackItem
import ru.yandex.market.clean.domain.model.cms.garson.CmsWidgetGarson
import ru.yandex.market.clean.domain.model.cms.garson.growingCashbackGarsonTestInstance
import ru.yandex.market.domain.cashback.model.GrowingCashbackActionState
import ru.yandex.market.domain.cashback.model.growingCashbackActionStateTestInstance
import ru.yandex.market.domain.cashback.usecase.growingcashback.GetGrowingCashbackActionStateUseCase
import ru.yandex.market.domain.cashback.usecase.growingcashback.GrowingCashbackCommunicationEnabledUseCase

class CmsGrowingCashbackUseCaseTest {

    private val rightGarson = growingCashbackGarsonTestInstance()
    private val wrongGarson = mock<CmsWidgetGarson>()
    private val growingCashbackCommunicationEnabledUseCase = mock<GrowingCashbackCommunicationEnabledUseCase> {
        on { execute() } doReturn Observable.just(true)
    }
    private val growingCashbackActionStateUseCase = mock<GetGrowingCashbackActionStateUseCase> {
        on { execute() } doReturn
                Single.just(
                    growingCashbackActionStateTestInstance(state = GrowingCashbackActionState.State.ACTIVE)
                )
    }

    private val useCase = CmsGrowingCashbackUseCase(
        growingCashbackCommunicationEnabledUseCase,
        growingCashbackActionStateUseCase
    )

    @Test
    fun `return empty list items if feature disabled`() {
        whenever(growingCashbackCommunicationEnabledUseCase.execute()) doReturn Observable.just(false)
        useCase.execute(rightGarson)
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `return empty list items if used wrong garson`() {
        useCase.execute(wrongGarson)
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `return empty list items if action ended`() {
        whenever(growingCashbackActionStateUseCase.execute()) doReturn Single.just(
            growingCashbackActionStateTestInstance(state = GrowingCashbackActionState.State.END)
        )
        useCase.execute(rightGarson)
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `return value for active action`() {
        val actionInfo = growingCashbackActionStateTestInstance(state = GrowingCashbackActionState.State.ACTIVE)
        whenever(growingCashbackActionStateUseCase.execute()) doReturn Single.just(actionInfo)
        useCase.execute(rightGarson)
            .test()
            .assertValue(
                listOf(
                    CmsGrowingCashbackItem(
                        ordersCount = actionInfo.maxOrdersCount,
                        maxReward = actionInfo.maxReward,
                        icon = rightGarson.icon,
                        state = CmsGrowingCashbackItem.State.GET_REWARD,
                    )
                )
            )
    }

    @Test
    fun `return value for action terms violation`() {
        val actionInfo = growingCashbackActionStateTestInstance(
            state = GrowingCashbackActionState.State.TERMS_VIOLATION
        )
        whenever(growingCashbackActionStateUseCase.execute()) doReturn Single.just(actionInfo)
        useCase.execute(rightGarson)
            .test()
            .assertValue(
                listOf(
                    CmsGrowingCashbackItem(
                        ordersCount = actionInfo.maxOrdersCount,
                        maxReward = actionInfo.maxReward,
                        icon = rightGarson.icon,
                        state = CmsGrowingCashbackItem.State.ACTION_TERMS_VIOLATION
                    )
                )
            )
    }

    @Test
    fun `return value for active action and full reward received`() {
        val actionInfo = growingCashbackActionStateTestInstance(state = GrowingCashbackActionState.State.COMPLETE)
        whenever(growingCashbackActionStateUseCase.execute()) doReturn Single.just(actionInfo)
        useCase.execute(rightGarson)
            .test()
            .assertValue(
                listOf(
                    CmsGrowingCashbackItem(
                        ordersCount = actionInfo.maxOrdersCount,
                        maxReward = actionInfo.maxReward,
                        icon = rightGarson.icon,
                        state = CmsGrowingCashbackItem.State.ACTION_COMPLETE
                    )
                )
            )
    }
}