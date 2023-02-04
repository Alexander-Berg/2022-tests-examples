package common.yt.tests.suites

import common.yt.Yt
import common.yt.Yt.Yt
import common.yt.tests.Typing.YtBasePath
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Has, ZIO}
import zio.test.TestAspect._

import java.util.UUID

object CypressSuite extends YtSuite {

  override def ytSuite: ZSpec[TestEnvironment with Yt with Has[YtBasePath], Any] =
    suite("Cypress should correctly")(
      correctlyReturnExists,
      correctlyCreateAndDeleteTable
    ) @@ sequential

  private def correctlyReturnExists =
    testM("return for exists configured base path") {
      for {
        basePath <- ZIO.service[YtBasePath]
        res <- ZIO
          .accessM[Yt](_.get.cypress.exists(basePath))
      } yield assertTrue(res)
    }

  private def correctlyCreateAndDeleteTable =
    testM("create table") {
      for {
        yt <- ZIO.service[Yt.Service]
        basePath <- ZIO.service[YtBasePath]
        target <- ZIO.effectTotal(basePath.child(UUID.randomUUID().toString))
        _ <- yt.cypress.createTable(target, ignoreExisting = false)
        check <- yt.cypress.exists(target)
        duplicationFailed <- yt.cypress
          .createTable(target, ignoreExisting = false)
          .as(false)
          .catchAll(_ => ZIO.succeed(true))
        _ <- yt.cypress.remove(target)
        nonExist <- yt.cypress.exists(target).map(!_)
      } yield assertTrue(check) && assertTrue(duplicationFailed) && assertTrue(nonExist)
    }
}
