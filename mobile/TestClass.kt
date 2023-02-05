package ru.yandex.market.uikitapp

import ru.yandex.market.processor.testinstance.GenerateTestInstance
import ru.yandex.market.processor.testinstance.TestFloat
import ru.yandex.market.processor.testinstance.TestLong
import ru.yandex.market.processor.testinstance.TestString

class TestTestClass {

    class TestClass {

        @GenerateTestInstance
        data class Entry(
            @TestString("test") val value: String,
            val orderIds: Map<String, List<OrderId>>,
            @TestFloat(0.5f) val f1: Float?,
            @TestLong(1310L) val l1: Long?,
            val f2: Float?,
            val i: Int?
        )
    }
}

@GenerateTestInstance
data class OrderId(
    val id: Long,
    val items: Set<String>,
    val orders: List<OrderId>,
    val anotherOrder: OrderId?,
    val ordersMap: Map<String, OrderId>
)