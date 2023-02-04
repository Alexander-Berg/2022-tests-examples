package vertis.shiva.client

import vertis.shiva.client.conf.ShivaPublicApiConfig
import vertis.zio.test.ZioSpecBase
import vertis.zio.test.ZioSpecBase.TestBody

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait ShivaClientSupport extends ZioSpecBase {

  protected def withShivaClient(test: ShivaPublicApiClient => TestBody): Unit =
    ioTest {
      ShivaPublicApiClient
        .make(ShivaPublicApiConfig("shiva-public.vertis.yandex.net:443"))
        .use(test)
    }
}
