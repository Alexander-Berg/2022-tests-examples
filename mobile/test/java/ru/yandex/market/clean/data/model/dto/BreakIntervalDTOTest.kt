package ru.yandex.market.clean.data.model.dto

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class BreakIntervalDTOTest {

    class JavaSerializationTest : JavaSerializationTestCase() {
        override fun getInstance() = BreakIntervalDto.testBuilder().build()
    }

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = BreakIntervalDto.testBuilder().build()

        override val type = BreakIntervalDto::class.java

        override val jsonSource = file("BreakIntervalDTO.json")
    }
}