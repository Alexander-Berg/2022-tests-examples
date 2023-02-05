package ru.yandex.market.clean.data.fapi.dto

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase

@RunWith(Enclosed::class)
class FrontApiMergedSkuDtoTest {

    class JavaSerializationTest : JavaSerializationTestCase() {

        override fun getInstance() = frontApiMergedSkuDtoTestInstance()
    }
}