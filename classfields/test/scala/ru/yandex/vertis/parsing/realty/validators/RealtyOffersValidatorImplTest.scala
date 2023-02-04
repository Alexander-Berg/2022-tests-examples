package ru.yandex.vertis.parsing.realty.validators

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.realty.proto.offer.FlatType
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.common.RegionIds._
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.components.time.TimeService
import ru.yandex.vertis.parsing.dao.cache.DbCache
import ru.yandex.vertis.parsing.realty.ParsingRealtyModel.{OfferCategory, OfferType, ParsedOffer}
import ru.yandex.vertis.parsing.realty.bunkerconfig.{BunkerConfig, BunkerFilter, BunkerFilterMode}
import ru.yandex.vertis.parsing.realty.components.TestCatalogsComponents
import ru.yandex.vertis.parsing.realty.dao.cache.phone.ActiveCountForPhoneCacheDao
import ru.yandex.vertis.parsing.realty.dao.phones.PhonesDao
import ru.yandex.vertis.parsing.realty.util.TestDataUtils._
import ru.yandex.vertis.parsing.util.RandomUtil._
import ru.yandex.vertis.parsing.validators.FilterReason._

@RunWith(classOf[JUnitRunner])
class RealtyOffersValidatorImplTest extends FunSuite with MockitoSupport {
  validatorTest =>
  private val regionTree = TestCatalogsComponents.regionTree
  private val activeForUserIdCache = mock[DbCache[Int]]
  private val activeForUserNameCache = mock[DbCache[Int]]
  private val activeForPhoneCache = mock[ActiveCountForPhoneCacheDao]
  private val phonesDao = mock[PhonesDao]
  private val timeService = mock[TimeService]
  private val now: DateTime = DateTime.now()
  when(timeService.getNow).thenReturn(now)

  private val bunkerConfig = BunkerConfig(
    Seq(
      BunkerFilter(
        sites = Seq(Site.Avito),
        categories = Seq(
          (OfferType.OWNER_RENT, OfferCategory.APARTMENT)
        ),
        mode = BunkerFilterMode.EnabledExceptRegions(
          Set(
            `Санкт-Петербург и Ленинградская область`,
            `Москва и Московская область`,
            `Чеченская Республика`
          )
        )
      ),
      BunkerFilter(
        sites = Seq(Site.Avito),
        categories = Seq(
          (OfferType.OWNER_RENT, OfferCategory.HOUSE)
        ),
        mode = BunkerFilterMode.Disabled
      ),
      BunkerFilter(
        sites = Seq(Site.Avito),
        categories = Seq(
          (OfferType.SELL, OfferCategory.APARTMENT),
          (OfferType.SELL, OfferCategory.HOUSE)
        ),
        mode = BunkerFilterMode.EnabledExceptRegions(
          Set(
            `Чеченская Республика`
          )
        )
      )
    ),
    Seq(),
    Set("79000000000"),
    Seq(),
    Seq()
  )

  private val validator = new RealtyOffersValidatorImpl(
    regionTree,
    activeForUserIdCache,
    activeForUserNameCache,
    activeForPhoneCache,
    phonesDao,
    timeService
  ) {
    override def bunkerConfig: BunkerConfig = validatorTest.bunkerConfig
    override protected def observeStep(name: String)(durationSecs: Float, error: Boolean): Unit = {}
  }

  private val Protvino = 20576
  private val Tula = 15

  test("validated rows") {}

