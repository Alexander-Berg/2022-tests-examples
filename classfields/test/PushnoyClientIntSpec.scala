package vertistraf.common.pushnoy.client

import common.zio.logging.Logging
import common.zio.ops.tracing.testkit.TestTracing
import common.zio.sttp.Sttp
import common.zio.sttp.model.Config
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.util.ahc.HttpSettings
import vertis.zio.test.ZioSpecBase
import vertistraf.common.pushnoy.client.model.PushnoyConfig
import vertistraf.common.pushnoy.client.service.PushnoyClient
import vertistraf.common.pushnoy.client.service.impl.PushnoySttpClient
import zio.ZManaged

import scala.concurrent.duration._

/** @author kusaeva
  */
class PushnoyClientIntSpec extends ZioSpecBase with PushRendererSupport {

  private val config = PushnoyConfig(
    url = "pushnoy-api-http-api.vrts-slb.test.vertis.yandex.net",
    http = HttpSettings(1.second, 1.second, 1, -1)
  )

  private val qaAutoUser = "user:20621551"

  private val domain = Domain.DOMAIN_AUTO

  private def managedPushClient: ZManaged[Any, Throwable, PushnoyClient.Service] =
    for {
      sttp <- Sttp.configured(Config.Default)
      log <- Logging.live.build.map(_.get)
      tracing <- TestTracing.noOp.build.map(_.get)
      client <- PushnoySttpClient.create(domain, config, sttp, log, tracing).toManaged_
    } yield client

  "PushnoyClient" should {
    "get user devices" in ioTest {
      managedPushClient.use { client =>
        for {
          devices <- client.getUserDevices(qaAutoUser)
          _ <- check(devices should not be empty)
        } yield ()
      }
    }

    "return zero counter while sending push to unknown device" in ioTest {
      managedPushClient.use { client =>
        for {
          rendered <- render
          result <- client.send("unknownDevice", rendered, Some("spam"), None)
          _ <- logger.info(s"result $result")
          _ <- check(result.count shouldEqual 0)
        } yield ()
      }
    }
  }
}
