package ru.yandex.market.clean.domain.usecase.cashback

import org.junit.Test
import ru.yandex.market.clean.domain.model.orderCreatedBucketResultTestInstance
import ru.yandex.market.clean.domain.model.successOrderResultTestInstance
import java.math.BigDecimal

class GetSuccessOrderSpendCashbackUseCaseTest {

    private val useCase = GetSuccessOrderSpendCashbackUseCase()

    @Test
    fun `calculate spend cashback from all orders`() {
        val orderResult = successOrderResultTestInstance(
            buckets = listOf(
                orderCreatedBucketResultTestInstance(spendCashbackValue = BigDecimal(150)),
                orderCreatedBucketResultTestInstance(spendCashbackValue = BigDecimal(450)),
            )
        )
        useCase.execute(orderResult)
            .test()
            .assertValue(BigDecimal(600))
    }

    @Test
    fun `return zero if there was no spend cashback for any order`() {
        val orderResult = successOrderResultTestInstance(
            buckets = listOf(
                orderCreatedBucketResultTestInstance(spendCashbackValue = null),
                orderCreatedBucketResultTestInstance(spendCashbackValue = BigDecimal.ZERO),
            )
        )
        useCase.execute(orderResult)
            .test()
            .assertValue(BigDecimal.ZERO)
    }
}