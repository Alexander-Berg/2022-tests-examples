package ru.yandex.vertis.zio_baker.zio.resource

import common.zio.logging.Logging
import zio.Exit
import zio.ZIO
import zio.clock.Clock
import zio.duration._
import zio.stream.ZStream
import zio.test._
import zio.test.environment.TestEnvironment

object ReloadUtilSpec extends DefaultRunnableSpec {
  object Fixture extends ReloadUtil with Logging

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ReloadUtil")(
      testM("Produce a ref after first load") {
        val expected = "done"
        for {
          result <- Fixture
            .reloadToServiceRef(
              ZStream(Exit.succeed(expected))
            )(ZIO.succeed(_))
            .use(_.get)
            .timeoutFail("timed out")(1.second)
        } yield assertTrue(result == expected)
      }
    ).provideCustomLayerShared(Clock.live >+> ResourceLoader.live)
}
