package ru.yandex.vertis.general.feed.parser.avito.test

import general.feed.transformer.RawGeoPoint
import ru.yandex.vertis.general.feed.parser.avito.AvitoParser
import ru.yandex.vertis.general.feed.parser.testkit.XmlParserTestkit._
import zio.test.Assertion._
import zio.test._

object AvitoParserSpec extends DefaultRunnableSpec {

  def spec: Spec[_root_.zio.test.environment.TestEnvironment, TestFailure[Throwable], TestSuccess] =
    suite("AvitoParser")(
      test("Парсинг пустого файла") {
        AvitoParser.assertParsing("")(
          isParsingError(isInvalidXml("Premature end of file.") && withLocation(1, 1))
        )
      },
      test("Парсинг невалидного файла") {
        AvitoParser.assertParsing("<Unexpected content")(
          isParsingError(
            isInvalidXml("XML document structures must start and end within the same entity.") &&
              withLocation(1, 20)
          )
        )
      },
      test("Парсинг пустого валидного файла") {
        AvitoParser
          .assertParsing("""<?xml version='1.0' encoding='UTF-8'?>
              |<Ads formatVersion="3" target="Avito.ru">
              |</Ads>""".stripMargin)()
      },
      test("Парсинг объявления без обязательных полей") {
        AvitoParser
          .assertParsing("""<?xml version='1.0' encoding='UTF-8'?><Ads formatVersion="3" target="Avito.ru">
              |<Ad>
              |  <Id>abc</Id>
              |</Ad>
              |</Ads>""".stripMargin)(
            isParsingError(isMissingTag("Title") && withLocation(2, 5))
          )
      },
      test("Пропускать невалидные объявления и продолжать дальше") {
        AvitoParser
          .assertParsing("""<?xml version='1.0' encoding='UTF-8'?><Ads formatVersion="3" target="Avito.ru">
                           |<Ad>
                           |  <Id>abc</Id>
                           |</Ad>
                           |<Ad>
                           |  <Id>bbb</Id>
                           |  <Title>Hello</Title>
                           |  <Description>World</Description>
                           |  <Condition>новый</Condition>
                           |  <Price>5</Price>
                           |  <Address>Moscow</Address>
                           |  <Category>Lamoda</Category>
                           |  <Images>
                           |    <Image url="http://ya.ru"/>
                           |  </Images>
                           |</Ad>
                           |</Ads>""".stripMargin)(
            isParsingError(isMissingTag("Title") && withLocation(2, 5)),
            isParsingSuccess(anything)
          )
      },
      test("Парсить фотографии") {
        AvitoParser
          .assertParsing("""<?xml version='1.0' encoding='UTF-8'?><Ads formatVersion="3" target="Avito.ru">
                           |<Ad>
                           |  <Id>bbb</Id>
                           |  <Title>Hello</Title>
                           |  <Description>World</Description>
                           |  <Condition>новый</Condition>
                           |  <Price>5</Price>
                           |  <Address>Moscow</Address>
                           |  <Category>Lamoda</Category>
                           |</Ad>
                           |<Ad>
                           |  <Id>bbb</Id>
                           |  <Title>Hello</Title>
                           |  <Description>World</Description>
                           |  <Condition>новый</Condition>
                           |  <Price>5</Price>
                           |  <Address>Moscow</Address>
                           |  <Category>Lamoda</Category>
                           |  <Images>
                           |  </Images>
                           |</Ad>
                           |<Ad>
                           |  <Id>bbb</Id>
                           |  <Title>Hello</Title>
                           |  <Description>World</Description>
                           |  <Condition>новый</Condition>
                           |  <Price>5</Price>
                           |  <Address>Moscow</Address>
                           |  <Category>Lamoda</Category>
                           |  <Images>
                           |    <Image url="http://ya.ru"/>
                           |  </Images>
                           |</Ad>
                           |<Ad>
                           |  <Id>bbb</Id>
                           |  <Title>Hello</Title>
                           |  <Description>World</Description>
                           |  <Condition>новый</Condition>
                           |  <Price>5</Price>
                           |  <Address>Moscow</Address>
                           |  <Category>Lamoda</Category>
                           |  <Images>
                           |    <Image url="http://ya.ru"/>
                           |    <Image url="http://ya2.ru"/>
                           |    <Image url="http://ya3.ru"/>
                           |  </Images>
                           |</Ad>
                           |</Ads>""".stripMargin)(
            isParsingSuccess(hasImages(isEmpty)),
            isParsingSuccess(hasImages(isEmpty)),
            isParsingSuccess(hasImages(hasSize(equalTo(1)))),
            isParsingSuccess(
              hasImages(
                hasElements(
                  hasUrl(equalTo("http://ya.ru")),
                  hasUrl(equalTo("http://ya2.ru")),
                  hasUrl(equalTo("http://ya3.ru"))
                )
              )
            )
          )
      },
      test("Парсить геопозицию") {
        AvitoParser
          .assertParsing("""<?xml version='1.0' encoding='UTF-8'?><Ads formatVersion="3" target="Avito.ru">
              |<Ad>
              |  <Id>bbb</Id>
              |  <Title>Hello</Title>
              |  <Description>World</Description>
              |  <Condition>новый</Condition>
              |  <Price>5</Price>
              |  <Latitude>45.0503002</Latitude>
              |  <Longitude>38.9584973</Longitude>
              |  <Category>Lamoda</Category>
              |</Ad>
              |</Ads>""".stripMargin)(
            isParsingSuccess(hasGeoPoint(equalTo(Some(RawGeoPoint(45.0503002, 38.9584973)))))
          )
      },
      testM("Примеры фидов из документации") {
        AvitoParser.checkSamples(
          "general/feed/parser/avito/samples/dlya_biznesa.xml",
          "general/feed/parser/avito/samples/dlya_doma_i_dachi.xml",
          "general/feed/parser/avito/samples/electronics.xml",
          "general/feed/parser/avito/samples/goods.xml",
          "general/feed/parser/avito/samples/hobby.xml",
          "general/feed/parser/avito/samples/parts.xml",
          "general/feed/parser/avito/samples/pets.xml",
          "general/feed/parser/avito/samples/phones.xml",
          "general/feed/parser/avito/samples/rabota.xml",
          "general/feed/parser/avito/samples/uslugi.xml",
          "general/feed/parser/avito/samples/water_transport.xml"
        )
      },
      testM("Примеры невалидных фидов") {
        AvitoParser.checkSamples(
          "general/feed/parser/avito/samples/errors/invalid_entity.xml",
          "general/feed/parser/avito/samples/errors/mismatch_tag.xml"
        )
      }
    )
}
