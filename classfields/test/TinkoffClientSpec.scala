package ru.yandex.vertis.safe_deal.client.bank

import cats.syntax.option._
import com.softwaremill.tagging._
import ru.yandex.vertis.safe_deal.client.bank.TinkoffClient._
import ru.yandex.vertis.safe_deal.config.TinkoffClientConfig
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import zio.test._
import zio.test.Assertion._
import io.circe.parser.decode
import ru.yandex.vertis.safe_deal.client.bank.model._
import ru.yandex.vertis.safe_deal.model.{Phone, Tag}
import ru.yandex.vertis.zio_baker.util.EmptyString
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import zio.ZLayer
import zio.blocking.Blocking
import zio.test.environment.TestEnvironment

import java.time.LocalDate

object TinkoffClientSpec extends DefaultRunnableSpec {

  def spec: ZSpec[TestEnvironment, Any] =
    suite("TinkoffClient")(
      test("codecs") {
        val responseOperations =
          """
            |{
            |  "accountNumber": "99998888777766665555",
            |  "saldoIn": 500,
            |  "income": 500,
            |  "outcome": 500,
            |  "saldoOut": 500,
            |  "operation": [
            |    {
            |      "operationId": "12345_89765",
            |      "id": "1234567890123456789",
            |      "date": "2015-04-01",
            |      "amount": 500,
            |      "drawDate": "2015-05-01",
            |      "payerName": "Иванов Иван Иванович",
            |      "payerInn": "987654321987",
            |      "payerAccount": "99998888777766665555",
            |      "payerCorrAccount": "40244447777333300000",
            |      "payerBic": "76543277778",
            |      "payerBank": "банк 'Лидеров'",
            |      "chargeDate": "2015-09-03",
            |      "recipient": "Петров Петр Петрович",
            |      "recipientInn": "765432198765",
            |      "recipientAccount": "77774444222277772222",
            |      "recipientCorrAccount": "40299998888777700000",
            |      "recipientBic": "12345678901",
            |      "recipientBank": "банк 'Чемпионов'",
            |      "paymentType": "",
            |      "operationType": "01",
            |      "uin": "0",
            |      "paymentPurpose": "материальная помощь",
            |      "creatorStatus": "",
            |      "kbk": "44445555666677778888",
            |      "oktmo": "44445555",
            |      "taxEvidence": "ТП",
            |      "taxPeriod": "ГД.00.2019",
            |      "taxDocNumber": "0",
            |      "taxDocDate": "0",
            |      "taxType": "taxType",
            |      "executionOrder": "5"
            |    }
            |  ]
            |}
            |""".stripMargin

        assert(decode[OperationsResponse](responseOperations).isRight)(isTrue)
        val responseRunPersonCheck =
          """
            |{
            |    "correlationId": "cf99df08-0829-4614-8da3-0e440fd23fe0"
            |}
            |""".stripMargin
        assert(decode[RunPersonCheckResponse](responseRunPersonCheck).isRight)(isTrue)

        val responsePersonCheckStatus =
          """
            |{
            |    "status": "READY",
            |    "result": {
            |        "isInnCorrect": true,
            |        "isPassportCorrect": false
            |    }
            |}
            |""".stripMargin
        assert(decode[PersonCheckStatusResponse](responsePersonCheckStatus).isRight)(isTrue)
      },
      testM("codecs") {
        val httpClientConfig = HttpClientConfig(
          url = "https://business.tinkoff.ru/openapi",
          proxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128).some
        )
        HttpClient.blockingLayer.build
          .use { backend =>
            val tc = new TinkoffClientImpl(
              TinkoffClientConfig(
                httpClientConfig,
                httpClientConfig,
                "t.6Df3nj4yakX7EpVWCwnFKuWC1vrlEi4CqU_oOJIgVfIhgly7L243hS0scbG5JJPb2uIUXBMR_YgqV_oPbYhzug",
                EmptyString,
                EmptyString,
                EmptyString,
                EmptyString
              )
            )(backend.get)

            tc.runPersonCheck(
              "Максим".taggedWith[Tag.FirstName],
              "Петрович".taggedWith[Tag.MiddleName].some,
              "Давыдов".taggedWith[Tag.LastName],
              Phone("+79172940866"),
              Passport(
                LocalDate.of(1995, 6, 8),
                "г. Казань".taggedWith[Tag.BirthPlace],
                "РФ".taggedWith[Tag.Citizenship],
                "9216131388".taggedWith[Tag.SerialNumber],
                "УФМС РОССИИ ПО РЕСПУБЛИКЕ ТАТАРСТАН В МОСКОВСКОМ РАЙОНЕ Г. КАЗАНИ".taggedWith[Tag.UnitName],
                LocalDate.of(2016, 9, 22),
                "160-007".taggedWith[Tag.UnitCode],
                "г. Казань, улица Колотушкина, дом 4, квартира 78".taggedWith[Tag.Address]
              ),
              None
            ).map(println) *>
              tc.personCheckStatus("ca51321a-2aa3-4508-b429-e487101e6024".taggedWith[Tag.CorrelationId])
                .map(println)
                .as(assert(1)(equalTo(1)))
          }
          .orDie
          .provideLayer(Blocking.live ++ ZLayer.succeed(httpClientConfig))
      } @@ TestAspect.ignore
    )
}
