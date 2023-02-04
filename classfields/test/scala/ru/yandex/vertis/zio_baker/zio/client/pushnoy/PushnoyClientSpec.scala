package ru.yandex.vertis.zio_baker.zio.client.pushnoy

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.zio_baker.model.Tag
import ru.yandex.vertis.zio_baker.model.pushnoy._
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio.test.Assertion.isUnit
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.ZLayer
import zio.blocking.Blocking

import scala.concurrent.duration._

object PushnoyClientSpec extends DefaultRunnableSpec {

  private val clientConfig: PushnoyClientConfig = PushnoyClientConfig(
    HttpClientConfig(
      url = "http://pushnoy-api-http-api.vrts-slb.test.vertis.yandex.net:80",
      connectionTimeout = 10.seconds
    )
  )

  private lazy val httpClientBackendLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(clientConfig.http) >>> HttpClient.blockingLayer

  private lazy val clientLayer = httpClientBackendLayer ++ ZLayer.succeed(clientConfig) >>> PushnoyClient.live

  private val template = PushTemplateV1(
    event = "test",
    pushName = "sharkNotificatiton",
    deepLink = "test.avto.ru/my/credits/",
    title = "Android title",
    body = "Android body",
    target = Target.Devices,
    pushParams = PushParams(
      userId = "5".taggedWith[Tag.UserId],
      delivery = PushParams.PushnoyDelivery.ServicesAndDiscounts.some,
      appVersion = None
    )
  )

  def spec: ZSpec[TestEnvironment, Any] =
    suite("PushnoyClient")(
      testM("send") {
        assertM(PushnoyClient.send(template))(isUnit).provideLayer(clientLayer)
      }
    ) @@ ignore
}
