package ru.yandex.vertis.general.wizard.scheduler.feed.writers

import general.bonsai.category_model.Category
import general.bonsai.export_model.ExportedEntity
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.core.service.BonsaiService
import ru.yandex.vertis.general.wizard.core.service.BonsaiService.Bonsai
import ru.yandex.vertis.general.wizard.core.service.impl.LiveBonsaiService
import ru.yandex.vertis.general.wizard.model.Price.PriceRub
import ru.yandex.vertis.general.wizard.model.{OfferFilter, OfferSource, StockOffer}
import ru.yandex.vertis.general.wizard.scheduler.feed.PinType
import ru.yandex.vertis.general.wizard.scheduler.services.FeedUrlBuilder.FeedUrlBuilder
import ru.yandex.vertis.general.wizard.scheduler.services.{FeedUrlBuilder, FeedUrlBuilderMock}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.general.wizard.scheduler.feed.utils.FeedStringUtils._
import zio.random.Random
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.valueF
import zio.test._
import zio.{RIO, ULayer, ZIO, ZLayer}

import java.io.File
import java.time.Instant

/** @author a-pashinin
  */
object CriteoWriterSpec extends DefaultRunnableSpec with MockitoSupport {

  private val file: File = mock[File]

  private def buildWriter(
      filters: Seq[OfferFilter] = Seq.empty,
      pinType: PinType.Value = PinType.None): RIO[Bonsai with FeedUrlBuilder, CriteoWriter] =
    for {
      urlBuilder <- ZIO.service[FeedUrlBuilder.Service]
      bonsai <- ZIO.service[BonsaiService.Service]
    } yield new CriteoWriter(urlBuilder, bonsai, filters, pinType, file)

  private val animals: Category =
    Category(
      id = "animals",
      uriPart = "jivotnie",
      name = "animals"
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

  private def buildFeedUrl(offer: StockOffer, filters: Seq[OfferFilter], pinType: PinType.Value): String =
    s"$offer::$filters::$pinType"

  private val urlBuilderMock = FeedUrlBuilderMock
    .Build(
      anything,
      valueF { case (offer, filters, pinType) =>
        Some(buildFeedUrl(offer, filters, pinType))
      }
    )

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
      imageUrl312x312 = "irrelevant string",
      images = Seq.empty,
      categoryId = cats.id,
      isMordaApproved = true,
      isYanApproved = true,
      regionIds = Seq.empty,
      attributes = Seq.empty,
      source = OfferSource.Unknown,
      createdAt = Instant.now()
    )

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

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("CriteoWriter")(
      testM("should have correct header and footer") {
        val ulrBuilderNotInvoked = FeedUrlBuilderMock.empty
        val writer = buildWriter(pinType = PinType.None)
        writer
          .provideLayer(bonsai ++ ulrBuilderNotInvoked)
          .map { writer =>
            assert(writer.header)(
              equalTo(""""id";"title";"price";"picture URL";"offer URL";"category id";"category name"""")
            ) &&
            assert(writer.footer)(equalTo(""))
          }
      },
      testM("should correctly serialize any input") {
        checkM(testParametersGen) { case ((filters, pinType), offer) =>
          (for {
            writer <- buildWriter(filters, pinType)
            url = buildFeedUrl(offer, filters, pinType)
            category = s"${animals.name} ${cats.name}"
            serialized <- writer.serializer.serialize(offer)
            price = offer.price.get.asInstanceOf[PriceRub].value
          } yield assert(serialized)(
            equalTo(
              Some(
                Seq(
                  offer.offerId,
                  offer.title,
                  s"$price RUB",
                  offer.imageUrl,
                  url,
                  offer.categoryId,
                  category
                ).map(_.escapeCsv)
                  .mkString(";")
              )
            )
          )).provideLayer(urlBuilderMock.toLayer ++ bonsai)
        }
      }
    )
  }
}
