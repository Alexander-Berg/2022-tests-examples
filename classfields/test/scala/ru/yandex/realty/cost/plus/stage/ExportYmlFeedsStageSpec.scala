package ru.yandex.realty.cost.plus.stage

import org.joda.time.Instant
import org.junit.runner.RunWith
import ru.yandex.realty.cost.plus.config.DomainConfig
import ru.yandex.realty.cost.plus.service.stage.CostPlusProcessingStage
import ru.yandex.realty.cost.plus.service.stage.live.ExportYmlFeedsStage
import ru.yandex.realty.traffic.utils.FilesService
import FilesService.{FilesService, FilesServiceConfig}
import ru.yandex.realty.traffic.model.s3.S3Path
import zio._
import zio.clock.Clock
import zio.duration.Duration
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.junit.{JUnitRunnableSpec, ZTestJUnitRunner}

import java.nio.file._
import java.time.{DateTimeException, OffsetDateTime}
import java.util.concurrent.TimeUnit
import eu.timepit.refined.auto._
import ru.yandex.realty.cost.plus.service.stage.CostPlusProcessingStage.CostPlusProcessingStage
import ru.yandex.realty.traffic.model.s3.S3Path.{PathPart, S3DirPath, S3FilePath}
import ru.yandex.realty.traffic.service.s3.S3
import ru.yandex.realty.traffic.service.s3.S3.S3
import ru.yandex.realty.traffic.testkit.InMemoryS3
import zio.blocking.Blocking
import zio.test.TestAspect.sequential

@RunWith(classOf[ZTestJUnitRunner])
class ExportYmlFeedsStageSpec extends JUnitRunnableSpec {

  private val s3Dir =
    S3Path.S3DirPath("bucket", Seq("some", "dir"))

  private val domainConfig =
    DomainConfig("desktop.host")

  private def indexFileContent(names: String*) =
    names.sorted
      .map {
        s"https://desktop.host/cost-plus/" + _
      }
      .mkString("", System.lineSeparator(), System.lineSeparator())

  type BaseSpecEnv =
    S3 with Has[Path] with CostPlusProcessingStage[Seq[Path], Unit]

  private def createFile(name: String, content: String): RIO[Has[Path], Path] =
    ZIO.service[Path].flatMap { p =>
      Task.effect(Files.writeString(p.resolve(name), content))
    }

  private def createAndUploadFile(s3Dir: S3DirPath, fileName: PathPart, fileContent: String) =
    createFile(fileName.value, fileContent) >>= (S3.writeFile(s3Dir.filePath(fileName), _))

  private def runStage(files: Seq[Path]) =
    CostPlusProcessingStage.process[Seq[Path], Unit](files)

  private def baseSpecEnv: RLayer[FilesService, BaseSpecEnv] = {
    val s3: ULayer[S3] = (Blocking.live >>> InMemoryS3.live)
      .tap(_.get.createBucket(s3Dir.bucket).orDie)

    val tempDir = FilesService.newTempDirectory("spec-temp-dir").toLayer

    val stage =
      ZLayer.succeed[Clock.Service](DummyClock) ++
        ZLayer.succeed(domainConfig) ++
        ZLayer.requires[FilesService] ++
        s3 ++
        ZLayer.succeed(ExportYmlFeedsStage.wrapPath(s3Dir)) >>> ExportYmlFeedsStage.live

    s3 ++ tempDir ++ stage
  }

  private def baseSpec[A](
    spec: => RIO[BaseSpecEnv, A]
  ): Task[A] =
    for {
      cfg <- Task.effect(Files.createTempDirectory(s"files-service")).map(FilesServiceConfig(_))
      fs <- ZIO.service[FilesService.Service].provideLayer(ZLayer.succeed(cfg) >>> FilesService.live)
      res <- spec.provideLayer(ZLayer.succeed(fs) >>> baseSpecEnv)
      _ <- fs.freeAllTemporary()
    } yield res

  private def failOnEmptyInputSpec() =
    testM("should fail on empty input") {
      baseSpec {
        assertM {
          runStage(Seq.empty).run
        }(dies(hasMessage(equalTo("Come empty files!"))))
      }
    }

  private def assertDirHasOnlyFiles(dir: S3DirPath)(hasOnly: Map[S3FilePath, String]) = {
    S3.list(dir)
      .map(assert(_)(hasSameElements(hasOnly.keys)))
      .flatMap { res =>
        if (res.isFailure) {
          UIO.effectTotal(res)
        } else {

          ZIO
            .foreach(hasOnly.toSeq) {
              case (key, content) =>
                S3.read(key)
                  .runCollect
                  .map(a => new String(a.toArray, "UTF-8"))
                  .map { actual =>
                    assert(actual)(equalTo[String, String](content).label(s"on key $key"))
                  }
            }
            .map(_.reduce(_ && _))

        }
      }
  }

