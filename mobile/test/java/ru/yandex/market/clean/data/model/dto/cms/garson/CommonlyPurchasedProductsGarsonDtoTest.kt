package ru.yandex.market.clean.data.model.dto.cms.garson

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class CommonlyPurchasedProductsGarsonDtoTest {

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = CommonlyPurchasedProductsGarsonDto.create(
            GarsonTypeDto.COMMONLY_PURCHASED_PRODUCTS,
            CommonlyPurchasedProductsGarsonDto.Params.create("user")
        )

        override val type = CommonlyPurchasedProductsGarsonDto::class.java

        override val jsonSource = text(
            """
                {
                    "id": "CommonlyPurchasedProducts",
                    "params": {
                        "personalization": "user"
                    }
                }
            """.trimIndent()
        )
    }

    class JavaSerializationTest : JavaSerializationTestCase() {
        override fun getInstance() = CommonlyPurchasedProductsGarsonDto.testInstance()
    }
}