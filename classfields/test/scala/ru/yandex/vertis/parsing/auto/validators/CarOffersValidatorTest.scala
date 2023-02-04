package ru.yandex.vertis.parsing.auto.validators

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.mockito.Mockito.reset
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.auto.api.ApiOfferModel.{Category, Section, Seller}
import ru.auto.api.CommonModel.PriceInfo
import ru.yandex.vertis.doppel.model.proto.{Cluster, Entity}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.ParsingAutoModel.ParsedOffer
import ru.yandex.vertis.parsing.auto.common.ResellerRecordWithOffset
import ru.yandex.vertis.parsing.auto.components.TestCatalogsAndFeaturesComponents
import ru.yandex.vertis.parsing.auto.components.bunkerconfig.{BunkerConfig, BunkerConfigAware, MarkModelDisabledInRegion, PriceRestrictRegion}
import ru.yandex.vertis.parsing.auto.dao.legacy.{LegacyDao, LegacyData, LegacyStatus}
import ru.yandex.vertis.parsing.auto.dao.model.ParsedRow
import ru.yandex.vertis.parsing.auto.dao.resellers.ResellersDao
import ru.yandex.vertis.parsing.auto.dao.resellers.model.ResellerRow
import ru.yandex.vertis.parsing.auto.diffs.OfferFields
import ru.yandex.vertis.parsing.auto.features.ParsingFeatures
import ru.yandex.vertis.parsing.auto.util.TestDataUtils._
import ru.yandex.vertis.parsing.common.RegionIds._
import ru.yandex.vertis.parsing.components.time.DefaultTimeService
import ru.yandex.vertis.parsing.dao.cache.DbCache
import ru.yandex.vertis.parsing.extdata.geo.Tree
import ru.yandex.vertis.parsing.parsers.CommonParser
import ru.yandex.vertis.parsing.validators.FilterReason
import ru.yandex.vertis.parsing.validators.FilterReason._
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
//noinspection ScalaStyle
@RunWith(classOf[JUnitRunner])
class CarOffersValidatorTest extends WordSpecLike with Matchers with MockitoSupport {

  trait Fixture {
    val sentDateforPhoneCache: DbCache[DateTime] = mock[DbCache[DateTime]]
    val features: ParsingFeatures = TestCatalogsAndFeaturesComponents.features

    val legacyDao: LegacyDao = mock[LegacyDao]
    when(legacyDao.getFilters(?)(?)).thenReturn(Map.empty[String, Option[LegacyData]])

    val resellersDao: ResellersDao = mock[ResellersDao]
    when(resellersDao.getResellers(?, ?)(?)).thenReturn(Seq.empty[ResellerRow])

    val defaultDate = new DateTime(0L)

    val regionTree: Tree = TestCatalogsAndFeaturesComponents.regionTree // TODO заменить моком
    when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(defaultDate)

    val timeService = new DefaultTimeService

    val bannedPhoneNumber = 79676138888L

    val offersValidator: CarOffersValidator with BunkerConfigAware =
      new CarOffersValidator(sentDateforPhoneCache, legacyDao, resellersDao, features, regionTree, timeService)
        with BunkerConfigAware {

        override def bunkerConfig: BunkerConfig = BunkerConfig(
          banned_phones = Set(bannedPhoneNumber),
          accepted_car_regions = Set("Санкт-Петербург и Ленинградская область"),
          disabled_car_mark_models = Set("DODGE CHALLENGER", "TESLA"),
          phones_sent_check_skip_regions = Set("Республика Тыва"),
          price_restrict_regions = Set(
            PriceRestrictRegion(500000, Seq("Ярославская область")),
            PriceRestrictRegion(750000, Seq("Свердловская область"))
          ),
          mark_models_disabled_in_regions = Set(MarkModelDisabledInRegion(Seq("Санкт-Петербург"), Seq("FORD FOCUS"))),
          Seq(),
          Seq()
        )
      }

    def checkValid(row: ParsedRow): Unit = {
      val result = offersValidator.validate(Seq(row))(Traced.empty)(row)
      withClue(result) {
        result.isValid shouldBe true
      }
    }

