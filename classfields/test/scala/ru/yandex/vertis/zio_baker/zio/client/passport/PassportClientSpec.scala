package ru.yandex.vertis.zio_baker.zio.client.passport

import com.softwaremill.tagging.Tagger
import ru.yandex.passport.model.api.api_model.UserEssentials
import ru.yandex.vertis.zio_baker.model.{AutoUser, Tag}
import ru.yandex.vertis.zio_baker.zio.client.passport.PassportClient.SearchBy
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio.test.Assertion.{isNonEmpty, isTrue}
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.ZLayer
import zio.blocking.Blocking

import scala.concurrent.duration.DurationInt

object PassportClientSpec extends DefaultRunnableSpec {

  private val clientConfig: PassportClientConfig = PassportClientConfig(
    HttpClientConfig(
      url = "http://passport-api-server.vrts-slb.test.vertis.yandex.net",
      connectionTimeout = 3.seconds
    )
  )

  private lazy val httpClientBackendLayer =
    ZLayer.requires[Blocking] ++ ZLayer.succeed(clientConfig.http) >>> HttpClient.blockingLayer

  private lazy val clientLayer =
    httpClientBackendLayer ++ ZLayer.succeed(clientConfig) >>> PassportClient.live

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PassportClient")(
      testM("getUserEssentials") {
        assertM(
          PassportClient
            .getUserEssentials(AutoUser("1".taggedWith[Tag.UserId]))
            .map(_.isInstanceOf[UserEssentials])
        )(isTrue).provideLayer(clientLayer)
      },
      testM("search") {
        val r = PassportClient.search(SearchBy.Phone("+79270953410"))
        assertM(r)(isNonEmpty).provideLayer(clientLayer)
      }
    ) @@ ignore
}
