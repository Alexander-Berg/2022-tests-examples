package ru.auto.salesman.client.impl

import akka.http.scaladsl.server.Directives._
import ru.auto.salesman.test.BaseSpec
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import ru.auto.salesman.Env
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.util.copypaste.ProtobufSupport
import ru.auto.salesman.util.sttp.SttpClientImpl
import ru.yandex.pushnoy.PushRequestModel.{
  PersonalDiscountTemplateRequest,
  SendPushTemplateRequest
}
import ru.yandex.pushnoy.PushResponseModel.ListPushSendResponse
import ru.yandex.vertis.ops.test.TestOperationalSupport
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.impl.zio.ExtendEnv
import sttp.model.StatusCode

class PushnoyClientV2ImplSpec extends BaseSpec with ProtobufSupport {

  private val personalDiscountTemplate = PersonalDiscountTemplateRequest
    .newBuilder()
    .setDeeplink("test deeplink")
    .setPushName("test name")
    .setText("test text")
    .setTitle("test title")
    .build

  private val template = SendPushTemplateRequest
    .newBuilder()
    .setPersonalDiscount(personalDiscountTemplate)
    .build
  private val user = "user:1234"
  private val deliverFrom = "2020-10-06T10:00:00.000"

  @volatile private var requestedDeliverFrom = Option.empty[String]

  private val response = ListPushSendResponse.newBuilder.build

  private val mockedPushnoyAddress = runServer(
    (path("api" / "v2" / "auto" / "user" / user / "template" / "send") & post &
      parameter("deliver_from")) { deliverFrom =>
      requestedDeliverFrom = Some(deliverFrom)
      complete(response)
    }
  )

  // Раньше здесь тоже использовался AsyncHttpClientZioBackend.stub, но он не
  // поймал сломанную бинарную совместимость между zio и sttp. Поэтому для
  // регресс-теста совместимости тест написан на реальном http-сервере.
  private val testingBackend = SttpClientImpl(TestOperationalSupport)

  val pushnoyClientV2Impl =
    new PushnoyClientV2Impl(mockedPushnoyAddress.toString, testingBackend)

  private val failingTestingBackend = AsyncHttpClientZioBackend.stub
    .whenRequestMatches(_.uri.params.get("delivery_name").contains("fail_name"))
    .thenRespondServerError()
    .whenRequestMatches(_.uri.params.get("delivery_name").contains("fail_name"))
    .thenRespondWithCode(StatusCode.BadRequest)
    .extendEnv[Env]

  val failingPushnoyClientV2Impl =
    new PushnoyClientV2Impl(
      "http://test",
      new SttpClientImpl(failingTestingBackend)
    )

  "PushnoyClientV2Impl" should {
    "ok" in {
      pushnoyClientV2Impl
        .sendPush(
          AutoruUser(user),
          DateTime.now(),
          DateTime.now().plusMinutes(2),
          "test_delivery_name",
          template
        )
        .success
    }

    "send date in right format" in {
      val fmt = ISODateTimeFormat.dateHourMinuteSecondMillis()
      pushnoyClientV2Impl
        .sendPush(
          AutoruUser(user),
          fmt.parseDateTime(deliverFrom),
          fmt.parseDateTime(deliverFrom).plusMinutes(2),
          "test_delivery_name",
          template
        )
        .success
      requestedDeliverFrom.value shouldBe deliverFrom
    }

    "return exception on error" in {
      failingPushnoyClientV2Impl
        .sendPush(
          AutoruUser(user),
          DateTime.now(),
          DateTime.now().plusMinutes(2),
          "server_error",
          template
        )
        .failure
    }

    "return exception on status 400" in {
      failingPushnoyClientV2Impl
        .sendPush(
          AutoruUser(user),
          DateTime.now(),
          DateTime.now().plusMinutes(2),
          "400_error",
          template
        )
        .failure
    }
  }

}
