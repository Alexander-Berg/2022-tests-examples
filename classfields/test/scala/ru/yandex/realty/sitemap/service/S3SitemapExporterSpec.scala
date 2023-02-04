package ru.yandex.realty.sitemap.service

import eu.timepit.refined.auto._
import org.junit.runner.RunWith
import ru.yandex.realty.model.url.UrlPath.UrlPath
import ru.yandex.realty.sitemap.config.DomainsConfig
import ru.yandex.realty.sitemap.model.{ChangeFrequency, FeedTarget, Sitemap, SitemapUrl}
import ru.yandex.realty.sitemap.service.SitemapExporter.SitemapExporter
import ru.yandex.realty.sitemap.service.SitemapXmlSerializer.SitemapXmlSerializer
import ru.yandex.realty.sitemap.service.live.S3SitemapExporter
import ru.yandex.realty.traffic.model.s3.S3Path.{PathPart, S3DirPath, S3FilePath}
import ru.yandex.realty.traffic.service.s3.S3
import ru.yandex.realty.traffic.service.s3.S3.S3
import ru.yandex.realty.traffic.testkit.InMemoryS3
import ru.yandex.realty.traffic.utils.FilesService.{FilesService, FilesServiceConfig}
import ru.yandex.realty.traffic.utils.distribution.Distribution.Distributed
import ru.yandex.realty.traffic.utils.{FilesService, StoredEntriesFile}
import zio._
import zio.blocking._
import zio.magic._
import zio.stream._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.junit._

import java.io._
import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.Date
import java.util.stream.Collectors
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

@RunWith(classOf[ZTestJUnitRunner])
class S3SitemapExporterSpec extends JUnitRunnableSpec {

  type WithServiceRequirements = SitemapExporter with Blocking with FilesService with S3

