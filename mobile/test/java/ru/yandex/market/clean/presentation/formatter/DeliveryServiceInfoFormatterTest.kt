package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.clean.domain.model.order.orderTestInstance
import ru.yandex.market.clean.presentation.feature.order.details.status.deliveryServiceInfo.DeliveryServiceInfoFormatter
import ru.yandex.market.clean.presentation.feature.order.details.status.deliveryServiceInfo.DeliveryServiceInfoVo
import ru.yandex.market.clean.presentation.vo.DeliveryServiceContactsVo
import ru.yandex.market.data.order.OrderSubstatus

@RunWith(Parameterized::class)
class DeliveryServiceInfoFormatterTest(
    private val order: Order,
    private val contacts: DeliveryServiceContactsVo?,
    private val expectedResult: DeliveryServiceInfoVo
) {

    private val formatter = DeliveryServiceInfoFormatter()

    @Test
    fun format() {
        val formatted = formatter.format(order, contacts)
        assertThat(formatted).isEqualTo(expectedResult)
    }

    companion object {

        private const val TEST_CONTACTS = "Тестовые данные для контакта"
        private val contacts = DeliveryServiceContactsVo(
            text = TEST_CONTACTS
        )

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            //0
            arrayOf(
                orderTestInstance(
                    subStatus = OrderSubstatus.USER_RECEIVED
                ),
                contacts,
                DeliveryServiceInfoVo(
                    isVisible = false,
                    text = TEST_CONTACTS
                )
            ),
            //1
            arrayOf(
                orderTestInstance(
                    subStatus = OrderSubstatus.SHIPPED
                ),
                contacts,
                DeliveryServiceInfoVo(
                    isVisible = true,
                    text = TEST_CONTACTS
                )
            ),
            //2
            arrayOf(
                orderTestInstance(
                    subStatus = OrderSubstatus.SHIPPED
                ),
                null,
                DeliveryServiceInfoVo(
                    isVisible = false,
                    text = null
                )
            )
        )
    }
}