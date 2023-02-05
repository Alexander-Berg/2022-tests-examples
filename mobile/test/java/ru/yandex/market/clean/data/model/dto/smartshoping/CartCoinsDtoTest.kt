package ru.yandex.market.clean.data.model.dto.smartshoping

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class CartCoinsDtoTest {

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance: Any
            get() {
                val coinRestrictionDTO = CoinRestrictionDto.builder()
                    .categoryId(0)
                    .skuId("string")
                    .restrictionType("CATEGORY")
                    .build()

                val coinDto = CoinDto(
                    title = "Большая скидка в 100%",
                    subtitle = "на любой заказ",
                    coinType = "FIXED",
                    nominal = 0.0,
                    description = "string",
                    endDate = "20-02-2017 15:40:00",
                    images = CoinImagesDto.testInstance,
                    backgroundColor = "#4ca64c",
                    coinRestrictions = mutableListOf(coinRestrictionDTO)
                )

                val applicableCoins = listOf(coinDto.copy(id = "0"))

                val disabledCoinDto = DisabledCoinDto("DROPSHIP_RESTRICTION", coinDto.copy(id = "1"))
                val disabledCoins = listOf(disabledCoinDto)

                return CartCoinsDto(
                    applicableCoins = applicableCoins,
                    disabledCoins = disabledCoins
                )
            }

        override val type = CartCoinsDto::class.java

        override val jsonSource = text(
            """
                {
                  "applicableCoins": [
                    {
                      "id": "0",
                      "title": "Большая скидка в 100%",
                      "subtitle": "на любой заказ",
                      "coinType": "FIXED",
                      "nominal": 0.0,
                      "description": "string",
                      "endDate": "20-02-2017 15:40:00",
                      "images": {
                        "standard": "standard",
                        "alt": "alt"
                      },
                      "backgroundColor": "#4ca64c",
                      "coinRestrictions": [
                        {
                          "categoryId": 0,
                          "skuId": "string",
                          "restrictionType": "CATEGORY"
                        }
                      ]
                    }
                  ],
                  "disabledCoins": [
                    {
                      "reason": "DROPSHIP_RESTRICTION",
                      "coin": {
                        "id": "1",
                        "title": "Большая скидка в 100%",
                        "subtitle": "на любой заказ",
                        "coinType": "FIXED",
                        "nominal": 0.0,
                        "description": "string",
                        "endDate": "20-02-2017 15:40:00",
                        "images": {
                          "standard": "standard",
                          "alt": "alt"
                        },
                        "backgroundColor": "#4ca64c",
                        "coinRestrictions": [
                          {
                            "categoryId": 0,
                            "skuId": "string",
                            "restrictionType": "CATEGORY"
                          }
                        ]
                      }
                    }
                  ]
                }                
            """.trimIndent()
        )
    }

    class JavaSerializationTest : JavaSerializationTestCase() {

        override fun getInstance() = cartCoinsDtoTestInstance()
    }
}