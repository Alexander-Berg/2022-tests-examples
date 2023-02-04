package ru.yandex.vertis.general.wizard.scheduler.feed.writers

import common.geobase.model.RegionIds.RegionId
import common.geobase.{Region, RegionTypes}
import general.bonsai.category_model.Category
import general.bonsai.export_model.ExportedEntity
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.core.service.BonsaiService.Bonsai
import ru.yandex.vertis.general.wizard.core.service.RegionService.RegionService
import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import ru.yandex.vertis.general.wizard.core.service.{BonsaiService, RegionService, RegionServiceMock}
import ru.yandex.vertis.general.wizard.model.Price.PriceRub
import ru.yandex.vertis.general.wizard.model.{OfferFilter, OfferSource, StockOffer}
import ru.yandex.vertis.general.wizard.scheduler.feed.PinType
import ru.yandex.vertis.general.wizard.scheduler.services.FeedUrlBuilder.FeedUrlBuilder
import ru.yandex.vertis.general.wizard.scheduler.services.{FeedUrlBuilder, FeedUrlBuilderMock}
import ru.yandex.vertis.general.wizard.scheduler.feed.utils.FeedStringUtils._
import ru.yandex.vertis.mockito.MockitoSupport
import zio.random.Random
import zio.test.Assertion.{anything, _}
import zio.test.mock.Expectation.{value, valueF}
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{RIO, ULayer, ZIO, ZLayer}

import java.io.File
import java.time.Instant

/** @author a-pashinin
  */
object GoogleWriterSpec extends DefaultRunnableSpec with MockitoSupport {

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

  private val bonsai: ULayer[Bonsai] =
    ZLayer.succeed(
      LiveBonsaiService.create(
        BonsaiSnapshot(
          Seq(
            ExportedEntity(ExportedEntity.CatalogEntity.Category(animals)),
            ExportedEntity(ExportedEntity.CatalogEntity.Category(cats))
          )
        )
      )
    )

  private val file: File = mock[File]

  private def regionMock(regionId: Long, regionType: RegionTypes.Type = RegionTypes.City): Region =
    Region(
      id = regionId,
      parentId = 0L,
      chiefRegionId = 0L,
      `type` = regionType,
      ruName = "Регион",
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

  private val regionService = RegionServiceMock.GetPathToRoot(anything, value(Seq(regionMock(213)))).toLayer

  private def buildFeedUrl(offer: StockOffer, filters: Seq[OfferFilter], pinType: PinType.Value): String =
    s"$offer::$filters::$pinType"

  private val urlBuilder = FeedUrlBuilderMock
    .Build(
      anything,
      valueF { case (offer, filters, pinType) =>
        Some(buildFeedUrl(offer, filters, pinType))
      }
    )
    .toLayer

  private def buildStockOffer(
      id: String,
      title: String,
      price: Long,
      imageUrl: String): StockOffer =
    StockOffer(
      title = title,
      description = "irrelevant string",
      address = "irrelevant string",
      offerId = id,
      price = Option(PriceRub(price)),
      isNew = None,
      imageUrl = imageUrl,
      imageUrl260x194 = "irrelevant string",
      imageUrl312x312 = "//irrelevant string",
      images = Seq.empty,
      categoryId = cats.id,
      isMordaApproved = true,
      isYanApproved = true,
      regionIds = Seq(RegionId(213)),
      attributes = Seq.empty,
      source = OfferSource.Unknown,
      createdAt = Instant.now()
    )

  private def buildWriter(
      filters: Seq[OfferFilter] = Seq.empty,
      pinType: PinType.Value = PinType.None): RIO[Bonsai with FeedUrlBuilder with RegionService, GoogleWriter] =
    for {
      urlBuilder <- ZIO.service[FeedUrlBuilder.Service]
      bonsai <- ZIO.service[BonsaiService.Service]
      regionService <- ZIO.service[RegionService.Service]
    } yield new GoogleWriter(urlBuilder, bonsai, regionService, filters, pinType, file)

  private val testParametersGen: Gen[Random with Sized, ((Seq[OfferFilter], PinType.Value), StockOffer)] =
    for {
      offerId <- Gen.alphaNumericString.filter(_.nonEmpty)
      title <- Gen.alphaNumericString
      price <- Gen.long(100, 100000)
      imageUrl <- Gen.alphaNumericString.filter(_.nonEmpty)
      filters <- Gen.listOfN(3)(
        Gen.elements(
          OfferFilter.ForFreeFilter(true),
          OfferFilter.IsNewFilter(true),
          OfferFilter.ForFreeFilter(false),
          OfferFilter.IsNewFilter(true)
        )
      )
      pinType <- Gen.elements(PinType.None, PinType.Subcategory, PinType.Category, PinType.FullText)
    } yield ((filters, pinType), buildStockOffer(offerId, title, price, imageUrl))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("GoogleWriter")(
      testM("should have correct header and footer") {
        for {
          writer <- buildWriter().provideLayer(bonsai ++ FeedUrlBuilderMock.empty ++ RegionServiceMock.empty)
        } yield assert(writer.header)(
          equalTo(s"""ID,ID2,Item title,Final URL,Image URL,Item category,Price,Item address""")
        ) &&
          assert(writer.footer)(equalTo(""))
      },
      testM("should correctly serialize any input") {
        checkM(testParametersGen) { case ((filters, pinType), offer) =>
          (for {
            writer <- buildWriter(filters, pinType)
            url = buildFeedUrl(offer, filters, pinType)
            serialized <- writer.serializer.serialize(offer)
            price = offer.price.get.asInstanceOf[PriceRub].value
          } yield assert(serialized)(
            equalTo(
              Some(
                Seq(
                  offer.offerId,
                  cats.id,
                  offer.title,
                  url,
                  "http:" + offer.imageUrl312x312,
                  cats.name,
                  s"$price RUB",
                  "Регион"
                ).map(_.escapeCsvGoogle)
                  .mkString(",")
              )
            )
          )).provideLayer(urlBuilder ++ regionService ++ bonsai)
        }
      }
    )
}