    def checkNoTheseErrors(row: ParsedRow, errors: String*): Unit = {
      val result = offersValidator.validate(Seq(row))(Traced.empty)(row)
      if (errors.isEmpty) result.isValid shouldBe true
      else {
        result.isInvalid shouldBe true
        errors.foreach(error => {
          result.asInvalid.errors should not contain error
        })
      }
    }

    def checkIsSkipped(row: ParsedRow): Unit = {
      val result = offersValidator.validate(Seq(row))(Traced.empty)(row)
      result.isSkipped shouldBe true
    }

    def checkErrors(row: ParsedRow, errors: String*): Unit = {
      val result = offersValidator.validate(Seq(row))(Traced.empty)(row)
      if (errors.isEmpty) result.isValid shouldBe true
      else {
        result.isInvalid shouldBe true
        errors.foreach(error => {
          val resultErrors = result.asInvalid.errors
          withClue(resultErrors.mkString(", ")) {
            resultErrors.distinct.length shouldBe resultErrors.length
          }
          resultErrors should contain(error)
        })
      }
    }

    def checkErrors(rows: Seq[ParsedRow], errorsMap: Map[String, Seq[String]]): Unit = {
      offersValidator.validate(rows)(Traced.empty).foreach {
        case (row, result) =>
          val errors = errorsMap(row.hash)
          if (errors.isEmpty) result.isValid shouldBe true
          else {
            result.isInvalid shouldBe true
            errors.foreach(error => {
              val resultErrors = result.asInvalid.errors
              withClue(resultErrors.mkString(", ")) {
                resultErrors.distinct.length shouldBe resultErrors.length
              }
              resultErrors should contain(error)
            })
          }
      }
    }
  }

  "CarOffersValidator" should {
    "Inactive" in new Fixture {
      def row: ParsedRow =
        testRow(testAvitoCarsUrl, category = Category.CARS, deactivateDate = Some(DateTime.now().minusDays(1)))
      checkErrors(row, Inactive)
    }

    "WrongCategory" in new Fixture {
      def row: ParsedRow = testRow(testAvitoTrucksUrl, category = Category.TRUCKS)
      checkErrors(row, WrongCategory)
    }

    "return noParseDate" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      checkErrors(row, NoParseDate)

      builder.getParseDateBuilder.setSeconds(CommonParser.minParseDateSeconds)
      checkErrors(row, NoParseDate)
    }

    "older5Days" in new Fixture {
      pending
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getParseDateBuilder.setSeconds(DateTime.now().minusDays(6).getMillis / 1000)
      checkErrors(row, Older5Days)
    }

    "noPhones" in new Fixture {
      def row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)

