package ru.yandex.realty.traffic.utils

import com.google.protobuf.Int64Value
import org.junit.runner.RunWith
import ru.yandex.realty.traffic.utils.FilesService.FilesServiceConfig
import ru.yandex.realty.traffic.utils.StoredEntriesFile.StoredFormat
import zio.blocking.Blocking
import zio.{ZIO, ZLayer}
import zio.stream.ZStream
import zio.test._
import zio.test.junit._

import java.nio.file.Files
import scala.tools.nsc.interpreter.{InputStream, OutputStream}

@RunWith(classOf[ZTestJUnitRunner])
class StreamToFilesDistributorSpec extends JUnitRunnableSpec {

  implicit object LongStoredFormat extends StoredFormat[Long] {
    override def write(e: Long, os: OutputStream): Unit =
      Int64Value.of(e).writeDelimitedTo(os)

    override def read(is: InputStream): Option[Long] =
      Option(Int64Value.parseDelimitedFrom(is)).map(_.getValue)
  }

  private def specEffect[T: StoredFormat](data: Seq[T], extractor: T => Seq[String], expected: Map[String, Seq[T]]) =
    (StreamToFilesDistributor
      .distribute[Any, T] {
        case (x, _) =>
          extractor(x)
      }(ZStream.fromIterable(data))
      .flatMap { result =>
        ZIO.foreach[Blocking, Throwable, String, String, StoredEntriesFile[T], Seq[T]](result) {
          case (name, file) =>
            file.read.runCollect.map(res => name -> res)
        }
      }
      .map(actual => assertTrue(actual == expected)) <* FilesService.freeAllTemporary())
      .provideLayer(filesServiceLayer ++ Blocking.live)

  private def correctlyDistributeSingleKeySpecEffect = {
    val denominator: Long = 5
    val maxElem: Long = 100
    val elements: Seq[Long] = 0L until maxElem
    val expected: Map[String, Seq[Long]] =
      (0L until denominator).map { d =>
        d.toString -> (d until maxElem by denominator)
      }.toMap

    specEffect[Long](elements, element => Seq((element % 5).toString), expected)
  }

  private def correctlyDistributeMultipleKeysSpecEffect = {
    val elements = 1L to 5L
    val expected: Map[String, Seq[Long]] = elements.map(k => k.toString -> elements).toMap

    specEffect[Long](elements, _ => elements.map(_.toString), expected)
  }

  private def filesServiceLayer =
    ZLayer.succeed(FilesServiceConfig(Files.createTempDirectory("StreamToFilesDistributorSpec"))) >>> FilesService.live

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("StreamToFilesDistributor")(
      testM("correctly distribute by single key")(
        correctlyDistributeSingleKeySpecEffect
      ),
      testM("correctly distribute multiple keys") {
        correctlyDistributeMultipleKeysSpecEffect
      }
    )
}
