package ru.yandex.market.data.order

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class OrderHistoryItemDTOTest {

    class JavaSerializationTest : JavaSerializationTestCase() {
        override fun getInstance() = OrderHistoryItemDto.testBuilder().build()
    }

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = OrderHistoryItemDto.testBuilder().build()

        override val type = OrderHistoryItemDto::class.java

        override val jsonSource = file("OrderHistoryItemDTO.json")
    }
}