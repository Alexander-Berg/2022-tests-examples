package ru.yandex.vertis.shark.client.saturn

import cats.implicits.catsSyntaxOptionId
import common.zio.features.testkit.FeaturesTest
import ru.yandex.vertis.shark.client.saturn.SaturnClient.DefaultService
import ru.yandex.vertis.shark.config.SaturnClientConfig
import ru.yandex.vertis.zio_baker.zio.httpclient.client.{HttpClient, TvmHttpClient}
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import ru.yandex.vertis.zio_baker.zio.tvm.Tvm
import ru.yandex.vertis.zio_baker.zio.tvm.config.TvmConfig
import zio.test.Assertion.isUnit
import zio.test.TestAspect.ignore
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{ZIO, ZLayer}
import zio.blocking.Blocking

import java.time.LocalDate

object SaturnClientSpec extends DefaultRunnableSpec {

  private val config = SaturnClientConfig(
    HttpClientConfig(
      url = "https://saturn-testing.mlp.yandex.net",
      tvmClientId = 2028166.some
    )
  )

  private val tvmConfig = TvmConfig(
    selfClientId = 2022880,
    secret = "secret",
    destClientIds = Seq(2028166),
    srcClientIds = Seq.empty
  )

  private lazy val tvmServiceLayer = ZLayer.succeed(tvmConfig) ++ FeaturesTest.test >>> Tvm.live

  private lazy val httpClientBackendLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(config.http) >>> HttpClient.blockingLayer

  private lazy val client =
    httpClientBackendLayer >+>
      tvmServiceLayer >+>
      ZLayer.succeed(config.http) >+>
      TvmHttpClient.layer >+>
      ZLayer.succeed(config) >+>
      SaturnClient.live

  private val request = SaturnClient.SearchRequest(
    "6fb40694-5003-43cd-add1-bfffd5cfe5c2",
    DefaultService,
    None,
    51727606,
    Some("79267010001"),
    Some("vasyivanov@yandex.ru"),
    Some("Иванов Василий"),
    Some(SaturnClient.SearchRequest.NumChildren.One),
    Some("1234506789"),
    Some(LocalDate.of(2005, 2, 2)),
    Some(LocalDate.of(1982, 11, 25)),
    Some("Россия Самарская обл г Тольятти"),
    Some(true),
    Some("Петров"),
    Some("Самарская обл, г Тольятти, Обводное шоссе, д 3"),
    Some("г Москва, пр-кт Вернадского, д 99, корп 1"),
    Some(SaturnClient.SearchRequest.JobType.Employee),
    Some(SaturnClient.SearchRequest.JobPositionType.ItSpecialist),
    Some(SaturnClient.SearchRequest.WorkExperience.FiveToSevenYears),
    Some(100000),
    Some(SaturnClient.SearchRequest.IncomeConfirmation.By2Ndfl),
    Some("Петров"),
    None,
    Some(SaturnClient.SearchRequest.Education.IncompleteHigher),
    Some(SaturnClient.SearchRequest.MaritalStatus.Divorced),
    Some(SaturnClient.SearchRequest.RentalType.PayRent)
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SaturnClient")(
      testM("get saturn score") {
        val res = for {
          client <- ZIO.service[SaturnClient.Service]
          result <- client.search(request)
        } yield {
          println(result)
        }
        assertM(res)(isUnit).provideLayer(client)
      } @@ ignore
    )
  }
}
