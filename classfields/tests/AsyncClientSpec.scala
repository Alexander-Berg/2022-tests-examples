package common.yt.tests

import common.yt.Yt.Yt
import common.yt.live.YtLive
import common.yt.tests.suites.{CypressSuite, TablesSuite, YtSuite}
import common.zio.logging.Logging.Logging
import ru.yandex.inside.yt.kosher.impl.YtConfiguration
import zio.clock.Clock
import zio.{Has, RLayer}

object AsyncClientSpec extends YtClientTest {
  override protected def clientName: String = "async"

  override protected def suites: Seq[YtSuite] = Seq(
    CypressSuite,
    TablesSuite
  )

  override protected def clientLayer: RLayer[Has[YtConfiguration] with Clock with Logging, Yt] =
    YtLive.http
}
