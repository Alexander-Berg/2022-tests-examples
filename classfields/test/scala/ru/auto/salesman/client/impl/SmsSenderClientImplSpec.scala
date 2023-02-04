package ru.auto.salesman.client.impl

import ru.auto.salesman.Task
import ru.auto.salesman.client.sms.model.{
  PhoneDeliveryParams,
  SmsMessage,
  SmsSenderConfig
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.sttp.SttpClientImpl
import sttp.client3.impl.zio.RIOMonadAsyncError
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Response, SttpBackend}

class SmsSenderClientImplSpec extends BaseSpec {

  val sttpBackendClientStub: SttpBackend[Task, Any] =
    new SttpBackendStub[Task, Any](
      new RIOMonadAsyncError(),
      PartialFunction.empty,
      None
    ).whenRequestMatchesPartial({
      case r if r.uri.params.get("phone").contains("NotValidXml") =>
        Response.ok("Not valid response xml")
      case r if r.uri.params.get("phone").contains("ValidXMLSuccessResponse") =>
        Response.ok(
          """<?xml version="1.0" encoding="utf-8"?>
            <doc>
            <message-sent id="2070000549904146" />
            <gates ids="78" />
            </doc>"""
        )

      case r if r.uri.params.get("phone").contains("ValidEmptyXML") =>
        Response.ok(
          """<?xml version="1.0" encoding="utf-8"?>"""
        )
      case r if r.uri.params.get("phone").contains("ValidXMLError") =>
        Response.ok(
          """<?xml version="1.0" encoding="utf-8"?>
            <doc>
            <error>ereeeee</error>
            </doc>"""
        )
    })

  val sttpClient = new SttpClientImpl(sttpBackendClientStub)

  val smsSender = new SmsSenderClientImpl(
    host = "test_host",
    port = 1,
    config = SmsSenderConfig(
      sender = "test_sender",
      route = "test_route",
      application = "test_application"
    ),
    backend = sttpClient
  )

  "SmsSenderClientImpl" should {
    "success send sms to sms service and return valid response" in {
      smsSender
        .send(
          message = SmsMessage(
            templateId = None,
            smsText = "temp_text"
          ),
          params = PhoneDeliveryParams(
            phone = "ValidXMLSuccessResponse"
          )
        )
        .success
        .value shouldBe "2070000549904146"
    }

    "failed send sms. Sms service respond not valid xml" in {
      val failuresResponse = smsSender
        .send(
          message = SmsMessage(
            templateId = None,
            smsText = "temp_text"
          ),
          params = PhoneDeliveryParams(
            phone = "NotValidXml"
          )
        )
        .failure
        .cause
        .failures
      failuresResponse.size shouldBe 1
      failuresResponse.head.getMessage shouldBe "Unexpected response: Not valid response xml"
    }

    "failed send sms. Sms service respond valid empty xml" in {
      val failuresResponse = smsSender
        .send(
          message = SmsMessage(
            templateId = None,
            smsText = "temp_text"
          ),
          params = PhoneDeliveryParams(
            phone = "ValidEmptyXML"
          )
        )
        .failure
        .cause
        .failures
      failuresResponse.size shouldBe 1
      failuresResponse.head.getMessage shouldBe "Unexpected response: <?xml version=\"1.0\" encoding=\"utf-8\"?>"
    }

    "failed send sms. Sms service respond error" in {
      val failuresResponse = smsSender
        .send(
          message = SmsMessage(
            templateId = None,
            smsText = "temp_text"
          ),
          params = PhoneDeliveryParams(
            phone = "ValidXMLError"
          )
        )
        .failure
        .cause
        .failures
      failuresResponse.size shouldBe 1
      failuresResponse.head.getMessage shouldBe "Response error: ereeeee"
    }

  }
}
