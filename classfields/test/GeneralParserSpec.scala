package ru.yandex.vertis.general.feed.parser.general.test

import general.feed.transformer.{RawAddress, RawAttribute, RawContactMethod, RawContacts, RawGeoPoint, RawSeller}
import ru.yandex.vertis.general.feed.parser.general.{GeneralOffer, GeneralParser}
import zio.test.{DefaultRunnableSpec, Spec, TestFailure, TestSuccess}
import ru.yandex.vertis.general.feed.parser.testkit.XmlParserTestkit.{
  hasAddress,
  hasAttribute,
  hasElements,
  hasGeoPoint,
  hasImages,
  hasSeller,
  hasUrl,
  isInvalidXml,
  isMissingTag,
  isParsingError,
  isParsingSuccess,
  withLocation,
  RichParserTestkit
}
import zio.test.Assertion.{anything, equalTo, hasSize, isEmpty}

object GeneralParserSpec extends DefaultRunnableSpec {

  def spec: Spec[_root_.zio.test.environment.TestEnvironment, TestFailure[Throwable], TestSuccess] =
    suite("GeneralParser")(
      test("Парсинг пустого файла") {
        GeneralParser.assertParsing("")(
          isParsingError(isInvalidXml("Premature end of file.") && withLocation(1, 1))
        )
      },
      test("Парсинг невалидного файла") {
        GeneralParser.assertParsing("<Unexpected content")(
          isParsingError(
            isInvalidXml("XML document structures must start and end within the same entity.") &&
              withLocation(1, 20)
          )
        )
      },
      test("Парсинг пустого валидного файла") {
        GeneralParser
          .assertParsing("""<?xml version="1.0" encoding="utf-8"?>
                           |<feed version="1"></feed>""".stripMargin)()
      },
      test("Парсинг объявления без обязательных полей") {
        GeneralParser
          .assertParsing("""<?xml version="1.0" encoding="UTF-8"?><feed version="1">
                           |<offers>
                           | <offer>
                           |   <id>abc</id>
                           | </offer>
                           |</offers>
                           |</feed>""".stripMargin)(
            isParsingError(isMissingTag("title") && withLocation(3, 9))
          )
      },
      test("Пропускать невалидные объявления и продолжать дальше") {
        GeneralParser
          .assertParsing("""<?xml version="1.0" encoding="UTF-8"?><feed version="1">
                           |<offers>
                           | <offer>
                           |  <id>value</id>
                           | </offer>
                           | <offer>
                           |   <id>abc</id>
                           |   <title>value</title>
                           |   <category>value</category>
                           |   <seller>
                           |                <contacts>
                           |                    <phone>+7-777-777-77-77</phone>
                           |                    <contact-method>only-phone</contact-method>
                           |                </contacts>
                           |                <locations>
                           |                    <location>
                           |                        <address>Россия, Москва, Тверская улица</address>
                           |                    </location>
                           |                    <location>
                           |                         <geopoint>
                           |                             <longitude>38.9584973</longitude>
                           |                             <latitude>37.606397</latitude>
                           |                         </geopoint>
                           |                    </location>
                           |                </locations>
                           |   </seller>
                           | </offer>
                           |</offers>
                           |</feed>""".stripMargin)(
            isParsingError(isMissingTag("title") && withLocation(3, 9)),
            isParsingSuccess(anything)
          )
      },
      test("Парсить фотографии") {
        GeneralParser
          .assertParsing("""<?xml version="1.0" encoding="UTF-8"?><feed version="1">
                           |<offers>
                           | <offer>
                           |   <id>abc</id>
                           |   <title>value</title>
                           |   <category>value</category>
                           |   <seller>
                           |                <contacts>
                           |                    <phone>+7-777-777-77-77</phone>
                           |                    <contact-method>only-phone</contact-method>
                           |                </contacts>
                           |                <locations>
                           |                    <location>
                           |                        <address>Россия, Москва, Тверская улица</address>
                           |                    </location>
                           |                    <location>
                           |                         <geopoint>
                           |                             <longitude>38.9584973</longitude>
                           |                             <latitude>37.606397</latitude>
                           |                         </geopoint>
                           |                    </location>
                           |                </locations>
                           |   </seller>
                           |   <images></images>
                           | </offer>
                           | <offer>
                           |   <id>abc</id>
                           |   <title>value</title>
                           |   <category>value</category>
                           |   <seller>
                           |                <contacts>
                           |                    <phone>+7-777-777-77-77</phone>
                           |                    <contact-method>only-phone</contact-method>
                           |                </contacts>
                           |                <locations>
                           |                    <location>
                           |                        <address>Россия, Москва, Тверская улица</address>
                           |                    </location>
                           |                    <location>
                           |                         <geopoint>
                           |                             <longitude>38.9584973</longitude>
                           |                             <latitude>37.606397</latitude>
                           |                         </geopoint>
                           |                    </location>
                           |                </locations>
                           |   </seller>
                           | </offer>
                           |  <offer>
                           |   <id>abc</id>
                           |   <title>value</title>
                           |   <category>value</category>
                           |   <seller>
                           |                <contacts>
                           |                    <contact-method>only-phone</contact-method>
                           |                    <phone>+7-777-777-77-77</phone>
                           |                </contacts>
                           |                <locations>
                           |                    <location>
                           |                        <address>Россия, Москва, Тверская улица</address>
                           |                    </location>
                           |                    <location>
                           |                         <geopoint>
                           |                             <longitude>38.9584973</longitude>
                           |                             <latitude>37.606397</latitude>
                           |                         </geopoint>
                           |                    </location>
                           |                </locations>
                           |   </seller>
                           |    <images>
                           |       <image>http://ya.ru</image>
                           |    </images>
                           | </offer>
                           | <offer>
                           |   <id>abc</id>
                           |   <title>value</title>
                           |   <category>value</category>
                           |   <seller>
                           |                <contacts>
                           |                    <contact-method>only-phone</contact-method>
                           |                    <phone>+7-777-777-77-77</phone>
                           |                </contacts>
                           |                <locations>
                           |                    <location>
                           |                        <address>Россия, Москва, Тверская улица</address>
                           |                    </location>
                           |                    <location>
                           |                         <geopoint>
                           |                             <longitude>38.9584973</longitude>
                           |                             <latitude>37.606397</latitude>
                           |                         </geopoint>
                           |                    </location>
                           |                </locations>
                           |   </seller>
                           |    <images>
                           |       <image>http://ya.ru</image>
                           |       <image>http://ya2.ru</image>
                           |       <image>http://ya3.ru</image>
                           |    </images>
                           | </offer>
                           |</offers>
                           |</feed>""".stripMargin)(
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
        GeneralParser
          .assertParsing("""<?xml version="1.0" encoding="UTF-8"?><feed version="1">
                                 |<offers>
                                 | <offer>
                                 |   <id>abc</id>
                                 |   <title>value</title>
                                 |   <category>value</category>
                                 |   <seller>
                                 |                <contacts>
                                 |                    <phone>+7-777-777-77-77</phone>
                                 |                    <contact-method>only-phone</contact-method>
                                 |                </contacts>
                                 |                <locations>
                                 |                    <location>
                                 |                         <geopoint>
                                 |                             <longitude>38.9584973</longitude>
                                 |                             <latitude>37.606397</latitude>
                                 |                         </geopoint>
                                 |                    </location>
                                 |                </locations>
                                 |   </seller>
                                 | </offer>
                                 |</offers>
                                 |</feed>""".stripMargin)(
            isParsingSuccess(hasGeoPoint(equalTo(Some(RawGeoPoint(latitude = 37.606397, longitude = 38.9584973)))))
          )
      },
      test("Парсить адресса") {
        GeneralParser
          .assertParsing("""<?xml version="1.0" encoding="UTF-8"?><feed version="1">
                           |<offers>
                           | <offer>
                           |   <id>abc</id>
                           |   <title>value</title>
                           |   <category>value</category>
                           |   <seller>
                           |                <contacts>
                           |                    <phone>+7-777-777-77-77</phone>
                           |                    <contact-method>only-phone</contact-method>
                           |                </contacts>
                           |                <locations>
                           |                  <location>
                           |                        <address>Россия, Москва, Тверская улица</address>
                           |                   </location>
                           |                </locations>
                           |   </seller>
                           | </offer>
                           |</offers>
                           |</feed>""".stripMargin)(
            isParsingSuccess(hasAddress(equalTo(Some(RawAddress("Россия, Москва, Тверская улица")))))
          )
      },
      test("Парсить продавца") {
        GeneralParser
          .assertParsing("""<?xml version="1.0" encoding="UTF-8"?><feed version="1">
                           |<offers>
                           | <offer>
                           |   <id>abc</id>
                           |   <title>value</title>
                           |   <category>value</category>
                           |   <seller>
                           |                <contacts>
                           |                    <phone>+7-777-777-77-77</phone>
                           |                    <contact-method>only-phone</contact-method>
                           |                </contacts>
                           |                <locations>
                           |                  <location>
                           |                        <address>Россия, Москва, Тверская улица</address>
                           |                   </location>
                           |                </locations>
                           |   </seller>
                           | </offer>
                           |</offers>
                           |</feed>""".stripMargin)(
            isParsingSuccess(
              hasSeller(
                equalTo(
                  Some(
                    RawSeller(
                      contacts = Some(
                        RawContacts(
                          phone = "+7-777-777-77-77",
                          contactMethod = RawContactMethod.ContactMethod.ONLY_PHONE
                        )
                      ),
                      locations = Seq(RawAddress("Россия, Москва, Тверская улица"))
                    )
                  )
                )
              )
            )
          )
      },
      test("Парсить атрибуты") {
        GeneralParser
          .assertParsing("""<?xml version="1.0" encoding="UTF-8"?><feed version="1">
                                 |<offers>
                                 | <offer>
                                 |   <id>abc</id>
                                 |   <title>Titlr</title>
                                 |   <category>Новое</category>
                                 |   <seller>
                                 |                <contacts>
                                 |                    <phone>+7-777-777-77-77</phone>
                                 |                    <contact-method>only-phone</contact-method>
                                 |                </contacts>
                                 |                <locations>
                                 |                  <location>
                                 |                        <address>Россия, Москва, Тверская улица</address>
                                 |                   </location>
                                 |                </locations>
                                 |   </seller>
                                 |   <images><image>http-example</image></images>
                                 |   <price>1000</price>
                                 |   <attributes>
                                 |       <attribute name="Цвет">Красный</attribute>
                                 |        <attribute name="Размер">46</attribute>
                                 |    </attributes>
                                 | </offer>
                                 |</offers>
                                 |</feed>""".stripMargin)(
            isParsingSuccess(
              hasAttribute(
                equalTo(
                  Seq(
                    RawAttribute(attribute = "Цвет", value = "Красный"),
                    RawAttribute(attribute = "Размер", value = "46")
                  )
                )
              )
            )
          )
      }
    )
}
