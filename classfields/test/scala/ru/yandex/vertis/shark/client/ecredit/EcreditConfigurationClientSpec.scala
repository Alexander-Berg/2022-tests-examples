package ru.yandex.vertis.shark.client.ecredit

import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.config.EcreditConfigurationClientConfig
import ru.yandex.vertis.shark.model.Tag
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio.test.Assertion.isUnit
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{ZIO, ZLayer}
import zio.blocking.Blocking

object EcreditConfigurationClientSpec extends DefaultRunnableSpec {

  private val config = EcreditConfigurationClientConfig(
    HttpClientConfig(
      url = "http://credspec.car-fin.com"
    )
  )

  private lazy val httpClientBackendLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(config.http) >>> HttpClient.blockingLayer

  private lazy val layer =
    httpClientBackendLayer ++ ZLayer.succeed(config) >>> EcreditConfigurationClient.live

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("EcreditConfigurationClientSpec") {
      testM("ecredit car fin req returns result") {
        val res = for {
          client <- ZIO.service[EcreditConfigurationClient.Service]
          configs <- client.configurations(6, 145000L.taggedWith[Tag.MoneyRub], None, None, None, None)
        } yield {
          println(configs)
        }
        assertM(res)(isUnit).provideLayer(layer)
      }
    } @@ ignore
  }
}
