package ru.yandex.market.clean.data.model.dto

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class CancellationRequestDTOTest {

    class JavaSerializationTest : JavaSerializationTestCase() {

        override fun getInstance() = CancellationRequestDto.testBuilder().build()
    }

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = CancellationRequestDto.testBuilder().build()

        override val type = CancellationRequestDto::class.java

        override val jsonSource = file("CancellationRequestDTO.json")
    }
}