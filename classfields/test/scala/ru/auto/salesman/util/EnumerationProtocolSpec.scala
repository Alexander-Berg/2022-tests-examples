package ru.auto.salesman.util

import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.test.BaseSpec
import spray.json.JsString

class EnumerationProtocolSpec extends BaseSpec with EnumerationProtocol {

  "javaEnumJsonFormat()" should {

    val format = javaEnumJsonFormat(Section.valueOf)

    "read JsString" in {
      format.read(JsString("USED")) shouldBe Section.USED
      format.read(JsString("NEW")) shouldBe Section.NEW
    }

    "throw on wrong JsString" in {
      an[IllegalArgumentException] should be thrownBy format.read(
        JsString("WRONG")
      )
    }

    "write JsString" in {
      format.write(Section.USED) shouldBe JsString("USED")
      format.write(Section.NEW) shouldBe JsString("NEW")
    }
  }

  "lowerCaseJavaEnumJsonFormat()" should {

    val format = lowerCaseJavaEnumJsonFormat(Section.valueOf)

    "read JsString" in {
      format.read(JsString("used")) shouldBe Section.USED
      format.read(JsString("new")) shouldBe Section.NEW
    }

    "throw on wrong JsString" in {
      an[IllegalArgumentException] should be thrownBy format.read(
        JsString("WRONG")
      )
    }

    "write JsString" in {
      format.write(Section.USED) shouldBe JsString("used")
      format.write(Section.NEW) shouldBe JsString("new")
    }
  }
}
