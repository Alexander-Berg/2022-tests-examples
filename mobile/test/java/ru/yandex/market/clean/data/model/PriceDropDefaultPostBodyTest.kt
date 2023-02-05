package ru.yandex.market.clean.data.model

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.clean.data.model.dto.CartItemSnapshotDto
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class PriceDropDefaultPostBodyTest {

    class JsonSerializationTest : JsonSerializationTestCase() {
        override val instance: PriceDropDefaultPostBody
            get() {
                return PriceDropDefaultPostBody(
                    listOf(
                        CartItemSnapshotDto(
                            persistentOfferId = "oid",
                            stockKeepingUnitId = "sid",
                            modelId = "mid",
                            categoryId = "cid",
                            count = 3,
                            isPriceDropPromoEnabled = false
                        )
                    )
                )
            }

        override val type = PriceDropDefaultPostBody::class.java

        override val jsonSource = text(
            """
                {
                    "cartSnapshot": [
                        {
                            "wareMd5": "oid",
                            "sku": "sid",
                            "modelId": "mid",
                            "hid": "cid",
                            "count": 3,
                            "pricedropPromoEnabled": false
                        }
                    ]
                }
            """.trimIndent()
        )
    }
}