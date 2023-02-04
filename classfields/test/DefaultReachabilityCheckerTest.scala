package auto.c2b.common.geo.test

import auto.c2b.common.geo.ReachabilityChecker
import auto.c2b.common.geo.ReachabilityChecker.Circle
import common.zio.logging.Logging
import zio.ZLayer
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object DefaultReachabilityCheckerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultReachabilityManager")(
      testM("Measure distance between geo points") {
        for {
          butovo <- ReachabilityChecker.isReachable(55.580518, 37.583311)
          zelenograd <- ReachabilityChecker.isReachable(55.984185, 37.189268)
        } yield assertTrue(butovo) && assertTrue(!zelenograd)
      }
    )
  }.provideCustomLayerShared {
    Logging.live ++ ZLayer.succeed(
      ReachabilityChecker.Config(Set(Circle(55.755705, 37.617879, 34, 5)))
    ) >>> ReachabilityChecker.live
  }
}
