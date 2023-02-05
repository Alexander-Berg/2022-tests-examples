package ru.yandex.market.clean.data.model.dto.cms.garson

import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import ru.yandex.market.testcase.JavaSerializationTestCase

@RunWith(Enclosed::class)
class PictureDtoTest {

    class JavaSerializationTest : JavaSerializationTestCase() {

        override fun getInstance() = pictureDtoTestInstance()
    }
}