package ru.yandex.vertis.zio_baker.zio.client.vos

import com.softwaremill.tagging._
import ru.auto.api.api_offer_model.Category
import ru.yandex.vertis.zio_baker.model.{OfferId, Tag}
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio.test.Assertion.equalTo
import zio.test.TestAspect.ignore
import zio.test._
import zio.test.environment.TestEnvironment
import zio.ZLayer
import zio.blocking.Blocking

object VosAutoruClientSpec extends DefaultRunnableSpec {

  private val config = VosAutoruClientConfig(
    HttpClientConfig(url = "http://vos2-autoru-api.vrts-slb.test.vertis.yandex.net:80")
  )

  private lazy val clientEnv =
    ZLayer.requires[Blocking] ++
      ZLayer.succeed(config.http) >>>
      HttpClient.blockingLayer ++
      ZLayer.succeed(config) >>>
      VosAutoruClient.live

  def spec: ZSpec[TestEnvironment, Any] =
    suite("VosAutoruClient") {
      testM("offer") {
        val offerId: OfferId = "1043045004-977b3".taggedWith[Tag.OfferId]
        val category = Category.CARS
        val res = for {
          offer <- VosAutoruClient.offer(category, offerId, includeRemoved = true)
        } yield offer.id
        assertM(res)(equalTo(offerId)).provideLayer(clientEnv)
      }
    } @@ ignore
}
