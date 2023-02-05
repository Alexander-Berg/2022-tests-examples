package ru.yandex.market.data.order

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class BuyerDtoTest {

    class JavaSerializationTest : JavaSerializationTestCase() {
        override fun getInstance() = buyerDtoTestInstance(
            firstName = "first-name",
            lastName = "last-name",
            middleName = "middle-name"
        )
    }

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = buyerDtoTestInstance(
            firstName = "first-name",
            lastName = "last-name",
            middleName = "middle-name"
        )

        override val type = BuyerDto::class.java

        override val jsonSource = file("BuyerDTO.json")
    }
}