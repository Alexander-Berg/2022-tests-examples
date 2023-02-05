package ru.yandex.market.redux.checkout.buckets

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.checkout.domain.model.ErrorsPack
import ru.yandex.market.checkout.domain.model.PackPosition
import ru.yandex.market.checkout.domain.model.packPositionTestInstance
import ru.yandex.market.clean.domain.model.checkout.BucketInfo2
import ru.yandex.market.clean.domain.model.checkout.bucketInfo2TestInstance
import ru.yandex.market.clean.domain.model.checkout.deliveryOptionModelTestInstance
import ru.yandex.market.clean.domain.model.checkout.deliverycustomizer.DeliveryCustomizerType
import ru.yandex.market.clean.domain.model.order.delivery.OrderDeliveryScheme
import ru.yandex.market.clean.domain.model.orderItemTestInstance
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.redux.actions.checkout.buckets.BucketAction
import ru.yandex.market.redux.reducers.checkout.buckets.changeStateFieldByAction

@RunWith(Parameterized::class)
class BucketsReducerTest(
    private val input: BucketInfo2,
    private val action: BucketAction,
    private val output: BucketInfo2,
) {

    @Test
    fun `Bucket updated by actions`() {
        assertThat(output).isEqualTo(input.changeStateFieldByAction(action))
    }

    companion object {

        private val emptyBucketInfo = bucketInfo2TestInstance().copy(
            packPosition = PackPosition(false, "", 0, 0),
            orderItems = emptyList(),
            errorsPack = ErrorsPack.empty(),
            selectedPaymentMethod = null,
            shopId = Long.MIN_VALUE,
            availablePaymentMethods = emptyList(),
            onDemandDeliveryOption = null,
            isOnDemandDeliverySelected = false,
            onDemandPaymentMethod = null,
            onDemandAvailablePaymentMethods = emptyList(),
            orderDeliveryScheme = null,
            deliveryCustomizersState = emptyMap(),
        )

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> {
            return listOf(
                with(emptyBucketInfo) {
                    arrayOf(
                        emptyBucketInfo,
                        BucketAction.UpdateBucketPackPositionAction(packId, packPositionTestInstance()),
                        copy(packPosition = packPositionTestInstance())
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketOrderItemsAction(packId, listOf(orderItemTestInstance())),
                        copy(orderItems = listOf(orderItemTestInstance()))
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketSelectedDeliveryOptionAction(
                            packId,
                            mapOf(DeliveryType.DELIVERY to deliveryOptionModelTestInstance())
                        ),
                        copy(selectedDeliveryOption = mapOf(DeliveryType.DELIVERY to deliveryOptionModelTestInstance()))
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketSelectedPaymentMethodAction(
                            packId,
                            PaymentMethod.GOOGLE_PAY,
                        ),
                        copy(selectedPaymentMethod = PaymentMethod.GOOGLE_PAY)
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketShopIdAction(
                            packId,
                            Long.MAX_VALUE
                        ),
                        copy(shopId = Long.MAX_VALUE)
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketAvailablePaymentMethodsAction(
                            packId,
                            listOf(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX)
                        ),
                        copy(availablePaymentMethods = listOf(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX))
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketOnDemandDeliveryOption(
                            packId,
                            deliveryOptionModelTestInstance()
                        ),
                        copy(onDemandDeliveryOption = deliveryOptionModelTestInstance())
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketIsOnDemandDeliverySelectedAction(
                            packId,
                            true
                        ),
                        copy(isOnDemandDeliverySelected = true)
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketOnDemandPaymentMethodAction(
                            packId,
                            PaymentMethod.GOOGLE_PAY
                        ),
                        copy(onDemandPaymentMethod = PaymentMethod.GOOGLE_PAY)
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketOnDemandAvailablePaymentMethodsAction(
                            packId,
                            listOf(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX)
                        ),
                        copy(onDemandAvailablePaymentMethods = listOf(PaymentMethod.GOOGLE_PAY, PaymentMethod.YANDEX))
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketOrderDeliverySchemeAction(
                            packId,
                            OrderDeliveryScheme.EXPRESS_DELIVERY,
                        ),
                        copy(orderDeliveryScheme = OrderDeliveryScheme.EXPRESS_DELIVERY)
                    )
                },
                with(emptyBucketInfo) {
                    arrayOf(
                        this,
                        BucketAction.UpdateBucketDeliveryCustomizerEnabledAction(
                            packId,
                            mapOf(
                                DeliveryCustomizerType.LEAVE_AT_THE_DOOR to true,
                                DeliveryCustomizerType.NOT_CALL to true,
                            ),
                        ),
                        copy(
                            deliveryCustomizersState = mapOf(
                                DeliveryCustomizerType.LEAVE_AT_THE_DOOR to true,
                                DeliveryCustomizerType.NOT_CALL to true,
                            )
                        )
                    )
                },
            )
        }
    }
}