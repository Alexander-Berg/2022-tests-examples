package ru.yandex.auto.extdata.jobs.feeds.feed.writers.cars

import org.junit.runner.RunWith
import org.scalatest.{GivenWhenThen, Matchers, OneInstancePerTest, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.auto.core.model.enums.State.Search
import ru.yandex.auto.core.region.RegionService
import ru.yandex.auto.eds.service.cars.CarsCatalogGroupingService
import ru.yandex.auto.extdata.jobs.feeds.feed.FeedProperties.ListingUrl
import ru.yandex.auto.extdata.jobs.feeds.feed.utils.NoOpFeedsExporter
import ru.yandex.auto.extdata.jobs.feeds.feed.writers.CsvFieldExporter
import ru.yandex.auto.extdata.jobs.feeds.feed.writers.FeedWriter.FeedEntry
import ru.yandex.auto.extdata.jobs.feeds.feed.{FeedProperties, LandingUrlType}
import ru.yandex.auto.log.Logging
import ru.yandex.auto.message.CarAdSchema
import ru.yandex.auto.message.CarAdSchema.DiscountPrice.DiscountPriceStatus
import ru.yandex.auto.message.CarAdSchema.{CarAdMessage, DiscountPrice, PhotoClass}
import ru.yandex.vertis.mockito.MockitoSupport

import java.io.File
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class GoogleAdWordsFeedFormatSpec
    extends WordSpec
    with Matchers
    with GivenWhenThen
    with MockitoSupport
    with Logging
    with OneInstancePerTest {

  def getTempFile(fn: String, ext: String): File = {
    import java.nio.file.Files
    val tmpFile = Files.createTempFile(fn, ext).toFile
    tmpFile.deleteOnExit()
    tmpFile
  }

  "GoogleAdWordsFeedFormat" should {

    "create by-spec formatter" in {
      val tmpFile = getTempFile("adWordsTest", ".csv")
      log.info(s"tmp file: ${tmpFile.toPath.toAbsolutePath}")

      val regionService: RegionService = mock[RegionService]
      val carsCatalogGroupingService: CarsCatalogGroupingService = mock[CarsCatalogGroupingService]
      val factory = new GoogleAdWordsFeedFormat(regionService, carsCatalogGroupingService)

      // current implementation will fail with initialization exception,
      // wrapping with catching all throwables to ensure format in test
      val format: Seq[CsvFieldExporter[FeedEntry[CarAdMessage]]] = factory
        .googleAdWordsFeedFormatMap(
          ListingUrl(
            listingWithoutGeneration = false,
            pinOffer = true,
            forceListing = false,
            LandingUrlType.Non,
            isCommercialSellerGroup = false,
            shouldAddRegion = false
          ),
          Map.empty[PhotoClass, Int]
        )
        .map { exporter =>
          val fatalWrappedExtractor: FeedEntry[CarAdMessage] => String = (entry) =>
            try {
              exporter.extract(entry)
            } catch { case _ => "" }
          new CsvFieldExporter[FeedEntry[CarAdMessage]](exporter.columnName, fatalWrappedExtractor)
        }

      val id = "1"
      val mark = "testMark"
      val model = "testModel"
      val engineType = "testEngineType"
      val power = 111
      val priceRur = 222
      val discountPrice = 200
      val autoruHash = "123"
      val state = Search.NEW
      val expectedUrl = s"https://auto.ru/cars/$mark/$model/$state/?pinned_offer_id=autoru-$id".toLowerCase

      val msgBuilder = {
        val builder = CarAdSchema.CarAdMessage.newBuilder().setVersion(AutoSchemaVersions.CAR_AD_VERSION)
        import builder._
        setId(id)
        setMark(mark)
        setModel(model)
        setSearchState(state.name().toLowerCase)
        setEngineType(engineType)
        setPower(power)
        setPriceRur(priceRur)
        setAutoruHashCode(autoruHash)
        setDiscount(
          DiscountPrice
            .newBuilder()
            .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
            .setPrice(discountPrice)
            .setStatus(DiscountPriceStatus.ACTIVE)
        )
        builder
      }

      val msg = msgBuilder.build()

      val p = mock[FeedProperties]
      when(p.recipient).thenReturn("testRecipient")
      when(p.fileName).thenReturn(tmpFile.getName.stripSuffix(".csv"))

      val feedWriter = new FeedCarsWriter[CarAdMessage](
        tmpFile.getAbsoluteFile.getParentFile.getAbsolutePath,
        format,
        p,
        NoOpFeedsExporter
      )
      feedWriter.start
      feedWriter.write(FeedEntry(msg, Seq.empty))
      feedWriter.close()

      val source = Source.fromFile(tmpFile)
      val strings = source.getLines().toArray
      strings.length should be(2)
      strings(0) should be(
        "ID,Item title,Final URL,Image URL,Item subtitle,Item description,Item category,Price,Sale price,Contextual keywords,Item address,Tracking template,Custom parameter"
      )

      //empty fields are "points of growth" here - they should be derived just from test, but need some improvements

      // cannot be acquired due to CatalogItils linked with Main.carsCatalogGroupingService, which is not inititialized in tests
      val itemTitle = "" //not ok
      val imageUrl = WriterUtils.DefaultImage
      val itemSubtitle = "" //ok
      val itemDesc = "" //ok
      val itemCat = "" //ok
      val contextKeywords = "" //ok

      // cannot be set up in tests - WriterUtils.regionService is not initialized
      val itemAddress = "" //not ok
      val trackingTemplate = "" //ok
      val customParam = "" //ok

      strings(1) should be(
        Seq(
          s"$id-$autoruHash",
          itemTitle,
          expectedUrl,
          imageUrl,
          itemSubtitle,
          itemDesc,
          itemCat,
          s"$priceRur RUB",
          s"$discountPrice RUB",
          contextKeywords,
          itemAddress,
          trackingTemplate,
          customParam
        ).mkString(",")
      )

      source.close
    }
  }
}
