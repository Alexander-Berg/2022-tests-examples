package ru.yandex.market.clean.data.model.dto

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class CartItemSnapshotDtoTest {

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance: CartItemSnapshotDto
            get() {
                return CartItemSnapshotDto(
                    persistentOfferId = "12345",
                    modelId = "model",
                    categoryId = "15",
                    count = 1,
                    stockKeepingUnitId = "sku42",
                    isPriceDropPromoEnabled = false
                )
            }

        override val type = CartItemSnapshotDto::class.java

        override val jsonSource: JsonSource
            get() {
                return text(
                    """
                    {
                      "wareMd5": "12345",
                      "modelId": "model",
                      "hid": "15",
                      "count": 1,
                      "sku": "sku42",
                      "pricedropPromoEnabled": false
                    }
                """.trimIndent()
                )
            }
    }

    class JavaSerializationTest : JavaSerializationTestCase() {

        override fun getInstance() = cartItemSnapshotDtoTestInstance()
    }
}