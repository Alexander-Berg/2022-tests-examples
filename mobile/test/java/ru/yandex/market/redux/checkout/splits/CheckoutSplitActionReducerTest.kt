package ru.yandex.market.redux.checkout.splits

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.base.redux.stateobject.asStateObject
import ru.yandex.market.clean.domain.model.checkout.CheckoutSplit
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.presentation.formatter.error.checkout.CargoLiftingType
import ru.yandex.market.data.order.options.point.OutletPoint
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.domain.models.region.deliveryLocalityTestInstance
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.redux.actions.checkout.splits.CheckoutSplitAction
import ru.yandex.market.redux.reducers.checkout.splits.reduceCheckoutSplitAction

@RunWith(Parameterized::class)
class CheckoutSplitActionReducerTest(
    private val input: CheckoutSplit,
    private val action: CheckoutSplitAction,
    private val output: CheckoutSplit
) {

    @Test
    fun `Split updated by CheckoutSplitAction`() {
        val inputStateObject = mapOf(input.id to input).asStateObject()
        val outputStateObject = mapOf(output.id to output).asStateObject()

        assertThat(outputStateObject).isEqualTo(reduceCheckoutSplitAction(inputStateObject, action))
    }

    companion object {

        private val emptyCheckoutSplit = checkoutSplitTestInstance(
            regionId = Long.MIN_VALUE,
            deliveryLocality = deliveryLocalityTestInstance().copy(regionId = Long.MIN_VALUE),
            selectedDeliveryType = DeliveryType.DELIVERY,
            selectedPostOutletPoint = null,
            selectedUserAddress = null,
            liftingDeliveryComment = "",
            selectedLiftingType = CargoLiftingType.MANUAL,
        )

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> {
            return listOf(
                with(emptyCheckoutSplit) {
                    arrayOf(
                        this,
                        CheckoutSplitAction.UpdateSplitRegionIdAction(id, Long.MAX_VALUE),
                        copy(regionId = Long.MAX_VALUE)
                    )
                },
                with(emptyCheckoutSplit) {
                    arrayOf(
                        this,
                        CheckoutSplitAction.UpdateSplitDeliveryLocalityAction(id, deliveryLocalityTestInstance()),
                        copy(deliveryLocality = deliveryLocalityTestInstance())
                    )
                },
                with(emptyCheckoutSplit) {
                    arrayOf(
                        this,
                        CheckoutSplitAction.UpdateSplitDeliveryTypeAction(id, DeliveryType.DIGITAL),
                        copy(selectedDeliveryType = DeliveryType.DIGITAL)
                    )
                },
                with(emptyCheckoutSplit) {
                    arrayOf(
                        this,
                        CheckoutSplitAction.UpdateSplitUserAddressAction(id, userAddressTestInstance()),
                        copy(selectedUserAddress = userAddressTestInstance())
                    )
                },
                with(emptyCheckoutSplit) {
                    arrayOf(
                        this,
                        CheckoutSplitAction.UpdateSplitOutletPointAction(id, OutletPoint.testInstance()),
                        copy(selectedPostOutletPoint = OutletPoint.testInstance())
                    )
                },
                with(emptyCheckoutSplit) {
                    arrayOf(
                        this,
                        CheckoutSplitAction.UpdateSplitLiftingTypeAction(id, CargoLiftingType.ELEVATOR),
                        copy(selectedLiftingType = CargoLiftingType.ELEVATOR)
                    )
                },
                with(emptyCheckoutSplit) {
                    arrayOf(
                        this,
                        CheckoutSplitAction.UpdateSplitLiftingDeliveryComment(id, "Comment"),
                        copy(liftingDeliveryComment = "Comment")
                    )
                },
            )
        }
    }
}