      checkErrors(row, NoPhones)
    }

    "noPhones: allow for avito cars if feature is enabled" in new Fixture {
      features.AllowAvitoCarsWithoutPhones.setEnabled(true)

      def row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)

      checkNoTheseErrors(row, NoPhones)
      features.AllowAvitoCarsWithoutPhones.setEnabled(false)
    }

    "resellers: skip validation if feature is disabled" in new Fixture {
      val sellerUrl = "reseller"
      def row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS, sellerUrl = sellerUrl)
      reset(resellersDao)
      when(resellersDao.getResellers(?, ?)(?)).thenReturn(Seq(resellerRow(sellerUrl)))
      checkNoTheseErrors(row, Reseller)
    }

    "resellers: skip validation if feature is enabled, but sellerUrl is not in resellers" in new Fixture {
      features.CheckResellers.setEnabled(true)
      val sellerUrl = "reseller"
      def row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS, sellerUrl = sellerUrl)
      checkNoTheseErrors(row, Reseller)
      features.CheckResellers.setEnabled(false)
    }

    "resellers: skip validation 2 if feature is enabled, but sellerUrl is not in resellers" in new Fixture {
      features.CheckResellers.setEnabled(true)
      val sellerUrl = "reseller"
      val sellerUrl2 = "reseller2"
      def row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS, sellerUrl = sellerUrl)
      reset(resellersDao)
      when(resellersDao.getResellers(?, ?)(?)).thenReturn(Seq(resellerRow(sellerUrl2)))
      checkNoTheseErrors(row, Reseller)
      features.CheckResellers.setEnabled(false)
    }

    "resellers: skip validation 3 if feature is enabled, but sellerUrl is not in resellers" in new Fixture {
      features.CheckResellers.setEnabled(true)
      val sellerUrl2 = "reseller2"
      def row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS)
      reset(resellersDao)
      when(resellersDao.getResellers(?, ?)(?)).thenReturn(Seq(resellerRow(sellerUrl2)))
      checkNoTheseErrors(row, Reseller)
      features.CheckResellers.setEnabled(false)
    }

    "resellers: throw error if feature is enabled, and sellerUrl is in resellers" in new Fixture {
      features.CheckResellers.setEnabled(true)
      val sellerUrl = "reseller"
      def row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS, sellerUrl = sellerUrl)
      reset(resellersDao)
      when(resellersDao.getResellers(?, ?)(?)).thenReturn(Seq(resellerRow(sellerUrl)))
      checkErrors(row, Reseller)
      features.CheckResellers.setEnabled(false)
    }

    "reseller by hash: throw error if feature is enabled, and sellerUrl is in reseller hashes" in new Fixture {
      features.CheckResellerHashes.setEnabled(true)
      val sellerHash = "adafe2bec7cfef0391ea62ab89cc8b1b"
      val sellerUrl =
        "https://www.avito.ru/user/adafe2bec7cfef0391ea62ab89cc8b1b27d41ff10e6ad9f2ae3a22783fa1af7f/profile"
      def row: ParsedRow = testRow(testAvitoCarsUrl, category = Category.CARS, sellerUrl = sellerUrl)
      reset(resellersDao)
      when(resellersDao.getResellers(?, ?)(?)).thenReturn(Seq(resellerRow(sellerHash)))
      checkErrors(row, ResellerByHash)
      features.CheckResellerHashes.setEnabled(false)
    }

    "noPhones: drom" in new Fixture {
      def row: ParsedRow = testRow(testDromCarsUrl, category = Category.CARS)

      checkNoTheseErrors(row, NoPhones)
    }

    "invalidPhone" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("5000")
      checkErrors(row, InvalidPhone)
    }

    "staticPhone" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("78004442211")
      checkErrors(row, StaticPhone)
    }

    "bannedPhone" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(bannedPhoneNumber.toString)
      checkErrors(row, BannedPhone)
    }

    "todaySent" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      val phone = "79294442211"
      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(DateTime.now().withMillisOfDay(0).plusHours(1))
      checkErrors(row, TodaySent)

      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(DateTime.now().withMillisOfDay(0).minusHours(1))
      checkNoTheseErrors(row, TodaySent)

      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(DateTime.now().minusDays(2))
      checkNoTheseErrors(row, TodaySent)

      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(defaultDate)
      checkNoTheseErrors(row, TodaySent)

      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(DateTime.now().minusHours(1))
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Республика Тыва`)
      checkNoTheseErrors(row, TodaySent)
    }

    "recentlySent: 5 days" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      val phone = "79294442211"
      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(DateTime.now().minusDays(4))
      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      checkErrors(row, RecentlySent5Days)

      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(DateTime.now().minusDays(6))
      checkNoTheseErrors(row, RecentlySent5Days)

      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(defaultDate)
      checkNoTheseErrors(row, RecentlySent5Days)

      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(DateTime.now().minusDays(4))
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Республика Тыва`)
      checkNoTheseErrors(row, RecentlySent5Days)

      when(sentDateforPhoneCache.getOrElseUpdate(?)(?)).thenReturn(defaultDate)
    }

    "filteredRegion" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Москва и Московская область`)
      checkErrors(row, FilteredRegion)
    }

    "unknownRegion" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setAddress("адрес")
      checkErrors(row, UnknownRegion)
    }

    "noRegion" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      checkErrors(row, NoRegion)
    }

    "emptyMarkModel" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      checkErrors(row, EmptyMarkModel)

      builder.getOfferBuilder.getCarInfoBuilder.setMark("MARK")
      checkErrors(row, EmptyMarkModel)

      builder.getOfferBuilder.getCarInfoBuilder.clearMark().setModel("MODEL")
      checkErrors(row, EmptyMarkModel)
    }

    "markModel" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getCarInfoBuilder.setMark("DODGE").setModel("CHALLENGER")
      checkErrors(row, MarkModel)

      builder.getOfferBuilder.getCarInfoBuilder.setMark("TESLA")
      checkErrors(row, MarkModel)
    }

    "fordFocusInSpb" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Санкт-Петербург`)
      builder.getOfferBuilder.getCarInfoBuilder.setMark("FORD").setModel("FOCUS")
      checkErrors(row, "ford_focus_in_region_2")
    }

    "lowPrice" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(10000)
      checkErrors(row, LowPrice)
    }

    "highPrice500k" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(600000)
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Ярославская область`)
      checkErrors(row, HighPrice500k)
    }

    "highPrice750k" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(800000)
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Свердловская область`)
      checkErrors(row, HighPrice750k)
    }

    "isDealer" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getIsDealerBuilder.setValue(true)
      checkErrors(row, IsDealer)
    }

    "was dealer in history: feature disabled" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getIsDealerBuilder.setValue(false)
      builder.addStatusHistoryBuilder().addDiffBuilder().setName("FilterReasons").setNewValue(IsDealer)
      checkNoTheseErrors(row, WasDealerInHistory)
    }

    "was dealer in history: feature enabled" in new Fixture {
      TestCatalogsAndFeaturesComponents.features.CheckDealerInHistory.setEnabled(true)
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getIsDealerBuilder.setValue(false)
      builder.addStatusHistoryBuilder().addDiffBuilder().setName("FilterReasons").setNewValue(IsDealer)
      checkErrors(row, WasDealerInHistory)
      TestCatalogsAndFeaturesComponents.features.CheckDealerInHistory.setEnabled(false)
    }

    "was dealer in history: feature enabled, is dealer now" in new Fixture {
      TestCatalogsAndFeaturesComponents.features.CheckDealerInHistory.setEnabled(true)
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getIsDealerBuilder.setValue(true)
      builder.addStatusHistoryBuilder().addDiffBuilder().setName("FilterReasons").setNewValue(IsDealer)
      checkNoTheseErrors(row, WasDealerInHistory)
      TestCatalogsAndFeaturesComponents.features.CheckDealerInHistory.setEnabled(false)
    }

    "was dealer in history: feature enabled, no filter reasons" in new Fixture {
      TestCatalogsAndFeaturesComponents.features.CheckDealerInHistory.setEnabled(true)
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      checkNoTheseErrors(row, WasDealerInHistory)
      TestCatalogsAndFeaturesComponents.features.CheckDealerInHistory.setEnabled(false)
    }

    "was reseller in history: feature disabled" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.addStatusHistoryBuilder().addDiffBuilder().setName("FilterReasons").setNewValue(Reseller)
      checkNoTheseErrors(row, WasResellerInHistory)
    }

    "was reseller in history: feature enabled" in new Fixture {
      TestCatalogsAndFeaturesComponents.features.CheckResellerInHistory.setEnabled(true)
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.addStatusHistoryBuilder().addDiffBuilder().setName("FilterReasons").setNewValue(Reseller)
      checkErrors(row, WasResellerInHistory)
      TestCatalogsAndFeaturesComponents.features.CheckResellerInHistory.setEnabled(false)
    }

    "was reseller in history: feature enabled, no filter reasons" in new Fixture {
      TestCatalogsAndFeaturesComponents.features.CheckResellerInHistory.setEnabled(true)
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      checkNoTheseErrors(row, WasResellerInHistory)
      TestCatalogsAndFeaturesComponents.features.CheckResellerInHistory.setEnabled(false)
    }

    "valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      fillBuilderForValidOffer(builder)
      checkValid(row)
    }

    "CheckLegacyStatus: status 2: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow =
        testRow(url, builder, category = Category.CARS).copy(createDate = DateTime.now().minusHours(23))

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)
      when(legacyDao.getStatus(?)(?)).thenReturn(Map(url -> Some(2)))

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "CheckLegacyStatus: status not found: create_date less than day: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow =
        testRow(url, builder, category = Category.CARS).copy(createDate = DateTime.now().minusHours(23))

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)
      when(legacyDao.getStatus(?)(?)).thenReturn(Map(url -> None))

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "CheckLegacyStatus: status not found: create_date more than day: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow =
        testRow(url, builder, category = Category.CARS).copy(createDate = DateTime.now().minusHours(25))

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)
      when(legacyDao.getStatus(?)(?)).thenReturn(Map(url -> None))

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "CheckLegacyStatus: status 1: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow =
        testRow(url, builder, category = Category.CARS).copy(createDate = DateTime.now().minusHours(23))

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)
      when(legacyDao.getStatus(?)(?)).thenReturn(Map(url -> Some(1)))

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "CheckLegacyStatus: status 1: create_date more than day: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow =
        testRow(url, builder, category = Category.CARS).copy(createDate = DateTime.now().minusHours(25))

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)
      when(legacyDao.getStatus(?)(?)).thenReturn(Map(url -> Some(1)))

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "CheckLegacyFilters: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(true)
      when(legacyDao.getFilters(?)(?)).thenReturn(
        Map(
          url -> Some(LegacyData(LegacyStatus.Filtered, Seq(ProcessedByScala)))
        )
      )

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(false)
    }

    "distinct phone errors" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      val sellerBuilder: Seller.Builder = builder.getOfferBuilder.getSellerBuilder
      sellerBuilder.addPhonesBuilder().setPhone("73912800811")
      sellerBuilder.addPhonesBuilder().setPhone("73912801811")

      checkErrors(row, StaticPhone)
    }

    "CheckLegacyStatus, am.ru: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAmruCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getStatus(?)(?)).thenReturn(Map.empty[String, Option[Int]])
      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "process_later: CheckLegacyFilters, am.ru" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAmruCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getFilters(?)(?)).thenReturn(Map.empty[String, Option[LegacyData]])
      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkErrors(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(false)
    }

    "CheckLegacyFilters: legacy status 1: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getFilters(?)(?)).thenReturn(
        Map(
          url -> Some(LegacyData(LegacyStatus.New, Seq()))
        )
      )
      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(false)
    }

    "CheckLegacyStatus: legacy status not found: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getStatus(?)(?)).thenReturn(Map.empty[String, Option[Int]])
      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "CheckLegacyFilters: no legacy status found: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getFilters(?)(?)).thenReturn(Map.empty[String, Option[LegacyData]])
      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(false)
    }

    "CheckLegacyStatus: processed_by_php" in new Fixture {
      pending
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getStatus(?)(?)).thenReturn(Map(url -> Some(3)))
      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkErrors(row, ProcessedByPHP)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "CheckLegacyStatus: status for another url: processed_by_php" in new Fixture {
      pending
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl
      val url2: String = testAvitoCarsUrl
      builder
        .addStatusHistoryBuilder()
        .addDiff(
          CommonModel.Diff
            .newBuilder()
            .setName(OfferFields.Url)
            .setOldValue(url2)
            .setNewValue(url)
        )

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getStatus(?)(?)).thenReturn(Map(url2 -> Some(3)))
      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkErrors(row, ProcessedByPHP)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyStatus.setEnabled(false)
    }

    "CheckLegacyFilters: legacy status 3: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getFilters(?)(?)).thenReturn(
        Map(
          url -> Some(LegacyData(3, Seq()))
        )
      )
      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(false)
    }

    "CheckLegacyFilters: legacy status 2: already_sent reason" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getFilters(?)(?)).thenReturn(
        Map(
          url -> Some(LegacyData(LegacyStatus.Filtered, Seq("already_sent")))
        )
      )
      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkErrors(row, AlreadySentPhp)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(false)
    }

    "CheckLegacyFilters: legacy status 2: non-sent reason: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getFilters(?)(?)).thenReturn(
        Map(
          url -> Some(LegacyData(LegacyStatus.Filtered, Seq("no_phone")))
        )
      )
      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(true)

      fillBuilderForValidOffer(builder)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacyFilters.setEnabled(false)
    }

    "CheckLegacySentPhones: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl
      val phone = "79291112233"

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getSentPhones(?)(?)).thenReturn(
        Map(
          phone -> None
        )
      )
      TestCatalogsAndFeaturesComponents.features.CheckLegacySentPhones.setEnabled(true)

      builder.getParseDateBuilder.setSeconds(DateTime.now().minusHours(1).getMillis / 1000)
      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Санкт-Петербург`)
      builder.getOfferBuilder.getCarInfoBuilder.setMark("MARK").setModel("MODEL")
      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(400000)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacySentPhones.setEnabled(false)
    }

    "CheckLegacySentPhones: TodaySentPhpByPhone" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl
      val phone = "79291112233"

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getSentPhones(?)(?)).thenReturn(
        Map(
          phone -> Some(DateTime.now().minusHours(23))
        )
      )
      TestCatalogsAndFeaturesComponents.features.CheckLegacySentPhones.setEnabled(true)

      builder.getParseDateBuilder.setSeconds(DateTime.now().minusHours(1).getMillis / 1000)
      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Санкт-Петербург`)
      builder.getOfferBuilder.getCarInfoBuilder.setMark("MARK").setModel("MODEL")
      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(400000)
      checkErrors(row, TodaySentPhpByPhone)
      checkNoTheseErrors(row, RecentlySentPhpByPhone)

      TestCatalogsAndFeaturesComponents.features.CheckLegacySentPhones.setEnabled(false)
    }

    "CheckLegacySentPhones: RecentlySentPhpByPhone" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl
      val phone = "79291112233"

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getSentPhones(?)(?)).thenReturn(
        Map(
          phone -> Some(DateTime.now().minusDays(29))
        )
      )
      TestCatalogsAndFeaturesComponents.features.CheckLegacySentPhones.setEnabled(true)

      builder.getParseDateBuilder.setSeconds(DateTime.now().minusHours(1).getMillis / 1000)
      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Санкт-Петербург`)
      builder.getOfferBuilder.getCarInfoBuilder.setMark("MARK").setModel("MODEL")
      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(400000)
      checkErrors(row, RecentlySentPhpByPhone)
      checkNoTheseErrors(row, TodaySentPhpByPhone)

      TestCatalogsAndFeaturesComponents.features.CheckLegacySentPhones.setEnabled(false)
    }

    "CheckLegacySentPhones: phone sent 181 days ago: valid" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl
      val phone = "79291112233"

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      when(legacyDao.getSentPhones(?)(?)).thenReturn(
        Map(
          phone -> Some(DateTime.now().minusDays(181))
        )
      )
      TestCatalogsAndFeaturesComponents.features.CheckLegacySentPhones.setEnabled(true)

      builder.getParseDateBuilder.setSeconds(DateTime.now().minusHours(1).getMillis / 1000)
      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Санкт-Петербург`)
      builder.getOfferBuilder.getCarInfoBuilder.setMark("MARK").setModel("MODEL")
      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(400000)
      checkValid(row)

      TestCatalogsAndFeaturesComponents.features.CheckLegacySentPhones.setEnabled(false)
    }

    "validatePreviousLegacyFilters" in new Fixture {
      pending
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl
      val phone = "79291112233"

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      builder.addStatusHistoryBuilder().setStatus(CommonModel.Status.FILTERED).setComment(ProcessedByPHP)

      builder.getParseDateBuilder.setSeconds(DateTime.now().minusHours(1).getMillis / 1000)
      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Санкт-Петербург`)
      builder.getOfferBuilder.getCarInfoBuilder.setMark("MARK").setModel("MODEL")
      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(400000)
      checkErrors(row, ProcessedByPHP)
    }

    "validatePreviousLegacyFilters: do not fail on other reasons" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl
      val phone = "79291112233"

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      builder.addStatusHistoryBuilder().setStatus(CommonModel.Status.FILTERED).setComment(Older5Days)

      builder.getParseDateBuilder.setSeconds(DateTime.now().minusHours(1).getMillis / 1000)
      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)
      builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Санкт-Петербург`)
      builder.getOfferBuilder.getCarInfoBuilder.setMark("MARK").setModel("MODEL")
      builder.getOfferBuilder.getPriceInfoBuilder.setPrice(400000)
      checkValid(row)
    }

    "validateSection" in new Fixture {
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()

      def row: ParsedRow = testRow(testAvitoCarsUrl, builder, category = Category.CARS)

      builder.getOfferBuilder.setSection(Section.NEW)
      checkErrors(row, IsNew)
    }

    "validateNotDealerPhone" in new Fixture {
      // убрали эту проверку
      val builder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      val url: String = testAvitoCarsUrl
      val phone = "79291112233"

      def row: ParsedRow = testRow(url, builder, category = Category.CARS)

      builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone(phone)

      checkNoTheseErrors(row, FilterReason.IsDealersPhone)
    }

    "skip validation: not enough data" in new Fixture {
      TestCatalogsAndFeaturesComponents.features.CheckDublicates.setEnabled(true)
      TestCatalogsAndFeaturesComponents.features.CheckDublicatesFromSite.values.foreach(_.setEnabled(true))

      val builder: Cluster.Builder = Cluster.newBuilder().setEnoughData(false)

      def row: ParsedRow =
        testRow(testAvitoCarsUrl, category = Category.CARS, doppelClusterSeq = Some(Seq(builder.build())))

      checkIsSkipped(row)
      TestCatalogsAndFeaturesComponents.features.CheckDublicates.setEnabled(false)
      TestCatalogsAndFeaturesComponents.features.CheckDublicatesFromSite.values.foreach(_.setEnabled(false))
    }

    "error validation: matched contains autoru" in new Fixture {
      TestCatalogsAndFeaturesComponents.features.CheckDublicates.setEnabled(true)
      TestCatalogsAndFeaturesComponents.features.CheckDublicatesFromSite.values.foreach(_.setEnabled(true))
      val matched: Entity = Entity.newBuilder().setClassified("AUTORU").build()
      val builder: Cluster.Builder = Cluster.newBuilder().addMatched(matched)

      def row: ParsedRow =
        testRow(testAvitoCarsUrl, category = Category.CARS, doppelClusterSeq = Some(Seq(builder.build())))

      checkErrors(row, ExistingOnAutoru)
      TestCatalogsAndFeaturesComponents.features.CheckDublicates.setEnabled(false)
      TestCatalogsAndFeaturesComponents.features.CheckDublicatesFromSite.values.foreach(_.setEnabled(false))
    }

    "valid: enough data=true" in new Fixture {
      val builder: Cluster.Builder = Cluster.newBuilder().setEnoughData(true)
      val parsedOfferBuilder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      fillBuilderForValidOffer(parsedOfferBuilder)

      def row: ParsedRow =
        testRow(
          testAvitoCarsUrl,
          parsedOffer = parsedOfferBuilder,
          category = Category.CARS,
          doppelClusterSeq = Some(Seq(builder.build()))
        )

      checkValid(row)
    }

    "skip validation: empty clusters and hour not passed" in new Fixture {
      TestCatalogsAndFeaturesComponents.features.CheckDublicates.setEnabled(true)
      TestCatalogsAndFeaturesComponents.features.CheckDublicatesFromSite.values.foreach(_.setEnabled(true))

      def row: ParsedRow =
        testRow(
          testAvitoCarsUrl,
          category = Category.CARS,
          now = DateTime.now().minusMinutes(55),
          doppelClusterSeq = Some(Seq())
        )

      checkIsSkipped(row)
      TestCatalogsAndFeaturesComponents.features.CheckDublicates.setEnabled(false)
      TestCatalogsAndFeaturesComponents.features.CheckDublicatesFromSite.values.foreach(_.setEnabled(false))
    }

    "valid: no matched from autoru, hour passed" in new Fixture {
      val matched: Entity = Entity.newBuilder().setClassified("DROM").build()
      val builder: Cluster.Builder = Cluster.newBuilder().addMatched(matched)
      val parsedOfferBuilder: ParsedOffer.Builder = ParsedOffer.newBuilder()
      fillBuilderForValidOffer(parsedOfferBuilder)

      def row: ParsedRow =
        testRow(
          testAvitoCarsUrl,
          category = Category.CARS,
          parsedOffer = parsedOfferBuilder,
          now = DateTime.now().minusMinutes(61),
          doppelClusterSeq = Some(Seq(builder.build()))
        )

      checkValid(row)
    }
  }

  def fillBuilderForValidOffer(builder: ParsedOffer.Builder): PriceInfo.Builder = {
    builder.getParseDateBuilder.setSeconds(DateTime.now().minusHours(1).getMillis / 1000)
    builder.getOfferBuilder.getSellerBuilder.addPhonesBuilder().setPhone("79291112233")
    builder.getOfferBuilder.getSellerBuilder.getLocationBuilder.setGeobaseId(`Санкт-Петербург`)
    builder.getOfferBuilder.getCarInfoBuilder.setMark("MARK").setModel("MODEL")
    builder.getOfferBuilder.getPriceInfoBuilder.setPrice(400000)
  }

  def resellerRow(sellerUrl: String): ResellerRow = {
    ResellerRow.newFromRecord(
      ResellerRecordWithOffset(sellerUrl, 5, 500),
      DateTime.now(),
      "comment"
    )
  }
}
