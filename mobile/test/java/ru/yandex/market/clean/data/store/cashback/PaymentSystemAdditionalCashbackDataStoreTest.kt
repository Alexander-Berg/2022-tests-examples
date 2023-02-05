package ru.yandex.market.clean.data.store.cashback

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.data.cashback.network.dto.order.PaymentSystemCashbackDto
import ru.yandex.market.data.cashback.network.dto.order.paymentSystemCashbackDtoTestInstance

@RunWith(Parameterized::class)
class PaymentSystemAdditionalCashbackDataStoreTest(
    private val saveOrderIds: List<String>,
    private val getOrderIds: List<String>,
    private val saveDto: PaymentSystemCashbackDto,
    private val getDto: PaymentSystemCashbackDto?
) {

    private val dataStore = PaymentSystemAdditionalCashbackDataStore()

    @Test
    fun `test save and get`() {
        dataStore.saveAdditionalPaymentSystemCashback(saveOrderIds, saveDto)
        val actual = dataStore.getAdditionalPaymentSystemCashback(getOrderIds)
        assertThat(actual).isEqualTo(getDto)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0}, {1} -> {3}")
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                listOf("order"),
                listOf("order"),
                paymentSystemCashbackDtoTestInstance(),
                paymentSystemCashbackDtoTestInstance()
            ),
            //1
            arrayOf(
                listOf("order1", "order2"),
                listOf("order1", "order2"),
                paymentSystemCashbackDtoTestInstance(),
                paymentSystemCashbackDtoTestInstance()
            ),
            //2
            arrayOf(
                listOf("order2", "order1"),
                listOf("order1", "order2"),
                paymentSystemCashbackDtoTestInstance(),
                paymentSystemCashbackDtoTestInstance()
            ),
            //3
            arrayOf(
                listOf("order1", "order2", "order3"),
                listOf("order1", "order3", "order2"),
                paymentSystemCashbackDtoTestInstance(),
                paymentSystemCashbackDtoTestInstance()
            ),
            //4
            arrayOf(
                listOf("order1", "order2"),
                listOf("order3", "order4"),
                paymentSystemCashbackDtoTestInstance(),
                null
            ),
            //5
            arrayOf(
                listOf("order1", "order2", "order3"),
                listOf("order1", "order2"),
                paymentSystemCashbackDtoTestInstance(),
                null
            ),
            //6
            arrayOf(
                listOf("order1", "order2"),
                listOf("order1", "order2", "order3"),
                paymentSystemCashbackDtoTestInstance(),
                null
            ),
            //7
            arrayOf(
                emptyList<String>(),
                listOf("order1", "order2"),
                paymentSystemCashbackDtoTestInstance(),
                null
            ),
            //8
            arrayOf(
                listOf("order1", "order2"),
                emptyList<String>(),
                paymentSystemCashbackDtoTestInstance(),
                null
            ),
            //9
            arrayOf(
                emptyList<String>(),
                emptyList<String>(),
                paymentSystemCashbackDtoTestInstance(),
                null
            ),
        )
    }
}