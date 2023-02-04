package vertis.zio.test

import vertis.zio.test.ZioSpecBase.TestEnv
import zio.duration._
import zio.{Schedule, ZIO}

/**
  */
trait ZioEventually { this: ZioSpecBase =>

  protected def defaultPatience: ZioPatienceConfig =
    ZioPatienceConfig(Schedule.recurs(7) && Schedule.exponential(15.millis))

  def checkEventually(
      assertion: ZIO[TestEnv, Throwable, _]
    )(implicit patience: ZioPatienceConfig = defaultPatience): ZIO[TestEnv, Any, _] =
    assertion.absorb.retry(patience.schedule)
}
