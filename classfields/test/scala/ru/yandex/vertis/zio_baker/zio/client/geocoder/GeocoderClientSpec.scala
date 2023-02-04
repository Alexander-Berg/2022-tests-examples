package ru.yandex.vertis.zio_baker.zio.client.geocoder

import com.softwaremill.tagging.Tagger
import common.zio.features.testkit.FeaturesTest
import ru.yandex.vertis.zio_baker.model.Tag
import ru.yandex.vertis.zio_baker.zio.client.geocoder.GeocoderClient.GeocodeResult
import ru.yandex.vertis.zio_baker.zio.httpclient.client.{HttpClient, TvmHttpClient}
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import ru.yandex.vertis.zio_baker.zio.tvm.Tvm
import ru.yandex.vertis.zio_baker.zio.tvm.config.TvmConfig
import zio.blocking.Blocking
import zio.test.Assertion.equalTo
import zio.test.{assertM, DefaultRunnableSpec, TestAspect, ZSpec}
import zio.{ZIO, ZLayer}

object GeocoderClientSpec extends DefaultRunnableSpec {

  private val config = GeocoderClientConfig(
    HttpClientConfig(
      url = "http://addrs-testing.search.yandex.net/search/stable/yandsearch"
    )
  )

  private val tvmConfig = TvmConfig(
    selfClientId = 2022638,
    secret = "secret",
    destClientIds = Seq(2001337),
    srcClientIds = Seq.empty
  )

  private lazy val tvmServiceLayer = ZLayer.succeed(tvmConfig) ++ FeaturesTest.test >>> Tvm.live

  private lazy val httpClientBackendLayer =
    Blocking.live ++ ZLayer.succeed(config.http) >+> HttpClient.blockingLayer

  private lazy val geocoderClientLayer =
    httpClientBackendLayer >+>
      tvmServiceLayer >+>
      ZLayer.succeed(config.http) >+>
      TvmHttpClient.layer >+>
      ZLayer.succeed(config) >+>
      GeocoderClient.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("BigBrotherClient")(
      testM("get big brother profile by phone") {
        val res = for {
          client <- ZIO.service[GeocoderClient.Service]
          geoResult <- client.geocode("Иркутск")
        } yield {
          println(geoResult)
          geoResult
        }
        assertM(res)(equalTo(Seq(GeocodeResult(63L.taggedWith[Tag.GeobaseId]))))
          .provideLayer(geocoderClientLayer)
      }
    ) @@ TestAspect.ignore
  }
}
