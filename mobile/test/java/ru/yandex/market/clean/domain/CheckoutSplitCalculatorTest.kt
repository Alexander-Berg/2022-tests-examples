package ru.yandex.market.clean.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Test
import ru.yandex.market.data.order.options.DeliveryOption

class CheckoutSplitCalculatorTest {

    private val splitCalculator = CheckoutSplitCalculator()

    @Test
    fun `Empty result for empty options`() {
        val splits = splitCalculator.calculateSplits(emptyMap())

        assertThat(splits).isEmpty()
    }

    @Test
    fun `Packs without common options puts in different splits`() {
        val option1Id = "option-1"
        val option2Id = "option-2"
        val pack1Id = "pack-1"
        val pack2Id = "pack-2"

        val options = mapOf(
            pack1Id to listOf(
                newDeliveryOption(
                    packId = pack1Id,
                    optionId = option1Id
                )
            ),
            pack2Id to listOf(
                newDeliveryOption(
                    packId = pack2Id,
                    optionId = option2Id
                )
            )
        )

        val splits = splitCalculator.calculateSplits(options)

        assertThat(splits).containsOnly(listOf(pack1Id), listOf(pack2Id))
    }

    @Test
    fun `Packs with common options puts in same splits`() {
        val option1Id = "option-1"
        val option2Id = "option-2"
        val option3Id = "option-3"
        val commonOptionId = "common-option"
        val pack1Id = "pack-1"
        val pack2Id = "pack-2"
        val pack3Id = "pack-3"

        val options = mapOf(
            pack1Id to listOf(
                newDeliveryOption(packId = pack1Id, optionId = option1Id),
                newDeliveryOption(packId = pack1Id, optionId = commonOptionId)
            ),
            pack2Id to listOf(
                newDeliveryOption(packId = pack2Id, optionId = option2Id),
                newDeliveryOption(packId = pack1Id, optionId = commonOptionId)
            ),
            pack3Id to listOf(
                newDeliveryOption(packId = pack3Id, optionId = option3Id),
                newDeliveryOption(packId = pack3Id, optionId = commonOptionId)
            )
        )

        val splits = splitCalculator.calculateSplits(options)

        assertThat(splits).`is`(
            HamcrestCondition(contains(containsInAnyOrder(pack1Id, pack2Id, pack3Id)))
        )
    }

    @Test
    fun `Packs puts into one split only if all has common option`() {
        val commonFor1And2Option = "common-1-2"
        val commonFor1And3Option = "common-1-3"
        val pack1Id = "pack-1"
        val pack2Id = "pack-2"
        val pack3Id = "pack-3"

        val options = mapOf(
            pack1Id to listOf(
                newDeliveryOption(packId = pack1Id, optionId = commonFor1And2Option),
                newDeliveryOption(packId = pack1Id, optionId = commonFor1And3Option)
            ),
            pack2Id to listOf(
                newDeliveryOption(packId = pack2Id, optionId = commonFor1And2Option)
            ),
            pack3Id to listOf(
                newDeliveryOption(packId = pack3Id, optionId = commonFor1And3Option)
            )
        )

        val splits = splitCalculator.calculateSplits(options)

        assertThat(splits).containsOnly(listOf(pack1Id), listOf(pack2Id), listOf(pack3Id))
    }

    private fun newDeliveryOption(packId: String, optionId: String): DeliveryOption {
        return DeliveryOption(
            packId = packId,
            id = optionId,
            deliveryType = null,
            title = null,
            price = null,
            beginDate = null,
            endDate = null,
            paymentMethods = null,
            deliveryPoint = null,
            intervals = emptyList(),
            isClickAndCollect = false,
            deliveryServiceId = null,
            regionId = 42,
            isMarketBranded = false,
            isOnDemand = false,
            isOneHourInterval = false,
            onDemandInterval = null,
            deliveryOptionFeatures = emptyList(),
            deliveryLiftingOptions = null,
            deliveryCustomizers = emptyList(),
            isTryingAvailable = false,
            extraCharge = null,
            undefinedDeliveryConfig = null,
        )
    }

}
