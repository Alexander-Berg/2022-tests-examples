package ru.yandex.vertis.passport.api.v2.service.device

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.api.{MockedBackend, RootedSpecBase}
import ru.yandex.vertis.passport.api.v2.V2Spec
import akka.http.scaladsl.model.StatusCodes._
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.vertis.passport.test.ModelGenerators
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.api.NoTvmAuthorization

/**
  *
  * @author zvez
  */
class DeviceHandlerSpec extends WordSpec with RootedSpecBase with MockedBackend with V2Spec with NoTvmAuthorization {

  val base = "/api/2.x/auto/device"

  "get device uid" should {
    "return it" in {
      val deviceUid = ModelGenerators.deviceUid.next
      when(deviceUidService.validateAndGenerate(?, ?)(?)).thenReturn(deviceUid)

      Get(s"$base/uid") ~>
        commonHeaders ~>
        route ~>
        check {
          status shouldBe OK
          contentType shouldBe expectedContentType
          val parsedResponse = responseAs[ApiModel.DeviceUidResult]
          parsedResponse.getDeviceUid shouldBe deviceUid
        }
    }
  }

}
