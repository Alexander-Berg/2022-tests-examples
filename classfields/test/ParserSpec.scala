package ru.yandex.vertis.general.feed.processor.parser.test

import java.io.{ByteArrayInputStream, File}
import java.nio.file.Files
import org.apache.commons.io.{FileUtils, IOUtils}
import ru.yandex.vertis.general.feed.parser.FeedParser
import ru.yandex.vertis.general.feed.parser.offer.Location._
import ru.yandex.vertis.general.feed.parser.offer.{
  Attribute,
  Attributes,
  Condition,
  ContactMethod,
  Contacts,
  DeliveryInfo,
  Image,
  Images,
  Locations,
  Offer,
  Seller
}
import zio.{Chunk, ZIO}
import zio.test.Assertion._
import zio.test._

object ParserSpec extends DefaultRunnableSpec {

  private def getResourceFile(path: String): File = {
    val file = Files.createTempFile(null, null).toFile
    file.deleteOnExit()
    FileUtils.copyInputStreamToFile(getClass.getResourceAsStream(path), file)
    file
  }

  private def saveStringToFile(str: String): File = {
    val file = Files.createTempFile(null, null).toFile
    file.deleteOnExit()
    FileUtils.copyInputStreamToFile(new ByteArrayInputStream(str.getBytes), file)
    file
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultParser")(
      testM("validate xsd") {
        val from = getResourceFile("/feed.xml")
        for {
          _ <- FeedParser.validateXsd(from)
        } yield assertCompletes
      },
      testM("валидация схемы должна быть защищен от ООМа") {
        val from = getResourceFile("/xxe_oom.xml")
        for {
          exit <- FeedParser.validateXsd(from).run
        } yield assert(exit)(
          fails(
            hasField(
              "reason",
              _.message,
              matchesRegex(
                "[\\s\\S]*The parser has encountered more than .* entity expansions in this document[\\s\\S]*"
              )
            )
          )
        )
      },
      testM("парсинг xml должен быть защищен от ООМа") {
        val from = getResourceFile("/xxe_oom.xml")
        for {
          _ <- FeedParser.parseOffersStream(from).runCollect
        } yield assertCompletes // because it should completely ignore references
      },
      testM("валидация схемы не должна читать внешние сущности") {
        val secretFile = saveStringToFile("secret string")
        val badXmlString = IOUtils
          .toString(getClass.getResourceAsStream("/feed_with_injection.xml"), "UTF-8")
          .replace("!{}", secretFile.getAbsolutePath)
        val xmlFile = saveStringToFile(badXmlString)
        for {
          exit <- FeedParser.validateXsd(xmlFile).run
        } yield assert(exit)(
          fails(
            hasField(
              "reason",
              _.message,
              matchesRegex(
                "[\\s\\S]*Failed to read external document .*, because 'file' access is not allowed due to restriction set by the accessExternalDTD property.[\\s\\S]*"
              )
            )
          )
        )
      },
      testM("парсинг xml не должен читать внешние сущности") {
        val secretFile = saveStringToFile("secret string")
        val badXmlString = IOUtils
          .toString(getClass.getResourceAsStream("/feed_with_injection.xml"), "UTF-8")
          .replace("!{}", secretFile.getAbsolutePath)
        val xmlFile = saveStringToFile(badXmlString)
        for {
          offers <- FeedParser.parseOffersStream(xmlFile).runCollect
        } yield assert(offers)(hasSize(equalTo(1))) &&
          assert(offers)(hasAt(0)(hasField[Offer, String]("title", _.title, not(containsString("secret string")))))
      },
      testM("fail on invalid xsd") {
        val from = getResourceFile("/invalid.xml")
        for {
          _ <- FeedParser.validateXsd(from).flip
        } yield assertCompletes
      },
      testM("parse xml") {
        val from = getResourceFile("/feed.xml")
        val expectedOffer1 = Offer(
          id = "full",
          seller = Seller(
            Contacts(Some("+7-777-777-77-77"), Some(ContactMethod.OnlyPhone)),
            Locations(
              Vector(
                Address("Россия, Москва, Тверская улица"),
                GeoPoint(55.763954, 37.606397)
              )
            )
          ),
          title = "Товар",
          description = Some("Товар <огонь><br> Всем брать.<br>Гарантия 100%"),
          condition = Some(Condition.Used),
          category = "Диван",
          attributes = Some(Attributes(Vector(Attribute("Цвет", "Красный"), Attribute("Размер", "46")))),
          images = Some(Images(Vector(Image("http://.."), Image("http://...")))),
          video = Some("http://..."),
          price = Some(Offer.InCurrency(1000)),
          deliveryInfo = None
        )
        val expectedOffer2 = Offer(
          id = "short",
          seller = Seller(
            Contacts(None, Some(ContactMethod.OnlyChat)),
            Locations(
              Vector(
                Address("Россия, Москва, Тверская улица")
              )
            )
          ),
          title = "Товар",
          description = None,
          condition = Some(Condition.Used),
          category = "Диван",
          attributes = None,
          images = None,
          video = None,
          price = Some(Offer.InCurrency(1000)),
          deliveryInfo = None
        )
        val expectedOffer3 = Offer(
          id = "salary",
          seller = Seller(
            Contacts(None, Some(ContactMethod.OnlyChat)),
            Locations(
              Vector(
                Address("Россия, Москва, Тверская улица")
              )
            )
          ),
          title = "Работник",
          description = None,
          condition = None,
          category = "Дизайнер",
          attributes = None,
          images = None,
          video = None,
          price = Some(Offer.Salary(1000L)),
          deliveryInfo = Some(DeliveryInfo(Some("true"), Some("false")))
        )
        for {
          offers <- FeedParser.parseOffersStream(from).runCollect
        } yield assert(offers)(equalTo(Chunk(expectedOffer1, expectedOffer2, expectedOffer3)))
      }
    ).provideCustomLayerShared(FeedParser.Live)
  }
}
