package ru.yandex.realty.traffic.utils.distribution

import com.google.protobuf.Int32Value
import ru.yandex.realty.traffic.utils.FilesService
import ru.yandex.realty.traffic.utils.FilesService.FilesServiceConfig
import ru.yandex.realty.traffic.utils.StoredEntriesFile.StoredFormat
import ru.yandex.realty.traffic.utils.distribution.Distribution.Distributed
import zio.blocking.Blocking
import zio.magic._
import zio.random.Random
import zio.stream.ZStream
import zio.test._
import zio.test.junit.JUnitRunnableSpec
import zio.{ZIO, ZLayer}

import java.nio.file.Paths
import scala.tools.nsc.interpreter.{InputStream, OutputStream}

class DistributionSpec extends JUnitRunnableSpec {

  implicit val storedFormat: StoredFormat[Int] = new StoredFormat[Int] {
    override def write(e: Int, os: OutputStream): Unit =
      Int32Value.of(e).writeDelimitedTo(os)

    override def read(is: InputStream): Option[Int] =
      Option(Int32Value.parseDelimitedFrom(is))
        .map(_.getValue)
  }

  private def runSpec =
    Gen
      .listOfN(100)(Gen.int(0, 10000))
      .runHead
      .map(_.get)
      .flatMap { input =>
        ZStream
          .fromIterable(input)
          .run {
            Distribution.sink(i => ZIO.effectTotal(i % 3), ZIO.succeed(_))
          }
          .flatMap {
            ZIO
              .foreach(_) {
                case Distributed(key, stored, count) =>
                  stored.read.runCollect
                    .map(_.toList)
                    .tap { data =>
                      ZIO.when(data.size != count) {
                        ZIO.fail(
                          new IllegalStateException(s"Wrong count found, expected ${data.size}, but found $count")
                        )
                      }
                    }
                    .map(key -> _)
              }
              .map(_.toMap)
          }
          .map { actual =>
            val expectedDist =
              input.groupBy(_ % 3)

            assertTrue(actual == expectedDist)
          }
      } <* FilesService.freeAllTemporary()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Distribution") {
      testM("should correctly distribute") {
        runSpec.inject(
          FilesService.live,
          ZLayer.succeed(FilesServiceConfig(Paths.get("/tmp/DistributionSpec/"))),
          Blocking.live,
          Random.live
        )
      }
    }
}
