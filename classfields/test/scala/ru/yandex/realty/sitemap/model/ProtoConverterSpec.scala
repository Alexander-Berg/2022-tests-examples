package ru.yandex.realty.sitemap.model

import ru.yandex.realty.sitemap.model.converter.{ChangeFrequencyProtoConverter, SitemapUrlProtoConverter}
import ru.yandex.realty.traffic.model.converter.StrictProtoConverter
import zio.ZIO
import zio.test._
import zio.test.junit.JUnitRunnableSpec
import eu.timepit.refined.auto._

import java.time.Instant
import java.util.Date

class ProtoConverterSpec extends JUnitRunnableSpec {

  private def protoConverterSpec[A, B](converter: StrictProtoConverter[A, B])(examples: A*) =
    testM(s"Converter ${converter.getClass.getSimpleName} should correctly convert") {
      checkAllM(Gen.fromIterable(examples)) { e =>
        ZIO.effect {
          val actual = converter.fromProto(converter.toProto(e))

          assertTrue(actual == e)
        }
      }

    }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Proto converters tests")(
      protoConverterSpec(ChangeFrequencyProtoConverter)(ChangeFrequency.values: _*),
      protoConverterSpec(SitemapUrlProtoConverter)(
        SitemapUrl(
          path = "/url/path/",
          lastMod = Date.from(Instant.now),
          changeFrequency = ChangeFrequency.Always,
          priority = 0.9,
          images = Seq("img1", "img2"),
          target = FeedTarget.YandexRentStandApartSitemap
        )
      )
    )
}
