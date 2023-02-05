package ru.yandex.market.clean.data.model.dto.smartshoping

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.clean.data.model.dto.smartshoping.CoinRestrictionDto.Companion.builder
import ru.yandex.market.clean.data.model.dto.smartshoping.UserCoinsDto.Companion.testBuilder
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase
import java.lang.reflect.Type

@RunWith(Enclosed::class)
class UserCoinsDTOTest {
    class JsonSerializationTest : JsonSerializationTestCase() {
        override val instance: Any
            get() {
                val coinImagesDTO = CoinImagesDto(
                    standard = "https://avatars.mdst.yandex.net/get-smart_shopping/1823/2080eff0-2a8e-4679-926c-032dfb3b29b8/",
                    alt = null
                )
                val coinRestrictionDTO = builder()
                    .categoryId(0)
                    .skuId("string")
                    .restrictionType("CATEGORY")
                    .build()
                val coinRestrictions = listOf(
                    coinRestrictionDTO
                )
                val coinDTO = CoinDto(
                    title = "Скидка  на 500 рублей",
                    subtitle = "На заказ от 1000 рублей ",
                    coinType = "FIXED",
                    nominal = 500.0,
                    description = "Скидка 500 рублей действует на все заказы от 1000 рублей ",
                    images = coinImagesDTO,
                    id = "10067",
                    creationDate = "29-09-2018 18:46:24",
                    endDate = "29-09-2018 18:46:24",
                    status = "ACTIVE",
                    coinRestrictions = coinRestrictions
                )
                val coins = listOf(coinDTO)
                val futureCoinDTO = FutureCoinDto(
                    title = "Скидка на 100 рублей",
                    subtitle = "на все заказы",
                    coinType = "FIXED",
                    nominal = 100.0,
                    description = "Скидку 100 рублей можно применить на любой заказ. ",
                    images = coinImagesDTO,
                    promoId = 10319L
                )
                val futureCoins = listOf(futureCoinDTO)
                return testBuilder()
                    .coins(coins)
                    .futureCoins(futureCoins)
                    .build()
            }
        override val type: Type
            get() = UserCoinsDto::class.java
        override val jsonSource: JsonSource
            get() = file("UserCoinsDTO.json")
    }

    class JavaSerializationTest : JavaSerializationTestCase() {
        override fun getInstance(): Any {
            return testBuilder().build()
        }
    }
}