  private def correctlyUploadToEmptyDirSpec() =
    testM("should correctly upload to empty dir") {
      baseSpec {
        for {
          f1 <- createFile("flats_0.yml", "Flats 0 content")
          f2 <- createFile("flats_1.yml", "Flats 1 content")
          f3 <- createFile("builds_0.yml", "Builds 0 content")
          _ <- runStage(Seq(f1, f2, f3))
          result <- assertDirHasOnlyFiles(s3Dir)(
            Map(
              s3Dir.filePath("flats_0.yml") -> "Flats 0 content",
              s3Dir.filePath("flats_1.yml") -> "Flats 1 content",
              s3Dir.filePath("builds_0.yml") -> "Builds 0 content",
              s3Dir.filePath("index.txt") -> indexFileContent("flats_0.yml", "flats_1.yml", "builds_0.yml"),
              s3Dir.filePath("fake-index.txt") -> ""
            )
          )
        } yield result
      }
    }

  private def correctlyUploadToNonEmptyDirSpec() =
    testM("should correctly upload to non empty dir") {
      baseSpec {
        for {
          _ <- createAndUploadFile(s3Dir, "flats_0.yml", "Flats 0 content old")
          _ <- createAndUploadFile(s3Dir, "flats_1.yml", "Flats 1 content old")
          _ <- createAndUploadFile(s3Dir, "builds_0.yml", "Builds 0 content old")

          f1 <- createFile("flats_0.yml", "Flats 0 content")
          f2 <- createFile("flats_1.yml", "Flats 1 content")
          f3 <- createFile("builds_0.yml", "Builds 0 content")
          idxFileContent <- UIO.effectTotal(indexFileContent("flats_0.yml", "flats_1.yml", "builds_0.yml"))
          _ <- runStage(Seq(f1, f2, f3))
          result <- assertDirHasOnlyFiles(s3Dir)(
            Map(
              s3Dir.filePath("flats_0.yml") -> "Flats 0 content",
              s3Dir.filePath("flats_1.yml") -> "Flats 1 content",
              s3Dir.filePath("builds_0.yml") -> "Builds 0 content",
              s3Dir.filePath("index.txt") -> idxFileContent,
              s3Dir.filePath("fake-index.txt") -> ""
            )
          )
        } yield result
      }
    }

  private def correctlyUploadToNonEmptyDirWithFakeSpec() =
    testM("should correctly upload to non empty dir with fake yml") {
      baseSpec {
        for {
          _ <- createAndUploadFile(s3Dir, "flats_0.yml", "Flats 0 content old")
          _ <- createAndUploadFile(s3Dir, "flats_1.yml", "Flats 1 content old")
          _ <- createAndUploadFile(s3Dir, "builds_0.yml", "Builds 0 content old")
          _ <- createAndUploadFile(s3Dir, "builds_1.yml", "Builds 1 content old")
          f1 <- createFile("flats_0.yml", "Flats 0 content")
          f2 <- createFile("flats_1.yml", "Flats 1 content")
          f3 <- createFile("builds_0.yml", "Builds 0 content")
          idxFileContent <- UIO.effectTotal(indexFileContent("flats_0.yml", "flats_1.yml", "builds_0.yml"))
          fakeIdxFileContent <- UIO.effectTotal(indexFileContent("builds_1.yml"))
          expectedFake <- UIO.effectTotal {
            ExportYmlFeedsStage.fakeFeedContent("builds1yml", Instant.ofEpochMilli(0).toString)
          }
          _ <- runStage(Seq(f1, f2, f3))
          result <- assertDirHasOnlyFiles(s3Dir)(
            Map(
              s3Dir.filePath("flats_0.yml") -> "Flats 0 content",
              s3Dir.filePath("flats_1.yml") -> "Flats 1 content",
              s3Dir.filePath("builds_0.yml") -> "Builds 0 content",
              s3Dir.filePath("index.txt") -> idxFileContent,
              s3Dir.filePath("builds_1.yml") -> expectedFake,
              s3Dir.filePath("fake-index.txt") -> fakeIdxFileContent
            )
          )
        } yield result
      }
    }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ExportYmlFeedsStage")(
      failOnEmptyInputSpec(),
      correctlyUploadToEmptyDirSpec(),
      correctlyUploadToNonEmptyDirSpec(),
      correctlyUploadToNonEmptyDirWithFakeSpec()
    ) @@ sequential

}

object DummyClock extends Clock.Service {
  override def currentTime(unit: TimeUnit): UIO[Long] = UIO.succeed(0)

  override def currentDateTime: IO[DateTimeException, OffsetDateTime] =
    IO.fail(new DateTimeException("Not implemented"))

  override def nanoTime: UIO[Long] = UIO.succeed(0)

  override def sleep(duration: Duration): UIO[Unit] = UIO.unit
}