  case class StubXmlSerializer(filesService: FilesService.Service, blocking: Blocking.Service)
    extends SitemapXmlSerializer.Service {

    private def makePrintWriter(f: Path): PrintWriter =
      new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(f))))

    private def writeGzipLines(lines: Seq[String]) =
      filesService.tempFile("result").tap { file =>
        ZIO.bracket(Task.effect(makePrintWriter(file)))(pw => UIO.effectTotal(pw.close())) { pw =>
          blocking.effectBlocking {
            lines.foreach(pw.println)
          }
        }
      }

    override def serializeUrlsFeed(urls: Seq[SitemapUrl]): Task[Path] =
      writeGzipLines {
        urls.map(_.path.value).sorted
      }

    override def serializeIndexFile(sitemapNames: Set[String]): Task[Path] =
      writeGzipLines(sitemapNames.toSeq.sorted)
  }

  val stubXmlSerializer: URLayer[FilesService with Blocking, SitemapXmlSerializer] = (StubXmlSerializer.apply _).toLayer

  private val DefaultExporterConfig =
    S3SitemapExporter.Config(
      S3DirPath.apply("test", Seq("sitemap")),
      maxUrlsInFile = 1,
      uploadingParallelism = 1
    )

  private def filePath(name: PathPart) =
    DefaultExporterConfig.s3Dir.filePath(name)

  private def withService[A](action: RIO[WithServiceRequirements, A]): RIO[TestEnvironment, A] = {
    val layer =
      ZLayer.wireSome[TestEnvironment, WithServiceRequirements](
        InMemoryS3.live,
        SitemapExporter.s3Live,
        ZLayer.succeed(DefaultExporterConfig),
        ZLayer.succeed(FilesServiceConfig(Files.createTempDirectory("S3SitemapExporterSpec"))),
        FilesService.live,
        stubXmlSerializer,
        ZLayer.succeed(DomainsConfig("", ""))
      )

    (S3.createBucket(DefaultExporterConfig.s3Dir.bucket) *>
      action
      <* FilesService.freeAllTemporary()).provideLayer(layer)
  }

  private def urlFromPath(target: FeedTarget)(urlPath: UrlPath): SitemapUrl =
    SitemapUrl(
      path = urlPath,
      lastMod = Date.from(Instant.now),
      changeFrequency = ChangeFrequency.Daily,
      priority = 1.0,
      images = Seq.empty,
      target = target
    )

  private def createSitemap(nameToUrls: Map[FeedTarget, Seq[UrlPath]]) =
    ZIO
      .foreach(nameToUrls.toSeq) {
        case (target, urls) =>
          FilesService
            .tempFile("file")
            .flatMap {
              StoredEntriesFile.dump(_) {
                ZStream.fromIterable(urls).map(urlFromPath(target))
              }
            }
            .map(Distributed(target, _, urls.size))
      }
      .map(Sitemap)

  private def uploadSpec(
    presentedInDir: Map[PathPart, String]
  ) =
    withService {
      for {
        _ <- ZIO.foreach_(presentedInDir.toSeq) {
          case (name, content) =>
            S3.writeString(DefaultExporterConfig.s3Dir.filePath(name), content)
        }
        sitemap <- createSitemap {
          Map(
            FeedTarget.SitemapOffers -> Seq("/url1", "/url2"),
            FeedTarget.SitemapCommons -> Seq("/url3"),
            FeedTarget.YandexRentStandApartSitemap -> Seq("/url-rent") // этого сайтмапа не должно быть в разводящей
          )
        }
        preCheck <- S3.listFiles(DefaultExporterConfig.s3Dir).map { res =>
          assert(res)(hasSameElements(presentedInDir.keys.map(DefaultExporterConfig.s3Dir.filePath)))
        }
        _ <- SitemapExporter.`export`(sitemap)
        expectedFiles = Seq(
          filePath("sitemap_offers-0.xml.gz") ->
            s"""/url1""".stripMargin,
          filePath("sitemap_offers-1.xml.gz") ->
            s"""/url2""".stripMargin,
          filePath("sitemap_commons-0.xml.gz") ->
            s"""/url3""".stripMargin,
          filePath("yandex_rent_stand_apart_sitemap-0.xml.gz") ->
            s"""/url-rent""".stripMargin,
          filePath("sitemap.xml.gz") ->
            s"""sitemap_commons-0.xml
               |sitemap_offers-0.xml
               |sitemap_offers-1.xml""".stripMargin
        )
        checkFiles <- hasOnlyFilesWithContent(expectedFiles)
      } yield preCheck && checkFiles
    }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("S3SitemapExporterSpec")(
      testM("should correctly upload to empty dir") {
        uploadSpec(presentedInDir = Map.empty)
      },
      testM("should correctly upload non empty dir") {
        uploadSpec(
          presentedInDir =
            Map[PathPart, String](("sitemap.xml.gz", "index file"), ("sitemap_offers-0.xml.gz", "some file content"))
        )
      },
      testM("should correctly upload to non empty dir and delete missed") {
        uploadSpec {
          Map[PathPart, String](
            ("sitemap.xml.gz", "index file"),
            ("sitemap_offers-0.xml.gz", "some file content"),
            ("sitemap_offers-1.xml.gz", "some file content"),
            ("sitemap_offers-2.xml.gz", "some file content"),
            ("sitemap_offers-3.xml.gz", "some file content")
          )
        }
      }
    )

  private def readGzipString(is: InputStream) =
    Task.effect {
      new BufferedReader(new InputStreamReader(new GZIPInputStream(is)))
        .lines()
        .collect(Collectors.joining(System.lineSeparator()))
    }

  private def hasOnlyFilesWithContent(expected: Seq[(S3FilePath, String)]) = {
    val expectedFiles = expected.map(_._1)

    val checks: Seq[RIO[S3, TestResult]] =
      Seq(S3.listFiles(DefaultExporterConfig.s3Dir).map(assert(_)(hasSameElements(expectedFiles)))) ++
        expected.map {
          case (file, content) =>
            S3.read(file)
              .toInputStream
              .use(readGzipString)
              .map(assert(_)(equalTo(content)))
        }

    ZIO.collectAll(checks).map(_.reduce(_ && _))
  }
}
