package ru.yandex.realty.clients.sms

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import akka.http.scaladsl.model.HttpMethods
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.sms.DefaultSmsSendClientSpec._
import ru.yandex.realty.http.{HttpClientMock, RequestAware}

@RunWith(classOf[JUnitRunner])
class DefaultSmsSendClientSpec extends AsyncSpecBase with RequestAware with HttpClientMock {

  private val client = new DefaultSmsSendClient(httpService, "unit-test")

  private def encode(s: String) =
    URLEncoder.encode(s, StandardCharsets.UTF_8.name())

  "DefaultSmsSendClient" should {
    "pass all parameters" in {
      httpClient.expect(
        HttpMethods.POST,
        s"/sendsms?sender=$Sender&route=realty&phone=${encode(Phone)}&text=${encode(Text)}"
      )
      httpClient.expectHeader("X-Application", "unit-test")
      httpClient.expectHeader("X-Request-ID", "")
      httpClient.expectHeader("X-Template", "api")
      httpClient.respondWith(SuccessResponse)

      client.sendSms(Phone, Text, SmsSendClient.Templates.Api, Sender).futureValue
    }
    "handle correct error response" in {
      httpClient.respondWith(FailureResponse)
      interceptCause[SmsSendException] {
        client.sendSms(Phone, Text, SmsSendClient.Templates.Promocode).futureValue
      }
    }

    "handle unexpected response" in {
      httpClient.respondWith(UnexpectedResponse)
      interceptCause[SmsSendException] {
        client.sendSms(Phone, Text, SmsSendClient.Templates.Promocode).futureValue
      }
    }
  }
}

object DefaultSmsSendClientSpec {
  val Phone = "+79991112233"
  val Text = "example text"
  val Sender = "sender"

  val SuccessResponse =
    """
          |<doc>
          |    <message-sent id="127000000003456" />
          |</doc>
    """.stripMargin

  val FailureResponse =
    """
          |<doc>
          |    <error>User does not have an active phone to recieve messages</error>
          |    <errorcode>NOCURRENT</errorcode>
          |</doc>
    """.stripMargin

  val UnexpectedResponse =
    """
          |<doc>
          |</doc>
    """.stripMargin
}
