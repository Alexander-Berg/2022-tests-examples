package ru.vertistraf.cost_plus.exporter.printer

import zio.stream.{ZStream, ZTransducer}
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Has, ULayer, ZLayer}

import java.io.PrintWriter
import java.nio.charset.{Charset, StandardCharsets}

object XmlPrinterSpec extends DefaultRunnableSpec {

  object SpecPrinter extends XmlPrinter[Seq[String]] {

    override def print(e: Seq[String])(pw: PrintWriter): Unit = {
      pw.println("<seq>")
      e.foreach { elem =>
        pw.println(s"  <elem>$elem</elem>")
      }
      pw.println("</seq>")
    }

    val layer: ULayer[Has[XmlPrinter[Seq[String]]]] = ZLayer.succeed(this)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("XmlPrinter")(
      testM("should correctly return gzip bytes") {

        val input = Seq("mama", "papa", "dada", "кириллица")

        val expected =
          """<seq>
            |  <elem>mama</elem>
            |  <elem>papa</elem>
            |  <elem>dada</elem>
            |  <elem>кириллица</elem>
            |</seq>
            |""".stripMargin

        XmlPrinter
          .printGzipBytes(input, StandardCharsets.UTF_8)
          .flatMap(array => ZStream.fromIterator(array.iterator).transduce(ZTransducer.gunzip()).runCollect)
          .map(x => new String(x.toArray, StandardCharsets.UTF_8))
          .map(actual => assertTrue(actual == expected))
      },
      testM("should correctly return gzip bytes (large)") {

        val input: Seq[String] = (0 until 1000000).map(_.toString)

        val expectedLinesCount = input.size + 2

        XmlPrinter
          .printGzipBytes(input, Charset.forName("UTF-8"))
          .flatMap(array => ZStream.fromIterator(array.iterator).transduce(ZTransducer.gunzip()).runCollect)
          .map { bytes =>
            val actualLinesCount =
              new String(bytes.toArray, StandardCharsets.UTF_8).split(System.lineSeparator()).length

            assertTrue(actualLinesCount == expectedLinesCount)
          }
      }
    ).provideLayer(SpecPrinter.layer ++ ZLayer.requires[TestEnvironment])

}
