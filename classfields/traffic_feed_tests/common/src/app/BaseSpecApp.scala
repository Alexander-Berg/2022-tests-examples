package ru.vertistraf.traffic_feed_tests.common.app

import common.zio.app.BaseApp
import common.zio.app.BaseApp.BaseEnvironment
import common.zio.logging.Logging
import ru.vertistraf.traffic_feed_tests.common.service.FeedTestSpec
import zio._
import zio.test.Annotations

trait BaseSpecApp extends BaseApp with Logging {

  type ResourcesLayer <: Has[_]

  override type Env = ResourcesLayer with Annotations

  protected def makeResources: RLayer[BaseEnvironment, ResourcesLayer]
  protected def allSpecs: Seq[FeedTestSpec[ResourcesLayer]]

  final override def makeEnv: ZLayer[BaseEnvironment, Throwable, Env] =
    (ZLayer.requires[BaseEnvironment] >>> makeResources) ++ Annotations.live

  final override def program: ZIO[Env, Throwable, Any] =
    TestRunner.run(allSpecs)

}
