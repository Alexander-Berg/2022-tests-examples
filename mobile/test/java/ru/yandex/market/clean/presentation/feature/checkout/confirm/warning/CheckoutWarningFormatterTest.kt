package ru.yandex.market.clean.presentation.feature.checkout.confirm.warning

import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.checkout.bucketInfo2TestInstance
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.checkout.deliveryOptionModelTestInstance
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.data.order.options.deliveryOptionTestInstance
import ru.yandex.market.data.order.options.undefinedDeliveryConfigTestInstance
import ru.yandex.market.domain.delivery.model.DeliveryType
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.doReturn
import ru.beru.android.R

internal class CheckoutWarningFormatterTest {

    private val resourcesManager = mock<ResourcesManager> {
        on {
            getFormattedString(R.string.checkout_confirm_estimated_delivery_description)
        } doReturn ESTIMATE_DELIVERY

        on {
            getFormattedString(R.string.checkout_confirm_unique_order_description)
        } doReturn UNIQUE_ORDER_WITH_ESTIMATE_DELIVERY
    }

    private val formatter = CheckoutWarningFormatter(resourcesManager)

    @Test
    fun `Estimate delivery warning`() {
        val expectedText = resourcesManager.getFormattedString(R.string.checkout_confirm_estimated_delivery_description)
        val actual = formatter.format(CHECKOUT_SPLIT_1)
        assertThat(actual?.description.orEmpty()).isEqualTo(expectedText)
    }

    @Test
    fun `Unique order with estimate delivery warning`() {
        val expectedText = resourcesManager.getFormattedString(R.string.checkout_confirm_unique_order_description)
        val actual = formatter.format(CHECKOUT_SPLIT_2)
        assertThat(actual?.description.orEmpty()).isEqualTo(expectedText)
    }

    @Test
    fun `Unique order warning is null`() {
        val actual = formatter.format(CHECKOUT_SPLIT_3)
        assertThat(actual).isEqualTo(null)
    }

    companion object {
        private const val ESTIMATE_DELIVERY =
            "Дата доставки ориентировочная. Продавец согласует с вами точную дату и время."

        private const val UNIQUE_ORDER_WITH_ESTIMATE_DELIVERY =
            "Дата доставки товара под заказ ориентировочная. Продавец согласует\u2028с вами точную дату и время."

        private val CHECKOUT_SPLIT_1 = checkoutSplitTestInstance(
            buckets = listOf(
                bucketInfo2TestInstance(
                    selectedDeliveryOption = mapOf(
                        DeliveryType.DELIVERY to deliveryOptionModelTestInstance(
                            deliveryOption = deliveryOptionTestInstance(
                                undefinedDeliveryConfig = undefinedDeliveryConfigTestInstance(
                                    isEstimatedDeliveryTime = true,
                                    isUniqueOrder = false,
                                )
                            )
                        )
                    )
                )
            )
        )

        private val CHECKOUT_SPLIT_2 = checkoutSplitTestInstance(
            buckets = listOf(
                bucketInfo2TestInstance(
                    selectedDeliveryOption = mapOf(
                        DeliveryType.DELIVERY to deliveryOptionModelTestInstance(
                            deliveryOption = deliveryOptionTestInstance(
                                undefinedDeliveryConfig = undefinedDeliveryConfigTestInstance(
                                    isEstimatedDeliveryTime = true,
                                    isUniqueOrder = true,
                                )
                            )
                        ),
                    )
                )
            )
        )

        private val CHECKOUT_SPLIT_3 = checkoutSplitTestInstance(
            buckets = listOf(
                bucketInfo2TestInstance(
                    selectedDeliveryOption = mapOf(
                        DeliveryType.DELIVERY to deliveryOptionModelTestInstance(
                            deliveryOption = deliveryOptionTestInstance(
                                undefinedDeliveryConfig = undefinedDeliveryConfigTestInstance(
                                    isEstimatedDeliveryTime = false,
                                    isUniqueOrder = true,
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
