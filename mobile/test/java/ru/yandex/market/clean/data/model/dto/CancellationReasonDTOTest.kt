package ru.yandex.market.clean.data.model.dto

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class CancellationReasonDTOTest {

    class JavaSerializationTest : JavaSerializationTestCase() {
        override fun getInstance() = CancellationReasonDto.testBuilder().build()
    }

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = CancellationReasonDto.testBuilder().build()

        override val type = CancellationReasonDto::class.java

        override val jsonSource = file("CancellationReasonDTO.json")
    }
}