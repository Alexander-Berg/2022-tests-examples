package ru.yandex.vertis.personal.api.settings.v1

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{FormData, StatusCodes}
import ru.yandex.vertis.personal.api.model.SuccessResponse
import ru.yandex.vertis.personal.api.settings.BaseSettingsVersionedHandlerSpec
import ru.yandex.vertis.personal.api.settings.v1.model.GetResponse
import ru.yandex.vertis.personal.model.settings.Settings
import ru.yandex.vertis.personal.model.UserRef
import spray.json.{DefaultJsonProtocol, JsValue}

/**
  * Specs on v1 settings HTTP API
  *
  * @author dimas
  */
class SettingsV1HandlerSpec extends BaseSettingsVersionedHandlerSpec with SprayJsonSupport with DefaultJsonProtocol {

  val user = UserRef("uid:123")
  val settings = Settings(user, Map("foo" -> "bar", "baz" -> "bee"))

  val backend = new SettingsV1Backend(registry)

  val route = sealRoute(new SettingsV1Handler(service, user, backend).routes)

  "SettingsV1Handler" should {

    "create user's settings" in {
      Put("/", FormData(settings.values)) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse(service, version, settings)
      }
    }

    "get users's settings" in {
      Get() ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse(service, version, settings)
      }
    }

    "delete user's settings" in {
      Delete() ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[JsValue] shouldBe SuccessResponse.InnerJson
      }
      Get() ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[GetResponse] shouldBe GetResponse(
          service,
          version,
          Settings.empty(user)
        )
      }
    }

  }

}
