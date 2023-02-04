package ru.yandex.vos2.autoru.api.utils

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{Json, OFormat}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vos2.autoru.api.utils.CommonFeaturesToggleHandlerTest._
import ru.yandex.vos2.autoru.utils.Vos2ApiSuite
import ru.yandex.vos2.autoru.utils.Vos2ApiHandlerResponses._
import ru.yandex.vos2.commonfeatures.VosFeatureTypes._

@RunWith(classOf[JUnitRunner])
class CommonFeaturesToggleHandlerTest extends AnyFunSuite with Vos2ApiSuite with ScalaFutures {
  implicit val featureFormat: OFormat[TestFeature] = Json.format[TestFeature]

  components.featureRegistry.register("some_feature", true)
  components.featureRegistry.register("another_feature", WithGeneration(value = false))

  test("get features") {
    Get("/utils/common-features") ~> route ~> check {
      status shouldBe StatusCodes.OK
      val resp = Json.parse(responseAs[String]).as[List[TestFeature]]
      resp.exists(_.name == "some_feature") shouldBe true
      resp.exists(_.name == "another_feature") shouldBe true
    }
  }

  test("change feature's value") {
    Put("/utils/common-features/some_feature")
      .withEntity(
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, "false".getBytes("UTF-8"))
      ) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

    Put("/utils/common-features/another_feature")
      .withEntity(
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, "false.2".getBytes("UTF-8"))
      ) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

    whenReady(components.featureRegistry.getFeatures) { list =>
      list.collect {
        case elem if elem.name == "some_feature" =>
          elem.value.value shouldBe "false"
        case elem if elem.name == "another_feature" =>
          elem.value.value shouldBe "false.2"
      }
    }
  }

  test("wrong value format") {
    val req = Put("/utils/common-features/another_feature")
      .withEntity(
        HttpEntity(ContentTypes.`text/plain(UTF-8)`, "false2.2".getBytes("UTF-8"))
      )

    val errorDescription = "requirement failed: Cannot deserialize false2.2 as custom_vos_feature"

    checkErrorRequest(req, StatusCodes.BadRequest, wrongFeatureValueFormat(errorDescription))
  }
}

object CommonFeaturesToggleHandlerTest {

  case class TestFeature(value: String, name: String, `type`: String)

}
