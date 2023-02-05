package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.fapi.dto.cart.CartDto
import ru.yandex.market.clean.data.fapi.dto.cart.cartDtoTestInstance
import ru.yandex.market.utils.PackLabelUtils

@RunWith(Parameterized::class)
class CartCombinePackIdMapperTest(
    private val carts: List<CartDto>,
    private val expectedResult: List<String>,
) {

    private val packLabelUtils = mock<PackLabelUtils>() {
        on { generateLabel() } doReturn GENERATED_LABEL
    }
    private val mapper = CartCombinePackIdMapper(packLabelUtils = packLabelUtils)

    @Test
    fun `Check cart combine packId mapper`() {
        val resultLabels = mutableListOf<String>()
        carts.forEachIndexed { index, _ ->
            resultLabels.add(mapper.mapLabel(carts, index))
        }

        assertThat(resultLabels).isEqualTo(expectedResult)
    }

    companion object {

        private const val GENERATED_LABEL = "100"

        private val CART_WITH_ID_10 = cartDtoTestInstance(warehouseId = "10")
        private val CART_WITH_ID_20 = cartDtoTestInstance(warehouseId = "20")
        private val CART_WITH_ID_30 = cartDtoTestInstance(warehouseId = "30")
        private val CART_WITH_ID_40 = cartDtoTestInstance(warehouseId = "40")

        private val CARTS_0 = listOf(CART_WITH_ID_10)
        private val CARTS_1 = listOf(
            CART_WITH_ID_10,
            CART_WITH_ID_10,
        )
        private val CARTS_2 = listOf(
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            CART_WITH_ID_10,
        )
        private val CARTS_3 = listOf(
            CART_WITH_ID_10,
            CART_WITH_ID_20,
            CART_WITH_ID_20,
            CART_WITH_ID_20,
        )
        private val CARTS_4 = listOf(
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            CART_WITH_ID_20,
            CART_WITH_ID_20,
            CART_WITH_ID_20,
            CART_WITH_ID_20,
        )
        private val CARTS_5 = listOf(
            CART_WITH_ID_10,
            CART_WITH_ID_20,
            CART_WITH_ID_30,
            CART_WITH_ID_40,
        )
        private val CARTS_6 = listOf(
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            CART_WITH_ID_20,
            CART_WITH_ID_20,
            CART_WITH_ID_30,
            CART_WITH_ID_30,
            CART_WITH_ID_40,
        )
        private val CARTS_7 = listOf(
            CART_WITH_ID_10,
            CART_WITH_ID_10,
            cartDtoTestInstance(warehouseId = null),
        )

        private val LABELS_0 = listOf("10")
        private val LABELS_1 = listOf("10_0", "10_1")
        private val LABELS_2 = listOf("10_0", "10_1", "10_2", "10_3", "10_4", "10_5")
        private val LABELS_3 = listOf("10", "20_0", "20_1", "20_2")
        private val LABELS_4 = listOf("10_0", "10_1", "10_2", "10_3", "20_0", "20_1", "20_2", "20_3")
        private val LABELS_5 = listOf("10", "20", "30", "40")
        private val LABELS_6 = listOf("10_0", "10_1", "20_0", "20_1", "30_0", "30_1", "40")
        private val LABELS_7 = listOf("10_0", "10_1", GENERATED_LABEL)

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun parameters() = listOf(

            // 0: "10" -> "10"
            arrayOf(
                CARTS_0,
                LABELS_0
            ),

            // 1: "10", "10" -> "10_0", "10_1"
            arrayOf(
                CARTS_1,
                LABELS_1
            ),

            // 2: "10", "10", "10", "10", "10", "10" -> "10_0", "10_1", "10_2", "10_3", "10_4", "10_5"
            arrayOf(
                CARTS_2,
                LABELS_2
            ),

            // 3: "10", "20", "20", "20" -> "10", "20_0", "20_1", "20_2"
            arrayOf(
                CARTS_3,
                LABELS_3
            ),

            // 4: "10", "10", "10", "10", "20", "20", "20", "20" ->
            // "10_0", "10_1", "10_2", "10_3", "20_0", "20_1", "20_2", "20_3"
            arrayOf(
                CARTS_4,
                LABELS_4
            ),

            // 5: "10", "20", "30", "40" -> "10", "20", "30", "40"
            arrayOf(
                CARTS_5,
                LABELS_5
            ),

            // 6: "10", "10", "20", "20", "30", "30", "40" -> "10_0", "10_1", "20_0", "20_1", "30_0", "30_1", "40"
            arrayOf(
                CARTS_6,
                LABELS_6
            ),

            // 7: "10", "10", "null" -> "10_0", "10_1", "GENERATED_LABEL"
            arrayOf(
                CARTS_7,
                LABELS_7
            ),

            )

    }
}