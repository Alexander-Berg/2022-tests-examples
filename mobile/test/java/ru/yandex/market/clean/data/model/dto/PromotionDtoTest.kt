package ru.yandex.market.clean.data.model.dto

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase
import java.math.BigDecimal

@RunWith(Enclosed::class)
class PromotionDtoTest {

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = PromotionDto(
            type = "type",
            buyerDiscount = BigDecimal.ZERO,
            deliveryDiscount = BigDecimal.TEN,
            promoCode = "promoCode"
        )

        override val type = PromotionDto::class.java

        override val jsonSource = text(
            """
                {
                    "type": "type",
			        "buyerDiscount": 0,
			        "deliveryDiscount": 10,
                    "promoCode": "promoCode"
                }
            """.trimIndent()
        )
    }

    class JavaSerializationTest : JavaSerializationTestCase() {

        override fun getInstance() = promotionDtoTestInstance()
    }
}