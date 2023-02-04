package ru.yandex.realty.clients.tinkoff.tinkoff.eacq

import akka.http.scaladsl.model.HttpMethods.POST
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.realty.clients.tinkoff.eacq.init.InitRequest
import ru.yandex.realty.clients.tinkoff.eacq.{DefaultTinkoffEACQClient, TinkoffEACQCredentials, TinkoffTokenHelper}
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}

@RunWith(classOf[JUnitRunner])
class DefaultTinkoffEACQClientSpec
  extends SpecBase
  with AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with HttpClientMock {

  private val client =
    new DefaultTinkoffEACQClient(httpService, new TinkoffEACQCredentials("1321054611234DEMO", "Dfsfh56dgKl"))

  "DefaultTinkoffEACQClient" should {
    "correctly calculate token" in {
      val json = Json.obj(
        "Amount" -> "100000",
        "OrderId" -> "TokenExample",
        "Description" -> "test",
        "DATA" -> Json.obj(
          "Phone" -> "+71234567890",
          "Email" -> "a@test.com"
        )
      )
      val expectedToken = "48d4ca825aab2ede06736d3eae099bd56ac97bd1bcdd598aff210f729de4eb21"
      TinkoffTokenHelper.makeToken(json, "TinkoffBankTest", "TinkoffBankTest") shouldEqual (expectedToken)
    }

    "init transaction successfully" in {
      val req = InitRequest(
        Amount = "140000",
        OrderId = "21051",
        IP = None,
        Description = "Подарочная карта на 1000 рублей",
        Currency = None,
        CustomerKey = None,
        Recurrent = None,
        PayType = None,
        Language = None,
        DATA = Map(
          "Email" -> "a@test.com",
          "Phone" -> "+71234567890"
        ),
        Receipt = None,
        NotificationURL = None,
        SuccessURL = None,
        FailURL = None,
        RedirectDueDate = Some(new DateTime("2021-02-28T10:59:13+03:00"))
      )
      httpClient.expect(POST, "/v2/Init")
      httpClient.expectJson(
        Json.stringify(
          Json.parse(
            s"""
               |{
               |  "Description":"Подарочная карта на 1000 рублей",
               |  "RedirectDueDate":"2021-02-28T10:59:13+03:00",
               |  "DATA":{"Email":"a@test.com","Phone":"+71234567890"},
               |  "Amount":"140000",
               |  "OrderId":"21051",
               |  "TerminalKey":"1321054611234DEMO",
               |  "Token":"d6f202160e1761313ad33d84a036623e259f92c60d5352e26b2b3997f04e7cfe"
               |}
               |""".stripMargin
          )
        )
      )
      httpClient.respondWith(
        s"""
           |{
           |  "Success" : true,
           |  "ErrorCode" : "0",
           |  "TerminalKey" : "1321054611234DEMO",
           |  "Status" : "NEW",
           |  "PaymentId": "13660",
           |  "OrderId" : "21051",
           |  "Amount" : 140000,
           |  "PaymentURL" : "https://securepay.tinkoff.ru/rest/Authorize/1B63Y1"
           |}
           |""".stripMargin
      )
      val trace: Traced = Traced.empty
      val resp = client.init(req)(trace).futureValue
      resp.Success shouldEqual (true)
      resp.Status shouldEqual (Some("NEW"))
      resp.PaymentURL shouldEqual (Some("https://securepay.tinkoff.ru/rest/Authorize/1B63Y1"))
      resp.PaymentId shouldEqual (Some("13660"))
    }
  }
}
