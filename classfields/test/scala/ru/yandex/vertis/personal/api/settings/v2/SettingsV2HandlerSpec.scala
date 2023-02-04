package ru.yandex.vertis.personal.api.settings.v2

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{FormData, StatusCodes}
import ru.yandex.vertis.personal.api.model.SuccessResponse
import ru.yandex.vertis.personal.api.settings.BaseSettingsVersionedHandlerSpec
import ru.yandex.vertis.personal.api.settings.v2.model.SettingsV2Response
import ru.yandex.vertis.personal.model.UserRef
import ru.yandex.vertis.personal.model.settings.Settings
import spray.json.{DefaultJsonProtocol, JsValue}

class SettingsV2HandlerSpec extends BaseSettingsVersionedHandlerSpec with SprayJsonSupport with DefaultJsonProtocol {

  val user = UserRef("uid:321")
  val settings = Settings(user, Map("bar" -> "foo", "bee" -> "baz"))

  val expectedResponse = SettingsV2Response(
    Map(domain.toString -> settings.values)
  )

  val backend = new SettingsV2Backend(registry)

  val route = sealRoute(
    new SettingsV2Handler(settingsDomain, user, backend).routes
  )

  private def getResponse: SettingsV2Response = {
    Get() ~> route ~> check {
      status shouldBe StatusCodes.OK
      responseAs[SettingsV2Response]
    }
  }

  "SettingsV2Handler" should {
    "create user's settings" in {
      Put("/", FormData(settings.values)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[SettingsV2Response] shouldBe expectedResponse
      }
    }
    "get user's settings" in {
      getResponse shouldBe expectedResponse
    }
    "rewrite user's settings" in {
      val newSettings = Settings(user, Map("a" -> "b", "b" -> "c"))
      val newExpectedResponse =
        SettingsV2Response(Map(domain.toString -> newSettings.values))
      Post("/", FormData(newSettings.values)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue] shouldBe SuccessResponse.InnerJson
      }
      getResponse shouldBe newExpectedResponse
    }
    "delete user's settings" in {
      Delete() ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue] shouldBe SuccessResponse.InnerJson
      }
      getResponse match {
        case SettingsV2Response(items) if items.isEmpty =>
          ()
        case other =>
          fail(s"Unexpected $other")
      }
    }
  }

}
