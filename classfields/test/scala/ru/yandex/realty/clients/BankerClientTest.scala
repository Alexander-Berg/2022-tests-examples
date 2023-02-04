package ru.yandex.realty.clients

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.clients.banker.BankerClientImpl
import ru.yandex.realty.http.{HttpEndpoint, RemoteHttpService, TestHttpClient}
import ru.yandex.vertis.banker.model.ApiModel.{Account, AccountInfo}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.duration._

/**
  * Created by Vsevolod Levin on 13.02.2018.
  */
class BankerClientTest extends FlatSpec with Matchers with ScalaFutures with TestHttpClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val traced: Traced = Traced.empty

  val tout: PatienceConfiguration.Timeout = timeout(2.seconds)
  val int: PatienceConfiguration.Interval = interval(500.milliseconds)

  val from: DateTime = ISODateTimeFormat.date().parseDateTime("2017-08-01")
  val to: DateTime = ISODateTimeFormat.date().parseDateTime("2017-12-31")
  val uid = "4007216463"
  val accountId = "4007216463_1500481824206"

  private val correctAccount =
    Account
      .newBuilder()
      .setId(accountId)
      .setUser(uid)
      .setProperties(
        Account.Properties
          .newBuilder()
          .setEmail("marbyaa1@yandex.ru")
      )
      .build

  private val correctAccountInfo =
    AccountInfo
      .newBuilder()
      .setTotalIncome(1608700)
      .setTotalSpent(1589195)
      .setOverdraft(0)
      .setBalance(19505)
      .build()

  val bankerClient = new BankerClientImpl(
    new RemoteHttpService(
      "banker-client-test",
      HttpEndpoint("banker-api-http-api.vrts-slb.test.vertis.yandex.net", 80),
      testClient
    )
  )

  "BankerClient" should "correctly get account when no vertis user is specified" in {
    whenReady(bankerClient.getAccount(uid, None), tout, int) { result =>
      result should matchPattern {
        case Some(`correctAccount`) =>
      }
    }
  }

  "BankerClient" should "get no account when no bad uid is given" in {
    whenReady(bankerClient.getAccount("12345", None), tout, int) { result =>
      result should matchPattern {
        case None =>
      }
    }
  }

  "BankerClient" should "correctly get account when correct vertis user is specified" in {
    whenReady(bankerClient.getAccount(uid, Some(uid)), tout, int) { result =>
      result should matchPattern {
        case Some(`correctAccount`) =>
      }
    }
  }

  "BankerClient" should "throw when incorrect vertis user is specified" in {
    bankerClient.getAccount(uid, Some("123456789")).failed.futureValue shouldBe an[IllegalArgumentException]
  }

}
