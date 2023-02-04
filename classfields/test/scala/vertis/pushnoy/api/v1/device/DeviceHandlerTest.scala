package vertis.pushnoy.api.v1.device

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.matchers.should.Matchers._
import vertis.pushnoy.MockedApiSuite
import vertis.pushnoy.model.request.enums.Platform
import vertis.pushnoy.model.request.{DeviceInfo, PushMessageV1, TokenInfo}
import vertis.pushnoy.model.response.PushHistoryResponse
import spray.json.JsObject
import vertis.pushnoy.dao.TestDao

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 12/07/2017.
  */
class DeviceHandlerTest extends MockedApiSuite {

  test("add token information") {
    Post("/v1/auto/device/testDevice/token", TestDao.tokenInfo) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("get token information") {
    val tokenInfo = TokenInfo("testToken", Platform.APNS, "pushToken")
    Get("/v1/auto/device/deviceId/token") ~> root.route ~> check {
      responseAs[TokenInfo] shouldBe tokenInfo
    }
  }

  test("add device information") {
    Post("/v1/auto/device/testDevice", TestDao.deviceInfo) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("get device information") {
    Get("/v1/auto/device/deviceId") ~> root.route ~> check {
      responseAs[DeviceInfo] shouldBe TestDao.deviceInfo
    }
  }

  test("send push to device") {
    val pushMessage = PushMessageV1(JsObject.empty, "test", None, None, None, None)
    Post("/v1/auto/device/testDevice/push", pushMessage) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
    Post("/v1/auto/device/testDevice/push/test", pushMessage) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("get push history") {
    Get("/v1/auto/device/deviceId/push/testPush/history") ~> root.route ~> check {
      responseAs[PushHistoryResponse] shouldBe TestDao.pushHistory
    }
  }

  test("delete push history") {
    Delete("/v1/auto/device/deviceId/push/testPush/history") ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
