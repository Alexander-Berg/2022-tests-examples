package vs.registry.db

import bootstrap.config.Source
import bootstrap.metrics.Registry
import bootstrap.test.TestBase
import bootstrap.testcontainers.ydb.YdbContainer
import bootstrap.tracing.$
import bootstrap.ydb.{YDB, YdbTest}
import zio.{Clock, Scope, ZLayer}

import scala.concurrent.duration.DurationInt

object Context {

  val ydbLayer: ZLayer[
    Scope,
    Nothing,
    $ & Registry & Clock & YDB[Source.Const[YDB.Config]],
  ] =
    YdbContainer.stable.orDie >>>
      (TestBase.bootstrap >+>
        YdbTest.fromContainer[TEST](
          Source.Const(
            YDB.Config(
              endpoint = "",
              database = "",
              tablePrefix = "/local",
              denyCrossDc = false,
              session = YDB.SessionConfig(
                minPoolSize = 10,
                maxPoolSize = 50,
                acquireTimeout = zio.Duration.fromScala(5.seconds),
                maxAcquireTries = 3,
                maxIdleTime = zio.Duration.fromScala(1.minute),
                keepAliveTimeout = zio.Duration.fromScala(5.minutes),
              ),
              keepQueryText = true,
              queryCacheSize = 16,
            ),
          ),
        ))

}
