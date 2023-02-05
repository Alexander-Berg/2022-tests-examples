package ru.yandex.market.clean.data.model.dto

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class CancellationRuleDTOTest {

    class JavaSerializationTest : JavaSerializationTestCase() {
        override fun getInstance() = CancellationRuleDto.testBuilder().build()
    }

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = CancellationRuleDto.testBuilder().build()

        override val type = CancellationRuleDto::class.java

        override val jsonSource = file("CancellationRuleDTO.json")
    }
}