  test("skipped") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.BUY,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusMinutes(59)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isSkipped)
  }

  test("validateCategory: WrongCategory") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.BUY,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(WrongCategory))
  }

  test("validateCategory: DisabledCategory") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.OWNER_RENT,
      offerCategory = OfferCategory.HOUSE,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(DisabledCategory))
  }

  test("FilteredRegion") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.OWNER_RENT,
      offerCategory = OfferCategory.APARTMENT,
      geobaseId = Protvino,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(FilteredRegion))
  }

  test("NoRegion") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.OWNER_RENT,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(NoRegion))
  }

  test("UnknownRegion") {
    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.getRawLocationBuilder.setAddress("Адрес")
    val row = testRow(
      testAvitoApartmentUrl,
      parsedOffer = parsedOffer,
      offerType = OfferType.OWNER_RENT,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(UnknownRegion))
  }

  test("NoUser") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.OWNER_RENT,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(NoUser))
  }

  test("ManyActive: UserId") {
    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    val userId = nextHexString(10)
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserId(userId)
    val row = testRow(
      testAvitoApartmentUrl,
      parsedOffer = parsedOffer,
      offerType = OfferType.OWNER_RENT,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map(userId -> 5), Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(ManyActiveByUserId))
  }

  test("ManyActive: UserName") {
    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    val userName = nextHexString(10)
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserName(userName)
    val row = testRow(
      testAvitoApartmentUrl,
      parsedOffer = parsedOffer,
      offerType = OfferType.OWNER_RENT,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map(userName -> 5), Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(ManyActiveByUserName))
  }

  test("Inactive") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.OWNER_RENT,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1),
      deactivateDate = Some(DateTime.now().minusDays(1))
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(Inactive))
  }

  test("NewFlat") {
    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    parsedOffer.getOfferBuilder.setFlatType(FlatType.FLAT_TYPE_NEW_FLAT)
    val row = testRow(
      testAvitoApartmentUrl,
      parsedOffer = parsedOffer,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(NewFlat))
  }

  test("NoPhones") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      phone = None,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(NoPhones))
  }

  test("BannedPhone") {
    val phone = "79000000000"
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      phone = Some(phone),
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map(phone -> 0), Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(BannedPhone))
  }

  test("ManyActiveByPhone") {
    val phone = getRandomPhone
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      phone = Some(phone),
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map(phone -> 5), Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(ManyActiveByPhone))
  }

  test("InvalidPhone") {
    val phone = "1234"
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      phone = Some(phone),
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map(phone -> 0), Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(InvalidPhone))
  }

  test("StaticPhone") {
    val phone = getRandomStaticPhone
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      phone = Some(phone),
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map(phone -> 0), Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(StaticPhone))
  }

  test("NoPrice") {
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      updated = now.minusHours(1)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map.empty, Map.empty)
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(NoPrice))
  }

  test("TodaySent") {
    val sendMoment = now.withMillisOfDay(0).plusHours(1)
    when(timeService.getNow).thenReturn(now.withMillisOfDay(0).plusHours(3))
    val phone = getRandomPhone
    val row = testRow(
      testAvitoApartmentUrl,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      phone = Some(phone),
      updated = now.withMillisOfDay(0).plusHours(2)
    )
    val result = validator.validate(row, Map.empty, Map.empty, Map(phone -> 0), Map(phone -> sendMoment))
    assert(result.isInvalid)
    assert(result.asInvalid.errors.contains(TodaySent))
    when(timeService.getNow).thenReturn(now)
  }

  test("Valid") {
    val sendMoment = now.withMillisOfDay(0).minusHours(1)
    when(timeService.getNow).thenReturn(now.withMillisOfDay(0).plusHours(3))
    val parsedOffer: ParsedOffer.Builder = ParsedOffer.newBuilder()
    val userId = nextHexString(10)
    parsedOffer.getOfferBuilder.getSellerBuilder.setUserId(userId)
    parsedOffer.getOfferBuilder.setPrice(5000000)
    val phone = getRandomPhone
    val row = testRow(
      testAvitoApartmentUrl,
      parsedOffer = parsedOffer,
      offerType = OfferType.SELL,
      offerCategory = OfferCategory.APARTMENT,
      geobaseId = Tula,
      phone = Some(phone),
      updated = now.withMillisOfDay(0).plusHours(2)
    )
    val result = validator.validate(row, Map(userId -> 1), Map.empty, Map(phone -> 0), Map(phone -> sendMoment))
    assert(result.isValid)
    when(timeService.getNow).thenReturn(now)
  }
}
