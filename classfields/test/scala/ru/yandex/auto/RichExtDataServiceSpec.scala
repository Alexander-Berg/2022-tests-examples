package ru.yandex.auto

import org.junit.runner.RunWith

import java.io.{ByteArrayInputStream, InputStream}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsNull, JsNumber, JsValue, Json}
import ru.yandex.auto.extdata.service.util.EDSUtils.RichExtDataService
import ru.yandex.extdata.core.Data.StreamingData
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.extdata.core.{DataType, Instance}

import scala.util.{Success, Try}

@RunWith(classOf[JUnitRunner])
class RichExtDataServiceSpec extends WordSpecLike with Matchers {

  private val dtDummy = DataType("dummy")
  private def stream(jsValue: JsValue): InputStream = new ByteArrayInputStream(jsValue.toString().getBytes)

  private def createInstance(jsValue: JsValue): RichExtDataService = {
    val instance = Instance(null, StreamingData(stream(jsValue)))
    val eds: ExtDataService = new TestExtDataService(Success(instance))
    new RichExtDataService(eds)
  }

  "read Json from ReachExtDataService" in {
    createInstance(JsNull).readJson(dtDummy) shouldBe Success(JsNull)
    createInstance(Json.arr()).readJson(dtDummy) shouldBe Success(Json.arr())
    createInstance(Json.arr(1, 2, 3)).readJson(dtDummy) shouldBe Success(Json.arr(1, 2, 3))
  }

  private def parse(jsValue: JsValue): Try[Seq[Int]] = {
    createInstance(jsValue)
      .readJson(dtDummy)
      .map(_.as[Seq[JsNumber]].map(_.value.toInt))
  }

  "parse from reader" in {
    parse(Json.arr(1, 2, 3)) shouldBe Success(Seq(1, 2, 3))
    parse(Json.arr()) shouldBe Success(Seq())

    parse(Json.arr("1")).isSuccess shouldBe false
    parse(JsNull).isSuccess shouldBe false
  }
}
