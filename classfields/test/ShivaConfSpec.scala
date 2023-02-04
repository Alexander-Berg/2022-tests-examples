package tools.shiva.conf.test

import common.clients.vault.{Value, Version}
import common.clients.vault.testkit.VaultClientMock
import ru.yandex.vertis.shiva.types.env.env.Env
import ru.yandex.vertis.shiva.types.layer.layer.Layer
import tools.shiva.client.testkit.ShivaClientMock
import tools.shiva.conf.ShivaConf
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.Assertion._
import zio.test.mock.Expectation._

object ShivaConfSpec extends DefaultRunnableSpec {
  val service = "qwe"
  val token = "123"
  val version = "ver-456"

  override val spec: ZSpec[TestEnvironment, Any] =
    suite("ShivaConfSpec")(
      testM("simple hocon resolution") {
        val shivaClient = ShivaClientMock.GetEnvs(
          equalTo((service, Layer.TEST)),
          value {
            Seq(
              Env(key = "QWE", value = "${sec-123:ver-456:qwe}"),
              Env(key = "ASD", value = "should skip"),
              Env(key = "ZXC", value = "from shiva")
            )
          }
        )

        val vaultClient = VaultClientMock.GetVersion(
          equalTo((token, version)),
          value {
            Version(
              List(
                Value("qwe", "from vault")
              )
            )
          }
        )

        val conf = """
          qwe = ${QWE}
          asd = "keep as is"

          zxc = {
            zxc = ${?ZXC}
          }
        """

        val want = """
          qwe = "from vault"
          asd = "keep as is"

          zxc = {
            zxc = "from shiva"
          }
        """

        assertM(ShivaConf(_.runHocon(conf, service, token)))(equalTo(want))
          .provideLayer(shivaClient ++ vaultClient >>> ShivaConf.live)
      },
      testM("replace consul urls") {
        val shivaClient = ShivaClientMock.GetEnvs(
          equalTo((service, Layer.TEST)),
          value {
            Seq(
              Env(key = "QWE", value = "dns://qwe-asd.service.consul:123")
            )
          }
        )

        val conf = """
          qwe = ${QWE}
        """

        val want = """
          qwe = "qwe-asd.vrts-slb.test.vertis.yandex.net"
        """

        assertM(ShivaConf(_.runHocon(conf, service, token)))(equalTo(want))
          .provideCustomLayer(VaultClientMock.empty ++ shivaClient >>> ShivaConf.live)
      },
      testM("simple envs resolution") {
        val shivaClient = ShivaClientMock.GetEnvs(
          equalTo((service, Layer.TEST)),
          value {
            Seq(
              Env(key = "QWE", value = "qwe"),
              Env(key = "ASD", value = "as'd")
            )
          }
        )

        val want = "QWE='qwe' ASD='as'\\''d'"

        assertM(ShivaConf(_.runEnvs(service, token)))(equalTo(want))
          .provideCustomLayer(VaultClientMock.empty ++ shivaClient >>> ShivaConf.live)
      }
    )
}
