package ru.yandex.vertis.json.formats

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json._

/**
  * @author Natalia Ratskevich (reimai@yandex-team.ru)
  */
class ArrayFormatsTest extends WordSpec with Matchers {

  case class Test(test: Int)

  implicit val TestFormat = Json.format[Test]
  implicit val TestArrFormat = ArrayFormats.formatArr[Test]

  "array format" should {

    "work" in {
      val json = TestArrFormat.writes(Seq(Test(1), Test(2)))
      json shouldBe JsArray(Seq(JsObject(Seq("test" -> JsNumber(1))), JsObject(Seq("test" -> JsNumber(2)))))
      val seq = TestArrFormat.reads(json)
      seq shouldBe JsSuccess(Seq(Test(1), Test(2)))
    }

    "write an empty seq" in {
      val emptyJson = TestArrFormat.writes(Seq.empty[Test])
      emptyJson shouldBe JsArray()
    }

    "read an empty seq" in {
      val emptySeq = TestArrFormat.reads(JsArray())
      emptySeq shouldBe JsSuccess(Seq.empty[Test])
    }
  }
}
