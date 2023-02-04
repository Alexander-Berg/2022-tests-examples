package ru.auto.cabinet.reporting.pdf

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.security.MessageDigest

import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.Ignore
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import PdfResources._

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class HtmlToPdfConverterTest
    extends AnyFlatSpecLike
    with Matchers
    with ScalaFutures {

  it should "generate pdf" in {

    val html = readResourceFile("html-source.html")
    val converter = new HtmlToPdfConverter(
      PdfGenContext("phantomjs --ignore-ssl-errors=true", pdfRenderConfig))
    val result: PdfGenResult = converter.renderPdf(html)

    val pdfBytes = result.result
      .futureValue(timeout(Span(20, Seconds)), interval(Span(500, Millis)))
    val document = PDDocument.load(new ByteArrayInputStream(pdfBytes))

    try {
      val pages = document.getDocumentCatalog.getAllPages.asScala
        .map(_.asInstanceOf[PDPage])

      pages.size shouldBe 5

      val expectedPageHashes = Seq(
        "c04fe9efb2614b37b140d9a88fcc025d",
        "0411f804cf9a43ab6754bcc1b29966fc",
        "e9823dd0dcf4bf7e70f8b8c59b50736a",
        "5567ecb6dcb08c9f42ce936de6f01c96",
        "10fc5481b31064f65865236aa5641c27"
      )

      // checking screenshot hashes, because hash of pdfs constantly changes
      pages.map(page => md5(screenshot(page))) shouldBe expectedPageHashes

    } finally document.close()
  }

  private def screenshot(page: PDPage) = {
    val bufferedImage = page.convertToImage()
    val output = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, "jpg", output)
    output
  }

  private def md5(output: ByteArrayOutputStream) = {
    MessageDigest
      .getInstance("MD5")
      .digest(output.toByteArray)
      .map("%02x".format(_))
      .mkString
  }
}
