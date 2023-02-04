package ru.yandex.vertis.general.wizard.scheduler.feed.writers

import common.geobase.model.RegionIds
import common.geobase.{Region, RegionTypes}
import general.bonsai.category_model.Category
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import ru.yandex.vertis.general.wizard.core.service.RegionService.RegionService
import ru.yandex.vertis.general.wizard.core.service.{RegionService, RegionServiceMock}
import ru.yandex.vertis.general.wizard.model.OfferFilter.{ForFreeFilter, IsNewFilter}
import ru.yandex.vertis.general.wizard.model.Price._
import ru.yandex.vertis.general.wizard.model.{OfferFilter, OfferSource, StockOffer}
import ru.yandex.vertis.general.wizard.scheduler.feed.PinType
import ru.yandex.vertis.general.wizard.scheduler.services.FeedUrlBuilder.FeedUrlBuilder
import ru.yandex.vertis.general.wizard.scheduler.services.{FeedUrlBuilder, FeedUrlBuilderMock}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._
import zio.{RIO, ZIO}

import java.io.File
import java.time.Instant

object DirectWriterSpec extends DefaultRunnableSpec with MockitoSupport {

  private val animals: Category =
    Category(
      id = "animals",
      uriPart = "jivotnie"
    )

  private val cats: Category =
    Category(
      id = "cat",
      name = "cat",
      uriPart = "koshki",
      parentId = "animals"
    )

  val file: File = mock[File]

  private def buildWriter(
      pinType: PinType.Value = PinType.Subcategory,
      filters: Seq[OfferFilter] = Seq.empty): RIO[FeedUrlBuilder with RegionService, DirectWriter] =
    for {
      urlBuilder <- ZIO.service[FeedUrlBuilder.Service]
      regionService <- ZIO.service[RegionService.Service]
    } yield new DirectWriter(urlBuilder, regionService, filters, file, pinType, Set(cats))

  private def regionMock(regionId: Long, regionType: RegionTypes.Type = RegionTypes.FederalSubject): Region =
    Region(
      id = regionId,
      parentId = 0L,
      chiefRegionId = 0L,
      `type` = regionType,
      ruName = "",
      ruPreposition = "",
      ruNamePrepositional = "",
      ruNameGenitive = "",
      ruNameDative = "",
      ruNameAccusative = "",
      ruNameInstrumental = "",
      latitude = 0d,
      longitude = 0d,
      tzOffset = 0,
      population = 0L
    )

  val defaultRegionService = RegionServiceMock.GetPathToRoot(anything, value(Seq(regionMock(213))))

