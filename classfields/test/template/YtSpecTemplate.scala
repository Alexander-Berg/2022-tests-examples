package ru.auto.salesman.test.template

import ru.auto.salesman.settings.{YtClientSettings, YtProxy, YtToken}
import ru.yandex.bolts.collection.{Cf, ListF}
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.impl.YtUtils
import ru.yandex.inside.yt.kosher.impl.common.http.HeavyProxiesProvider

import scala.collection.JavaConverters._

trait YtSpecTemplate {
  import YtSpecTemplate._

  def client: Yt = {
    val cfg = YtClientSettings(YtProxy(YtContainer.httpApi), YtToken(testToken))

    final class TestHeavyProxyProvider() extends HeavyProxiesProvider {

      def getHeavyProxies: ListF[String] =
        Cf.wrap(List[String](cfg.proxy.asString).asJava)
    }

    val ytCfg = YtUtils
      .getDefaultConfigurationBuilder(cfg.proxy.asString, cfg.token.asString)
      .withHeavyProxiesProvider(new TestHeavyProxyProvider())
      .build()

    YtUtils.http(ytCfg)
  }
}

object YtSpecTemplate {

  private val testToken = "test-token"

}
