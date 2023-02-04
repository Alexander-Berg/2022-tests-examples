package vertis.pushnoy.api.v1.user

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.matchers.should.Matchers._
import vertis.pushnoy.MockedApiSuite
import vertis.pushnoy.model.request.enums.Platform
import vertis.pushnoy.model.request.{PushMessageV1, TokenInfo}
import vertis.pushnoy.model.DeviceFullInfo
import ru.yandex.vertis.generators.ProducerProvider
import spray.json.JsObject
import vertis.pushnoy.dao.TestDao

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 12/07/2017.
  */
class UserHandlerTest extends MockedApiSuite with ProducerProvider {

  test("send push to user") {
    val pushMessage = PushMessageV1(JsObject.empty, "test", None, None, None, None)

    Post("/v1/auto/user/testUser/push", pushMessage) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
    Post("/v1/auto/user/testUser/push/test", pushMessage) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
    Post("/v1/auto/user/testUser/push?target=websocket", pushMessage) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
    Post("/v1/auto/user/testUser/push?target=devices", pushMessage) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
    Post("/v1/auto/user/testUser/push/test?target=websocket", pushMessage) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("post device to user") {
    val tokenInfo = TokenInfo("testToken", Platform.APNS, "pushToken")
    Post("/v1/auto/user/testUser/device/testDevice", tokenInfo) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("detach device from user") {
    val tokenInfo = TokenInfo("testToken", Platform.APNS, "pushToken")
    Delete("/v1/auto/user/testUser/device/testDevice", tokenInfo) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("get user devices") {
    Get("/v1/auto/user/userId") ~> root.route ~> check {
      responseAs[Seq[DeviceFullInfo]] shouldBe List(TestDao.deviceFullInfo)
    }
  }

  test("get secret sign") {
    Get("/v1/auto/user/testUser/sign") ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

}
