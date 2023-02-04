package ru.yandex.vertis.phonoteka.config

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.namemappers.implicits.hyphenCase
import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.phonoteka.client.SwitchbladeClient.Implementation
import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.quality.cached_utils.config.MethodCachingConfig.cachingConfigMapReader
import ru.yandex.vertis.phonoteka.config.ClientConfig.clientConfigReader
import ru.yandex.vertis.quality.http_client_utils.config.{HttpClientConfig, ProxyConfig, SslContexConfig}

import scala.concurrent.duration._

class OfClientConfigReaderSpec extends SpecBase {

  val config: Config =
    ConfigFactory.parseString(
      """
      |of-client {
      |  implementation: http
      |  http-client-config {
      |    url = "https://ml.datalab.megafon.ru:10443"
      |    proxy-config {
      |      host = "infra-proxy.slb.vertis.yandex.net"
      |      port = 3128
      |    }
      |    ssl-context-config {
      |      certificate-path = "/etc/yandex/vertis-datasources-secrets/of-cert.p12"
      |      password = "not_real_password_at_all"
      |    }
      |  }
      |}
    """.stripMargin
    )

  "ficus config" should {
    "parse OfClientConfig correctly" in {
      val ofClientConfig = config.as[ClientConfig]("of-client")
      val expected =
        ClientConfig(
          Implementation.Http,
          HttpClientConfig(
            "https://ml.datalab.megafon.ru:10443",
            0,
            3.seconds,
            Map(),
            Some(ProxyConfig("infra-proxy.slb.vertis.yandex.net", 3128)),
            Some(
              SslContexConfig(
                "/etc/yandex/vertis-datasources-secrets/of-cert.p12",
                "not_real_password_at_all",
                trustManagerFactorySecure = false
              )
            ),
            None
          )
        )
      ofClientConfig shouldBe expected
    }
  }
}
