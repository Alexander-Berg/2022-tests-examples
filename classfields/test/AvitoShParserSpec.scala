package ru.yandex.vertis.general.feed.parser.avitosh.test

import general.feed.transformer.{ErrorCode, FeedFormat, ParsingError, RawOffer}
import ru.yandex.vertis.general.feed.parser.avitosh.AvitoShParser
import zio.Chunk
import zio.test.Assertion._
import zio.test._

import java.io.File
import java.nio.charset.StandardCharsets
import scala.io.Source
import scala.util.Using

object AvitoShParserSpec extends DefaultRunnableSpec {

  def spec =
    suite("AvitoShParser")(
      testM("Парсинг рамдомных фидов") {
        val files = new File("./general/feed/parser/avitosh/test/samples").listFiles().toList
        val jsonFiles = files.filter(_.getAbsolutePath.endsWith(".json"))
        val jsonWithExpected = jsonFiles.map { f =>
          val expected = files
            .find(_.getAbsolutePath == f.getAbsolutePath + ".expected")
            .getOrElse(throw new RuntimeException(s"No expectancy defined for $f"))
          f -> expected
        }
        scala.Predef.assert(jsonWithExpected.nonEmpty)

        checkAllM(Gen.fromIterable(jsonWithExpected)) { case (source, expectedFile) =>
          val expectedJsonStr = Using(Source.fromFile(expectedFile)(StandardCharsets.UTF_8))(_.mkString).get
          val expected = scalapb_circe.JsonFormat.fromJsonString[RawOffer](expectedJsonStr)

          AvitoShParser
            .parse(source, FeedFormat.AVITO_SH)
            .runCollect
            .map(assert(_)(equalTo(Chunk(expected))))
        }
      },
      testM("return readable error from failed parsing") {
        AvitoShParser
          .parse(new File("./general/feed/parser/avitosh/test/samples/invalid-file.failedjson"), FeedFormat.AVITO_SH)
          .runCollect
          .flip
          .map(
            assert(_)(
              equalTo(
                ParsingError(
                  FeedFormat.AVITO_SH,
                  fatal = true,
                  code = ErrorCode.INVALID_FILE,
                  customDescription = Some(
                    "Attempt to decode value on failed cursor: DownField(listing_id)," +
                      "DownArray,DownField(offers),DownField(avito_sh_profile)"
                  )
                )
              )
            )
          )
      }
    )
}