  val complexRegionService = RegionServiceMock.GetPathToRoot(
    anything,
    value(Seq(regionMock(213, RegionTypes.City), regionMock(225)))
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DirectWriter")(
      testM("should have correct header and footer") {
        val ulrBuilderNotInvoked = FeedUrlBuilderMock.empty
        val now = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").print(DateTime.now())
        val directWriter = buildWriter(pinType = PinType.None)
        directWriter
          .provideLayer(ulrBuilderNotInvoked ++ RegionServiceMock.empty)
          .map { writer =>
            assert(writer.header)(equalTo(s"""<?xml version=\"1.0\" encoding=\"UTF-8\"?>
                 |<yml_catalog date=\"$now\">
                 |<categories>
                 |<category id="0">${cats.id}</category>
                 |</categories>""".stripMargin)) &&
            assert(writer.footer)(equalTo("</yml_catalog>"))
          }
      },
      testM("should write correct feed, when initialized without any pin") {
        val offer = catsOffer
        val serialized = for {
          writer <- buildWriter(pinType = PinType.None)
          serialized <- writer.serializer.serialize(offer)
        } yield serialized
        val offerUrl = s"https://o.yandex.ru/${animals.uriPart}/offer/${offer.offerId}"
        val offerRequestUrlBuilder = FeedUrlBuilderMock
          .Build(
            equalTo((offer, Seq.empty[OfferFilter], PinType.None)),
            value(Some(offerUrl))
          )
        assertM(serialized.provideLayer(offerRequestUrlBuilder ++ defaultRegionService))(
          equalTo(
            Some(
              s"""<offer id="4" available="true">
                 |  <url>$offerUrl</url>
                 |  <name>magic cat</name>
                 |  <picture>www.cats.com</picture>
                 |  <price>100500</price>
                 |  <currencyId>RUB</currencyId>
                 |  <description>cat for wizard tests</description>
                 |  <categoryId>0</categoryId>
                 |  <vendorCode>213</vendorCode>
                 |</offer>""".stripMargin
            )
          )
        )
      },
      testM("should write correct feed, when initialized with category pin") {
        val offer = catsOffer
        val serialized = for {
          writer <- buildWriter(pinType = PinType.Category)
          serialized <- writer.serializer.serialize(offer)
        } yield serialized
        val categoryUrl = s"https://o.yandex.ru/${animals.uriPart}/?pinned_offer_id=${offer.offerId}"
        val categoryUrlBuilder = FeedUrlBuilderMock
          .Build(
            equalTo((offer, Seq.empty, PinType.Category)),
            value(Some(categoryUrl))
          )
        assertM(serialized.provideLayer(categoryUrlBuilder ++ defaultRegionService))(
          equalTo(Some(s"""<offer id="4" available="true">
             |  <url>$categoryUrl</url>
             |  <name>magic cat</name>
             |  <picture>www.cats.com</picture>
             |  <price>100500</price>
             |  <currencyId>RUB</currencyId>
             |  <description>cat for wizard tests</description>
             |  <categoryId>0</categoryId>
             |  <vendorCode>213</vendorCode>
             |</offer>""".stripMargin))
        )
      },
      testM("should write correct feed, when initialized with subcategory pin") {
        val offer = catsOffer
        val serialized = for {
          writer <- buildWriter(pinType = PinType.Subcategory)
          serialized <- writer.serializer.serialize(offer)
        } yield serialized
        val subcategoryUrl =
          s"https://o.yandex.ru/${animals.uriPart}/${cats.uriPart}/?pinned_offer_id=${offer.offerId}"
        val subcategoryUrlBuilder = FeedUrlBuilderMock
          .Build(
            equalTo((offer, Seq.empty, PinType.Subcategory)),
            value(Some(subcategoryUrl))
          )
        assertM(serialized.provideLayer(subcategoryUrlBuilder ++ defaultRegionService))(
          equalTo(Some(s"""<offer id="4" available="true">
             |  <url>$subcategoryUrl</url>
             |  <name>magic cat</name>
             |  <picture>www.cats.com</picture>
             |  <price>100500</price>
             |  <currencyId>RUB</currencyId>
             |  <description>cat for wizard tests</description>
             |  <categoryId>0</categoryId>
             |  <vendorCode>213</vendorCode>
             |</offer>""".stripMargin))
        )
      },
      testM("should write correct feed, when initialized with subcategory pin") {
        val offer = catsOffer
        val serialized = for {
          writer <- buildWriter(pinType = PinType.Subcategory)
          serialized <- writer.serializer.serialize(offer)
        } yield serialized
        val subcategoryUrl =
          s"https://o.yandex.ru/${animals.uriPart}/${cats.uriPart}/?pinned_offer_id=${offer.offerId}"
        val subcategoryUrlBuilder = FeedUrlBuilderMock
          .Build(
            equalTo((offer, Seq.empty, PinType.Subcategory)),
            value(Some(subcategoryUrl))
          )
        assertM(serialized.provideLayer(subcategoryUrlBuilder ++ defaultRegionService))(
          equalTo(Some(s"""<offer id="4" available="true">
               |  <url>$subcategoryUrl</url>
               |  <name>magic cat</name>
               |  <picture>www.cats.com</picture>
               |  <price>100500</price>
               |  <currencyId>RUB</currencyId>
               |  <description>cat for wizard tests</description>
               |  <categoryId>0</categoryId>
               |  <vendorCode>213</vendorCode>
               |</offer>""".stripMargin))
        )
      },
      testM("should forward filters to UrlBuilder") {
        val offer = catsOffer
        val filtersGen =
          Gen.setOfBounded(0, 2)(Gen.oneOf(Gen.const(IsNewFilter(true)), Gen.const(ForFreeFilter(true)))).map(_.toSeq)
        val filteredUrl = "some filtered url"
        checkM(filtersGen) { filters =>
          val filteredUrlBuilder = FeedUrlBuilderMock
            .Build(
              equalTo((offer, filters, PinType.Subcategory)),
              value(Some(filteredUrl))
            )
          val serialized = for {
            writer <- buildWriter(filters = filters)
            serialized <- writer.serializer.serialize(offer)
          } yield serialized
          for {
            result <- serialized.provideLayer(filteredUrlBuilder ++ defaultRegionService)
          } yield assert(result)(equalTo(Some(s"""<offer id="4" available="true">
               |  <url>$filteredUrl</url>
               |  <name>magic cat</name>
               |  <picture>www.cats.com</picture>
               |  <price>100500</price>
               |  <currencyId>RUB</currencyId>
               |  <description>cat for wizard tests</description>
               |  <categoryId>0</categoryId>
               |  <vendorCode>213</vendorCode>
               |</offer>""".stripMargin)))
        }
      },
      testM("should write choose region of SubjectFederation type") {
        val offer = catsOffer
        val serialized = for {
          writer <- buildWriter(pinType = PinType.None)
          serialized <- writer.serializer.serialize(offer)
        } yield serialized
        val offerUrl = "doesn't matter"
        val doesntMatterUrlBuilder = FeedUrlBuilderMock
          .Build(
            anything,
            value(Some(offerUrl))
          )
        assertM(serialized.provideLayer(doesntMatterUrlBuilder ++ complexRegionService))(
          equalTo(
            Some(
              s"""<offer id="4" available="true">
                 |  <url>$offerUrl</url>
                 |  <name>magic cat</name>
                 |  <picture>www.cats.com</picture>
                 |  <price>100500</price>
                 |  <currencyId>RUB</currencyId>
                 |  <description>cat for wizard tests</description>
                 |  <categoryId>0</categoryId>
                 |  <vendorCode>225</vendorCode>
                 |</offer>""".stripMargin
            )
          )
        )
      }
    )
  }

  private val catsOffer: StockOffer = StockOffer(
    title = "magic cat",
    description = "cat for wizard tests",
    address = StringUtils.EMPTY,
    offerId = "4",
    price = Option(PriceRub(100500)),
    imageUrl = "www.cats.com",
    imageUrl260x194 = "www.cats.com2",
    imageUrl312x312 = "www.cats.com3",
    images = Seq.empty,
    categoryId = cats.id,
    isMordaApproved = true,
    isYanApproved = true,
    regionIds = Seq(RegionIds.Moscow),
    attributes = Seq.empty,
    isNew = None,
    source = OfferSource.Unknown,
    createdAt = Instant.parse("2021-08-20T11:15:30Z")
  )
}
