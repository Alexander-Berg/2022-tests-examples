package common.yt.tests

import common.yt.Yt.Yt
import common.yt.live.YtLive
import common.yt.tests.suites.{CypressSuite, OperationsSuite, TablesSuite, YtSuite}
import common.zio.logging.Logging
import ru.yandex.inside.yt.kosher.impl.YtConfiguration
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.{Has, RLayer, ZLayer}

object SyncClientSpec extends YtClientTest {
  override protected def clientName: String = "sync"

  override protected def suites: Seq[YtSuite] = Seq(
    CypressSuite,
    TablesSuite,
    OperationsSuite
  )

  override protected def clientLayer: RLayer[Has[YtConfiguration], Yt] =
    ZLayer.service[YtConfiguration] ++ Blocking.live ++ Logging.live ++ Clock.live ++ Random.live >>> YtLive.sync
}
