package common.yt.tests.suites

import common.yt.Yt.Yt
import common.yt.tests.Typing.YtBasePath
import zio._
import zio.random.Random
import zio.stream.ZStream
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.environment.TestEnvironment

import java.util.UUID
import scala.concurrent.duration._

object TablesSuite extends YtSuite {

  override def ytSuite: ZSpec[TestEnvironment with Yt with Has[YtBasePath], Any] =
    suite("Tables should correctly")(
      correctlyWriteAndRead
    ) @@ sequential

  case class Data(name: String, value: Int)

  private lazy val sampleGen: Gen[Random, Data] =
    for {
      name <- Gen.stringBounded(3, 10)(Gen.alphaChar)
      value <- Gen.anyInt
    } yield Data(name, value)

  private def correctlyWriteAndRead =
    testM("read and write data") {
      for {
        samples <- sampleGen.runCollectN(200)
        tables <- ZIO.access[Yt](_.get.tables)
        cypress <- ZIO.access[Yt](_.get.cypress)
        basePath <- ZIO.service[YtBasePath]
        table <- Task.effectTotal(basePath.child(s"rw_spec_${UUID.randomUUID().toString}"))
        _ <- cypress.createTable(table, ignoreExisting = false)
        _ <- tables.write(table, ZStream.fromIterable(samples))
        read <- tables.read[Data](table).runCollect
        _ <- cypress.remove(table)
      } yield assert(read)(hasSameElements(samples))
    }
}
