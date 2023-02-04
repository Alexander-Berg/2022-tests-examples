package auto.dealers.multiposting.logic.test.statistic

import auto.dealers.multiposting.logic.statistic.ExternalStatisticCounterCleaner
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import common.zio.logging.Logging
import auto.dealers.multiposting.storage.testkit.ExternalStatisticCounterDaoMock

object ExternalStatisticCounterCleanerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ExternalStatisticCounterCleanerTask")(
      removeRowsChunked
    ) @@ sequential

  private val removeRowsChunked = testM("should delete rows") {
    val externalStatisticCounterDaoMock =
      ExternalStatisticCounterDaoMock.DeleteStaleRows(value(10)) ++
        ExternalStatisticCounterDaoMock.DeleteStaleRows(value(5)) ++
        ExternalStatisticCounterDaoMock.DeleteStaleRows(value(0))

    val env = (Clock.live ++ externalStatisticCounterDaoMock ++ Logging.live) >>> ExternalStatisticCounterCleaner.live

    assertM(ExternalStatisticCounterCleaner.removeStale())(isUnit).provideCustomLayer(env)
  }

}
