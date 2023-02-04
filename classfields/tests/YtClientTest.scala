package common.yt.tests

import com.typesafe.config.ConfigFactory
import common.yt.Yt.Yt
import common.yt.live.YtLive
import common.yt.tests.Typing.{wrapBasePath, YtBasePath}
import common.yt.tests.suites.YtSuite
import common.yt.{Yt, YtConfig}
import common.zio.config.Configuration
import common.zio.logging.Logging
import common.zio.pureconfig.Pureconfig
import ru.yandex.inside.yt.kosher.impl.YtConfiguration
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import zio.test.TestAspect.{afterAll, beforeAll}
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, TestFailure, ZSpec}
import zio._
import zio.clock.Clock

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

trait YtClientTest extends DefaultRunnableSpec {

  protected def clientName: String
  protected def suites: Seq[YtSuite]
  protected def clientLayer: RLayer[Has[YtConfiguration] with Clock with Logging.Logging, Yt]

  override def spec: ZSpec[TestEnvironment, Any] =
    (suite(s"Yt `$clientName` client")(
      suites.map(_.ytSuite): _*
    ) @@ beforeAll(createYtBasePath) @@ afterAll(dropYtBasePath))
      .provideCustomLayerShared(managedYtAndUniqBasePath)

  private def createYtBasePath: URIO[Has[YtBasePath] with Yt, Unit] =
    (for {
      yt <- ZIO.service[Yt.Service]
      basePath <- ZIO.service[YtBasePath]
      _ <- yt.cypress.createDirectory(
        basePath,
        ignoreExisting = false,
        recursive = true,
        attributes = Map(
          "expiration_time" -> YTree.stringNode(
            Instant.now().plus(1L, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS).toString
          )
        )
      )
    } yield ()).orDie

  private def dropYtBasePath: URIO[Has[YtBasePath] with Yt, Unit] =
    (for {
      yt <- ZIO.service[Yt.Service]
      basePath <- ZIO.service[YtBasePath]
      _ <- yt.cypress.remove(basePath)
    } yield ()).orDie

  private def managedYtAndUniqBasePath: ZLayer[Any, TestFailure[Throwable], Has[YtBasePath] with Yt] = {
    val configuration = Configuration.load(ConfigFactory.load("test-yt.conf")).toLayer

    val ytConfig = configuration >>> Pureconfig
      .loadLayer[YtConfig]("yt-test")

    val ytBasePath = configuration >>> Pureconfig
      .load[YtBasePath]("yt-temp-dir")
      .map(_.child(UUID.randomUUID().toString))
      .map(wrapBasePath)
      .toLayer

    ytBasePath ++ (ytConfig >>> YtLive.configuration ++ Clock.live ++ Logging.live >>> clientLayer)
  }.mapError(ex => TestFailure.fail(ex))
}
