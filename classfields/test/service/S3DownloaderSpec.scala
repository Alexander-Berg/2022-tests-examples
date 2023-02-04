package ru.vertistraf.common.service

import common.zio.clients.s3.S3Client
import common.zio.clients.s3.S3Client.S3Client
import common.zio.clients.s3.testkit.InMemoryS3
import eu.timepit.refined.auto._
import ru.vertistraf.common.model.s3.S3Path.{PathPart, S3DirPath, S3FilePath}
import ru.vertistraf.common.service.S3Downloader.{S3Downloader, S3DownloaderConfig}
import zio.clock.Clock
import zio.stream.ZStream
import zio.test._
import zio.{RIO, Task, ZIO, ZLayer}

import java.nio.file.{Files, Path}

object S3DownloaderSpec extends DefaultRunnableSpec {

  private val Bucket: String = "some-bucket"
  private val TempBucket: String = "temp-bucket"

  private val TempDir = S3DirPath(TempBucket, Seq("empty-dir"))

  private val TestFiles: Map[S3FilePath, String] =
    Map(
      simpleFilePath(Bucket, "some", "file1") -> "file 1 content",
      simpleFilePath(Bucket, "file2") -> "file 2 content",
      simpleFilePath(Bucket, "dir", "file3") -> "file 3 content"
    )

  private def simpleFilePath(bucket: String, first: PathPart, tail: PathPart*) =
    if (tail.isEmpty) {
      S3FilePath(S3DirPath(bucket, Seq.empty), first)
    } else {
      S3FilePath(S3DirPath(bucket, Seq(first) ++ tail.dropRight(1)), tail.last)
    }

  private def s3Layer(files: Map[S3FilePath, String]) =
    (for {
      s3 <- InMemoryS3.make
      _ <- ZIO.foreach_(files) { case (path, content) =>
        val bytes = content.getBytes

        s3.uploadContent(
          path.bucket,
          path.key,
          bytes.length,
          "type",
          ZStream.fromIterable(bytes),
          Map.empty,
          None
        )
      }
    } yield s3: S3Client.Service).toLayer

  private def testWithFilesService[R](label: String)(f: FilesService.Service => RIO[R, TestResult]) =
    testM(label) {
      for {
        fs <- ZIO.service[FilesService.Service].provideLayer(FilesService.default)
        result <- f(fs)
        _ <- fs.clean()
      } yield result
    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("S3Downloader")(
      viaTempDirSpec()
    )

  private def readFileToContent(path: Path): Task[(String, String)] =
    Task.effect {
      path.getFileName.toString -> Files.readString(path)
    }

  private def baseDownloaderSpec(
      label: String
    )(s3Files: Map[S3FilePath, String],
      downloader: Seq[S3FilePath] => ZStream[S3Downloader with S3Client, Throwable, Path]
    )(validation: Map[String, String] => TestResult) =
    testWithFilesService(label) { filesService =>
      val s3 = s3Layer(s3Files)
      val downloaderLayer = s3 ++ ZLayer.succeed(filesService) ++ ZLayer.succeed(
        S3DownloaderConfig(10, 5, TempDir)
      ) ++ Clock.live >>> S3Downloader.live

      downloader(s3Files.keys.toSeq)
        .flatMap(p => ZStream.fromEffect(readFileToContent(p)))
        .runCollect
        .map(_.toMap)
        .map(validation)
        .provideLayer(s3 ++ downloaderLayer)
    }

  private def viaTempDirSpec() =
    baseDownloaderSpec("should correctly do download with temp dir")(
      TestFiles,
      S3Downloader.load(_)
    ) { actualLocalFiles =>
      val expectedLocalFiles = Map(
        "file1" -> "file 1 content",
        "file2" -> "file 2 content",
        "file3" -> "file 3 content"
      )
      assertTrue(actualLocalFiles == expectedLocalFiles)
    }
}
