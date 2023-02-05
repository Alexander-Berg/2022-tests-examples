package ru.yandex.market.clean.data.model.dto

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase
import ru.yandex.market.testcase.JsonSerializationTestCase

@RunWith(Enclosed::class)
class OpenHoursDTOTest {

    class JavaSerializationTest : JavaSerializationTestCase() {
        override fun getInstance() = OpenHoursDto.testBuilder().build()
    }

    class JsonSerializationTest : JsonSerializationTestCase() {

        override val instance = OpenHoursDto.testBuilder().build()

        override val type = OpenHoursDto::class.java

        override val jsonSource = file("OpenHoursDTO.json")
    }
}