package ru.vertistraf.common.util

import com.typesafe.config.ConfigFactory
import common.zio.config.Configuration
import common.zio.pureconfig.Pureconfig
import ru.vertistraf.common.model.s3.S3Path
import ru.vertistraf.common.model.s3.S3Path.{PathPart, S3DirPath, S3FilePath}
import zio._
import zio.test._
import zio.test.Assertion._
import eu.timepit.refined.auto._
import pureconfig.ConfigReader

import scala.reflect.ClassTag

object S3PathReadersSpec extends DefaultRunnableSpec {

  import S3PathReaders._

  private def readStringEffect(str: String): Task[S3Path] =
    ZIO.fromTry(S3PathReaders.readFromString(str))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("S3PathReaders")(
      successReadFromStringSpec(),
      failsOnStringRead(),
      failOnConfigRead(),
      correctlyReadFromConfig()
    )

  private def s3File(bucket: String, fileName: PathPart, parts: Seq[PathPart]): S3FilePath =
    S3FilePath(S3DirPath(bucket, parts), fileName)

  private def successReadFromStringSpec() = {
    val cases: Map[String, S3Path] = Map(
      "bucket:file" -> s3File("bucket", "file", Seq.empty),
      "bucket:/file" -> s3File("bucket", "file", Seq.empty),
      "bucket:file/in/dir/file" -> s3File("bucket", "file", Seq("file", "in", "dir")),
      "bucket:/file/in/dir/file" -> s3File("bucket", "file", Seq("file", "in", "dir")),
      "bucket:" -> S3DirPath("bucket", Seq.empty),
      "bucket:/" -> S3DirPath("bucket", Seq.empty),
      "bucket:dir/" -> S3DirPath("bucket", Seq("dir")),
      "bucket:/dir/" -> S3DirPath("bucket", Seq("dir")),
      "bucket:long/dir/key/" -> S3DirPath("bucket", Seq("long", "dir", "key")),
      "realty:export/landings/flats_0.yml" -> s3File("realty", "flats_0.yml", Seq("export", "landings")),
      "realty:/export/landings/flats_0.yml" -> s3File("realty", "flats_0.yml", Seq("export", "landings")),
      "realty:export/landings/" -> S3DirPath("realty", Seq("export", "landings"))
    )

    def singleTest(input: String, expected: S3Path): ZSpec[Any, Throwable] =
      testM(s"when comes `$input`") {
        readStringEffect(input).map(actual => assertTrue(actual == expected))
      }

    suite("should correctly read from string")(
      cases.toSeq
        .map { case (i, e) => singleTest(i, e) }: _*
    )
  }

  private def failsOnStringRead() = {
    val cases: Seq[String] = Set(
      "bucket-only",
      "incorrect!bucket:",
      "bucket::/",
      " bucket:/path",
      "bucket :/path",
      "bucket: /path",
      "bucket://path",
      "bucket:/path/to my/file.xml"
    ).toSeq

    suite("should correctly fail on read from string")(
      cases
        .map { input =>
          testM(s"when comes `$input`") {
            assertM(readStringEffect(input).run)(fails(anything))
          }
        }: _*
    )
  }

  private def configsLayer =
    Configuration
      .load(ConfigFactory.load("s3-path-readers.spec.conf"))
      .mapError { e =>
        TestFailure.die(e)
      }
      .toLayer

  private def failOnConfigRead() =
    suite("should correctly fail on configs")(
      testM("when file reading and dir presented") {
        assertM(Pureconfig.load[S3DirPath]("valid-file").run)(fails(anything))
      },
      testM("when dir reading and file presented") {
        assertM(Pureconfig.load[S3FilePath]("valid-dir").run)(fails(anything))
      },
      testM("when missing dir property") {
        assertM(Pureconfig.load[S3DirPath]("only-bucket-property").run)(fails(anything))
      },
      testM("when missing file property") {
        assertM(Pureconfig.load[S3FilePath]("only-bucket-property").run)(fails(anything))
      },
      testM("when invalid file") {
        assertM(Pureconfig.load[S3FilePath]("invalid-file").run)(fails(anything))
      },
      testM("when invalid dir") {
        assertM(Pureconfig.load[S3DirPath]("invalid-dir").run)(fails(anything))
      }
    ).provideLayer(configsLayer)

  private def correctlyReadFromConfig() = {
    val file = s3File("bucket", "file.txt", Seq("path", "to"))
    val dir = S3DirPath("bucket", Seq("path", "to", "dir"))

    def testRead[T: ConfigReader: ClassTag](name: String, expected: T, labelsSuffix: String = "") =
      testM(s"correctly read `$name`$labelsSuffix") {
        Pureconfig.load[T](name).map(actual => assertTrue(actual == expected))
      }

    suite("correctly read from config")(
      testRead[S3FilePath]("valid-file", file),
      testRead[S3FilePath]("one-line-file", file),
      testRead[S3DirPath]("valid-dir", dir),
      testRead[S3DirPath]("one-line-dir", dir),
      testRead[S3Path]("valid-file", file, labelsSuffix = " when S3Path model requested"),
      testRead[S3Path]("one-line-file", file, labelsSuffix = " when S3Path model requested"),
      testRead[S3Path]("valid-dir", dir, labelsSuffix = " when S3Path model requested"),
      testRead[S3Path]("one-line-dir", dir, labelsSuffix = " when S3Path model requested")
    ).provideLayer(configsLayer)
  }
}
