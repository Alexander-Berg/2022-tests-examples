package ru.yandex.auto.extdata.jobs.sitemap.service

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.PutObjectResult
import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.app.service.FilesService
import ru.yandex.auto.extdata.jobs.sitemap.config.DomainsConfig
import ru.yandex.auto.extdata.jobs.sitemap.model.{SitemapFeed, SitemapRow, SitemapTarget}
import ru.yandex.auto.extdata.jobs.sitemap.services.impl.S3SitemapExporter
import ru.yandex.auto.extdata.jobs.sitemap.services.{SitemapExporter, XmlSerializer}
import ru.yandex.auto.extdata.jobs.sitemap.utils.SitemapUtils
import ru.yandex.common.util.IOUtils
import ru.yandex.vertis.mockito.MockitoSupport
import zio.ZLayer
import zio.clock.Clock
import zio.magic._
import zio.stream.ZStream

import java.io.{ByteArrayOutputStream, File, FileInputStream, InputStream}
import java.nio.file.Paths
import java.time.Instant
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import java.util.zip.{GZIPInputStream, ZipInputStream}

@RunWith(classOf[JUnitRunner])
class SitemapExporterSpec extends WordSpec with Matchers with MockitoSupport {

  private def withStubS3(f: AmazonS3 => Any): Map[(String, String), File] = {
    val s3 = mock[AmazonS3]

    val result = scala.collection.mutable.Map.empty[(String, String), File]
    when(s3.putObject(?[String], ?[String], ?[File])).thenAnswer {
      new Answer[PutObjectResult] {
        override def answer(invocationOnMock: InvocationOnMock): PutObjectResult = {
          val bucket = invocationOnMock.getArgument[String](0)
          val key = invocationOnMock.getArgument[String](1)
          val file = invocationOnMock.getArgument[File](2)

          result.update((bucket, key), file)

          new PutObjectResult
        }
      }
    }

    f(s3)

    result.toMap
  }

  private def makeFeed(target: SitemapTarget, index: Int, content: String): SitemapFeed =
    SitemapFeed(
      target = target,
      index = index,
      urlsCount = 0,
      gzipContent = IOUtils.gzip(content.getBytes("UTF-8"))
    )

  "S3SitemapExporter" should {

    "correctly export files" in {

      val feeds = Seq(
        makeFeed(
          target = SitemapTarget.OffersCars,
          index = 1,
          content = """url2
                      |url3
                      |""".stripMargin
        ),
        makeFeed(
          target = SitemapTarget.OffersCars,
          index = 2,
          content = """url4
                      |""".stripMargin
        ),
        makeFeed(
          target = SitemapTarget.CostPlusAll,
          index = 1,
          content = """url1
                      |url5
                      |""".stripMargin
        ),
        makeFeed(
          target = SitemapTarget.StaticPages,
          index = 1,
          content = """https://desktop.host/path1
                      |https://desktop.host/path2
                      |""".stripMargin
        )
      )

      val config =
        S3SitemapExporter.Config(
          bucket = "export-bucket",
          prefix = "key/to/export/",
          desktopIndexUrlPrefix = "https://desktop.host/",
          mobileIndexUrlPrefix = "https://mobile.host/"
        )

      val tmpDir = Paths.get("/tmp/SitemapExporterSpec/")

      val exported = withStubS3 { s3 =>
        zio.Runtime.default.unsafeRun {
          ZStream
            .fromIterable(feeds)
            .run(SitemapExporter.exportSink)
            .inject(
              ZLayer.succeed(s3),
              ZLayer.succeed(config),
              ZLayer.succeed(tmpDir) >>> FilesService.live,
              SitemapExporter.s3Live,
              Clock.live,
              ZLayer.succeed[XmlSerializer.Service](XmlSerializerStub),
              ZLayer.succeed(DomainsConfig("desktop.host"))
            )
        }
      }

      exported should have size 1

      val desktopExported = (config.bucket, config.prefix + SitemapUtils.AutoruSitemapArchiveFileName)

      (exported should contain).key(desktopExported)

      shouldBeZipWithContent(
        exported(desktopExported)
      )(
        Map(
          "sitemap_offers_cars_1.xml.gz" -> shouldHaveGzipContent {
            """url2
              |url3
              |""".stripMargin
          },
          "sitemap_offers_cars_2.xml.gz" -> shouldHaveGzipContent {
            """url4
              |""".stripMargin
          },
          "sitemapindex.xml" -> shouldHaveContent {
            """https://desktop.host/sitemap_cost_plus_all_1.xml.gz
              |https://desktop.host/sitemap_offers_cars_1.xml.gz
              |https://desktop.host/sitemap_offers_cars_2.xml.gz
              |https://desktop.host/sitemap_static_pages_1.xml.gz
              |""".stripMargin
          },
          "sitemap_cost_plus_all_1.xml.gz" -> shouldHaveGzipContent {
            """url1
              |url5
              |""".stripMargin
          },
          "sitemap_static_pages_1.xml.gz" -> shouldHaveGzipContent {
            """https://desktop.host/path1
              |https://desktop.host/path2
              |""".stripMargin
          }
        )
      )

      FileUtils.deleteDirectory(tmpDir.toFile)
    }
  }

  object XmlSerializerStub extends XmlSerializer.Service {

    override def serializeSitemap(entries: Seq[SitemapRow]): String =
      entries.map(_.url).mkString("", System.lineSeparator(), System.lineSeparator())

    override def serializeIndexFile(names: Seq[String], lastMod: Instant, urlPrefix: String): String =
      names.map(urlPrefix + _).mkString("", System.lineSeparator(), System.lineSeparator())
  }

  private def shouldBeZipWithContent(
      file: File
  )(assertions: Map[String, InputStream => Unit]): Unit = {

    val in = new ZipInputStream(new FileInputStream(file))
    val entry = new AtomicReference(in.getNextEntry)
    val checked = new AtomicLong(0)

    while (entry.get() != null) {
      withClue("zip should contain on of expected") {
        (assertions should contain).key(entry.get().getName)
      }

      withClue(s"On ${entry.get().getName}") {
        assertions(entry.get().getName)(in)
      }

      in.closeEntry()
      entry.set(in.getNextEntry)

      checked.incrementAndGet()
    }

    checked.get() shouldBe assertions.size
  }

  private def shouldHaveGzipContent(expected: String)(in: InputStream) =
    shouldHaveContent(expected)(new GZIPInputStream(in))

  private def shouldHaveContent(expected: String)(in: InputStream) = {
    val baos = new ByteArrayOutputStream()

    var c = in.read()
    while (c != -1) {
      baos.write(c)
      c = in.read()
    }

    val content = baos.toString("UTF-8")

    content shouldBe expected
  }

}
