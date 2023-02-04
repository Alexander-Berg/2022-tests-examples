package vertis.pushnoy.api.v2.device

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.matchers.should.Matchers._
import vertis.pushnoy.MockedApiSuite
import ru.yandex.pushnoy.PushRequestModel.PushRequest
import vertis.pushnoy.api.ApiMarshallers
import vertis.pushnoy.model.template.SeveralDaysInactivityTemplate
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import spray.json._

/** Created by Karpenko Maksim (knkmx@yandex-team.ru) on 04/05/2018.
  */
class DeviceHandlerTest extends MockedApiSuite with ProtobufSupport with ApiMarshallers {

  test("send push") {
    val pushRequest = PushRequest.newBuilder().build()
    Post(
      "/v2/auto/device/testDevice/send?deliver_from=2018-03-09T10%3A00%3A00&deliver_to=2018-08-09T10%3A00%3A00",
      pushRequest
    ) ~> root.route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("unsupported device application") {
    val template = SeveralDaysInactivityTemplate("mark", "model", None, None)

    Post("/v2/auto/device/testDevice/template/severalDaysInactivity/send", template.toJson) ~> root.route ~> check {
      status shouldBe StatusCodes.Forbidden
    }
  }
}
