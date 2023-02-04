package baker.common.client.dadata

import baker.common.client.dadata.model.Fio.Gender.Male
import cats.implicits.catsSyntaxOptionId
import ru.yandex.vertis.zio_baker.zio.httpclient.client.HttpClient
import ru.yandex.vertis.zio_baker.zio.httpclient.config.{HttpClientConfig, ProxyConfig}
import zio.test.Assertion.isTrue
import zio.test.TestAspect.ignore
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.ZLayer
import zio.blocking.Blocking

import scala.concurrent.duration._

object DadataClientSpec extends DefaultRunnableSpec {

  private val proxyConfig: ProxyConfig = ProxyConfig("proxy-ext.test.vertis.yandex.net", 3128)

  private val clientConfig: DadataClientConfig = DadataClientConfig(
    HttpClientConfig(
      url = "https://suggestions.dadata.ru:443",
      connectionTimeout = 10.seconds,
      proxyConfig = proxyConfig.some
    ),
    "token"
  )

  private lazy val httpClientBackendLayer =
    Blocking.any ++ ZLayer.succeed(clientConfig.http) >>> HttpClient.blockingLayer

  private lazy val clientLayer = httpClientBackendLayer ++ ZLayer.succeed(clientConfig) >>> DadataClient.live

  def spec: ZSpec[TestEnvironment, Any] =
    suite("DadataClient")(
      testM("fio") {
        assertM(DadataClient.fio("Иванов Иван И").map {
          _.exists { s =>
            s.value == "Иванов Иван Иванович" && s.data.exists(_.gender == Male.some)
          }
        })(isTrue).provideLayer(clientLayer)
      }
    ) @@ ignore
}
