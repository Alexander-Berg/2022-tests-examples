package ru.yandex.vos2.realty.model

import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.{TableFor2, TableFor4, TableFor5}
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.util.IOUtils
import ru.yandex.realty.context.ProviderAdapter
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.proto.offer.vos.Offer.Placement
import ru.yandex.realty.util.Mappings._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.vos2.model.realty.RealtyOffer.RealtyCategory
import ru.yandex.vertis.vos2.model.realty.RealtyOffer.RealtyCategory._
import ru.yandex.vertis.vos2.model.realty.{OfferType, Price, PricePaymentPeriod}
import ru.yandex.vos2.BasicsModel.Location
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.UserModel.UserType

import scala.concurrent.duration._

/**
  * @author Ilya Gerasimov (747mmhg@yandex-team.ru)
  */
@RunWith(classOf[JUnitRunner])
class ShowDurationSelectorSpec extends WordSpec with Matchers with MockitoSupport {

  val regionGraph: RegionGraph =
    RegionGraphProtoConverter.deserialize(
      IOUtils.gunzip(
        getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
      )
    )
  private val regionGraphProvider = ProviderAdapter.create(regionGraph)
  private val showDurationSelector = new ShowDurationSelector(regionGraphProvider)

  private def testInfiniteDuration(geoId: Int, address: String) {
    val offer = TestUtils.createOffer()
    offer.getOfferRealtyBuilder
      .setOfferType(OfferType.SELL)
      .setUnifiedAddress(
        Location
          .newBuilder()
          .setSubjectFederationId(geoId)
      )
      .getAddressBuilder
      .setAddress(address)
    assert(showDurationSelector.select(offer) == 365.days)
  }

  private val dateTimeFormat = DateTimeFormat.forPattern("dd.MM.yyyy")
  private val bucket29Hash = "i_12"
  private val bucket0Hash = "i_13"
  private val bucket6Hash = "i_19"
  private def setDateTtl(offer: Offer.Builder, dateTtlStart: String, dateTtlEnd: String): Offer.Builder = {
    val d1 = dateTimeFormat.parseDateTime(dateTtlStart).getMillis
    val d2 = dateTimeFormat.parseDateTime(dateTtlEnd).getMillis
    val h = (d2 - d1).millis.toHours.toInt
    offer.setTimestampTtlStart(d1)
    offer.setTimestampWillExpire(d2)
    offer.setOfferTTLHours(h)
  }

  "ShowDurationSelector" should {

    "set ttl for commercial offers to 60 days" in {
      val offer = TestUtils.createOffer()
      offer.getOfferRealtyBuilder
        .setCategory(RealtyCategory.CAT_COMMERCIAL)
      assert(showDurationSelector.select(offer) == 60.days)
    }

    "set ttl for daily rent to 60 days" in {
      val offer = TestUtils.createOffer()
      offer.getOfferRealtyBuilder
        .setPricePaymentPeriod(PricePaymentPeriod.PER_DAY)
        .setOfferType(OfferType.RENT)
      assert(showDurationSelector.select(offer) == 60.days)
    }

    "set ttl for garage rent in moscow to 60 days" in {
      val offer = TestUtils.createOffer()
      offer.getOfferRealtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_GARAGE)
        .getAddressBuilder
        .setRgid(587795)
        .setAddress("Москва, какая-то улица, какой-то дом")
      assert(showDurationSelector.select(offer) == 60.days)
    }

    "set ttl for garage rent not in moscow to 30 days" in {
      val offer = TestUtils.createOffer()
      offer.getOfferRealtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_GARAGE)
      assert(showDurationSelector.select(offer) == 30.days)
    }

    "set ttl for house or lot sell for 90 days" in {
      for (_ <- 1 to 10) {
        val offer = TestUtils.createOffer()
        offer.getOfferRealtyBuilder
          .setOfferType(OfferType.SELL)
          .setCategory(Gen.oneOf(RealtyCategory.CAT_HOUSE, RealtyCategory.CAT_LOT).sample.get)
        assert(showDurationSelector.select(offer) == 90.days)
      }
    }

    "set ttl for other regions low price apartment sale to 45 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.SELL)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("Пб, какая-то улица, какой-то дом")
      realtyBuilder.getPriceBuilder.setPriceValue(3000000)
      assert(showDurationSelector.select(offer) == 45.days)
    }

    "set ttl for other regions medium price apartment sale to 60 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.SELL)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("СПб, какая-то улица, какой-то дом")
      realtyBuilder.getPriceBuilder.setPriceValue(8000000)
      assert(showDurationSelector.select(offer) == 60.days)
    }

    "set ttl for other regions high price apartment sale to 90 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.SELL)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("Пб, какая-то улица, какой-то дом")
      realtyBuilder.getPriceBuilder.setPriceValue(20000000)
      assert(showDurationSelector.select(offer) == 90.days)
    }

    "set ttl for rooms sale to 45 days" in {
      val offer = TestUtils.createOffer()
      offer.getOfferRealtyBuilder
        .setOfferType(OfferType.SELL)
        .setCategory(RealtyCategory.CAT_ROOMS)
      assert(showDurationSelector.select(offer) == 45.days)
    }

    "set ttl for low price apartment rent in Moscow to 14 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("Москва, какая-то улица, какой-то дом")
        .setRgid(587795)
      realtyBuilder.getPriceBuilder.setPriceValue(40000)
      assert(showDurationSelector.select(offer) == 14.days)
    }

    "set ttl for medium price apartment rent in Moscow to 21 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("Москва, какая-то улица, какой-то дом")
      realtyBuilder.getPriceBuilder.setPriceValue(60000)
      assert(showDurationSelector.select(offer) == 21.days)
    }

    "set ttl for high price apartment rent in Moscow to 28 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("Москва, какая-то улица, какой-то дом")
        .setRgid(587795)
      realtyBuilder.getPriceBuilder.setPriceValue(80000)
      assert(showDurationSelector.select(offer) == 28.days)
    }

    "set ttl for low price apartment rent in SPb or Moscow region to 7 days" in {
      for (_ <- 1 to 10) {
        val offer = TestUtils.createOffer()
        val realtyBuilder = offer.getOfferRealtyBuilder
        realtyBuilder
          .setOfferType(OfferType.RENT)
          .setCategory(RealtyCategory.CAT_APARTMENT)
          .getAddressBuilder
          .setRgid(417899)
        realtyBuilder.getPriceBuilder.setPriceValue(20000)
        assert(showDurationSelector.select(offer) == 7.days)
      }
    }

    "set ttl for medium price apartment rent in SPb or Moscow region to 14 days" in {
      for (_ <- 1 to 10) {
        val offer = TestUtils.createOffer()
        val realtyBuilder = offer.getOfferRealtyBuilder
        realtyBuilder
          .setOfferType(OfferType.RENT)
          .setCategory(RealtyCategory.CAT_APARTMENT)
          .getAddressBuilder
          .setAddress(Gen.oneOf("Московская область", "Санкт-Петербург").sample.get)
          .setRgid(587795)
        realtyBuilder.getPriceBuilder.setPriceValue(40000)
        assert(showDurationSelector.select(offer) == 14.days)
      }
    }

    "set ttl for high price apartment rent in SPb or Moscow region to 21 days" in {
      for (_ <- 1 to 10) {
        val offer = TestUtils.createOffer()
        val realtyBuilder = offer.getOfferRealtyBuilder
        realtyBuilder
          .setOfferType(OfferType.RENT)
          .setCategory(RealtyCategory.CAT_APARTMENT)
          .getAddressBuilder
          .setAddress(Gen.oneOf("Московская область", "Санкт-Петербург").sample.get)
        realtyBuilder.getPriceBuilder.setPriceValue(60000)
        assert(showDurationSelector.select(offer) == 21.days)
      }
    }

    "set ttl for low price apartment rent in other regions to 7 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("Другой регион")
      realtyBuilder.getPriceBuilder.setPriceValue(10000)
      assert(showDurationSelector.select(offer) == 7.days)
    }

    "set ttl for medium price apartment rent in other regions to 14 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("Другой регион")
      realtyBuilder.getPriceBuilder.setPriceValue(30000)
      assert(showDurationSelector.select(offer) == 14.days)
    }

    "set ttl for high price apartment rent in other regions to 21 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_APARTMENT)
        .getAddressBuilder
        .setAddress("Другой регион")
      realtyBuilder.getPriceBuilder.setPriceValue(50000)
      assert(showDurationSelector.select(offer) == 21.days)
    }

    "set ttl for rooms rent to 7 days" in {
      val offer = TestUtils.createOffer()
      val realtyBuilder = offer.getOfferRealtyBuilder
      realtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(RealtyCategory.CAT_ROOMS)
      assert(showDurationSelector.select(offer) == 7.days)
    }

    "set ttl for house or lot rent to 45 days" in {
      for (_ <- 1 to 10) {
        val offer = TestUtils.createOffer()
        offer.getOfferRealtyBuilder
          .setOfferType(OfferType.RENT)
          .setCategory(Gen.oneOf(RealtyCategory.CAT_HOUSE, RealtyCategory.CAT_LOT).sample.get)
        assert(showDurationSelector.select(offer) == 30.days)
      }
    }

    "set ttl for draft to 7 days" in {
      val offer = TestUtils.createOffer()
      offer.addFlag(OfferFlag.OF_DRAFT)
      offer.getOfferRealtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(Gen.oneOf(RealtyCategory.CAT_HOUSE, RealtyCategory.CAT_LOT).sample.get)
      assert(showDurationSelector.select(offer) == 7.days)
    }

    "set ttl for offers from verifier to 3 years" in {
      val offer = TestUtils.createOffer()
      offer.getOfferRealtyBuilder
        .setOfferType(OfferType.RENT)
        .setCategory(Gen.oneOf(RealtyCategory.CAT_HOUSE, RealtyCategory.CAT_LOT).sample.get)
      offer.getUserBuilder.setUserType(UserType.UT_VERIFIER)
      assert(showDurationSelector.select(offer) == (3 * 365).days)
    }

    val commonTable = Table(
      ("offerType", "paymentPeriod", "dateTtlStart", "dateTtlEnd", "bucket", "result"),
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "01.03.2020", "01.09.2020", bucket29Hash, 92.days), // докидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "01.03.2020", "01.09.2020", bucket0Hash, 121.days), // докидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "01.03.2020", "01.09.2020", bucket0Hash, 121.days), // уже докинули, перешедулинг
      (OfferType.SELL, PricePaymentPeriod.PER_DAY, "30.03.2020", "01.09.2020", bucket29Hash, 93.days), // докидываем
      (OfferType.SELL, PricePaymentPeriod.PER_DAY, "30.03.2020", "01.09.2020", bucket0Hash, 122.days), // докидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "15.03.2020", "01.09.2020", bucket6Hash, 101.days), // докидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "15.03.2020", "15.05.2020", bucket0Hash, 60.days), // old logic
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "01.01.2020", "28.03.2020", bucket0Hash, 60.days), // old logic
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "01.03.2020", "14.04.2020", bucket0Hash, 60.days), // old logic
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "01.09.2019", "01.09.2020", bucket0Hash, 303.days), // докидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, "01.09.2019", "01.09.2020", bucket0Hash, 303.days) // докидываем, перешедулинг
    )

    "set ttl: 1.04.2020 + defaultTtl(offerType, paymentPeriod) - dateTtl - bucket => result" in {
      forAll(commonTable) { (offerType, paymentPeriod, dateTtlStart, dateTtlEnd, bucket, result) =>
        val offer = TestUtils.createOffer()
        offer.getOfferRealtyBuilder
          .setOfferType(offerType)
          .setPricePaymentPeriod(paymentPeriod)
          .getAddressBuilder
          .setRgid(587795)
        offer.getOfferRealtyBuilder.getAddressBuilder.setAddress("москва")
        setDateTtl(offer, dateTtlStart, dateTtlEnd)
        offer.setOfferID(bucket)
        val ttl = showDurationSelector.select(offer)
        val ttl1 = showDurationSelector.select(offer.setOfferTTLHours(ttl.toHours.toInt))
        assert(ttl == result)
        assert(ttl1 == result)
      }
    }

    val rentPerDayMskSpbTable = Table(
      ("offerType", "paymentPeriod", "dateTtlStart", "dateTtlEnd", "bucket", "result"),
      (OfferType.RENT, PricePaymentPeriod.PER_DAY, "01.02.2020", "01.08.2020", bucket29Hash, 151.days), // режем, умирает в июле
      (OfferType.RENT, PricePaymentPeriod.PER_DAY, "05.02.2020", "05.08.2020", bucket0Hash, 176.days), // режем, умирает в июле
      (OfferType.RENT, PricePaymentPeriod.PER_DAY, "01.06.2020", "01.12.2020", bucket6Hash, 120.days) //  не режем, просто 120 по новым
    )

    "set ttl: 15.06.2020 msk + spb per day" in {
      forAll(rentPerDayMskSpbTable) { (offerType, paymentPeriod, dateTtlStart, dateTtlEnd, bucket, result) =>
        val offer = TestUtils.createOffer()
        offer.getOfferRealtyBuilder
          .setOfferType(offerType)
          .setPricePaymentPeriod(paymentPeriod)
          .getAddressBuilder
          .setRgid(587795)
        offer.getOfferRealtyBuilder.getAddressBuilder.setAddress("москва")
        setDateTtl(offer, dateTtlStart, dateTtlEnd)
        offer.setOfferID(bucket)
        val ttl = showDurationSelector.select(offer)
        val ttl1 = showDurationSelector.select(offer.setOfferTTLHours(ttl.toHours.toInt))
        assert(ttl == result)
        assert(ttl1 == result)
      }
    }

    val rentPerDayRegionsTable = Table(
      ("offerType", "paymentPeriod", "dateTtlStart", "dateTtlEnd", "bucket", "result"),
      (OfferType.RENT, PricePaymentPeriod.PER_DAY, "13.06.2020", "13.08.2020", bucket29Hash, 3.days), // режем
      (OfferType.RENT, PricePaymentPeriod.PER_DAY, "14.04.2020", "14.06.2020", bucket6Hash, 60.days), // не режем, по старым
      (OfferType.RENT, PricePaymentPeriod.PER_DAY, "15.06.2020", "15.07.2020", bucket29Hash, 30.days), // ровно 30 по новым
      (OfferType.RENT, PricePaymentPeriod.PER_DAY, "12.06.2020", "12.08.2020", bucket6Hash, 27.days) // режем, 29 бакет
    )

    "set ttl: 30 days for rent day in spec regions" in {
      forAll(rentPerDayRegionsTable) { (offerType, paymentPeriod, dateTtlStart, dateTtlEnd, bucket, result) =>
        val offer = TestUtils.createOffer()
        offer.getOfferRealtyBuilder
          .setOfferType(offerType)
          .setPricePaymentPeriod(paymentPeriod)
          .getAddressBuilder
          .setRgid(681264)
        offer.getOfferRealtyBuilder.getAddressBuilder.setAddress("крым")
        setDateTtl(offer, dateTtlStart, dateTtlEnd)
        offer.setOfferID(bucket)
        val ttl = showDurationSelector.select(offer)
        val ttl1 = showDurationSelector.select(offer.setOfferTTLHours(ttl.toHours.toInt))
        assert(ttl == result)
        assert(ttl1 == result)
      }
    }

    val rentPerMonthRegionsTable = Table(
      ("offerType", "paymentPeriod", "dateTtlStart", "category", "dateTtlEnd", "bucket", "result"),
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, CAT_APARTMENT, "02.06.2020", "16.06.2020", bucket29Hash, 14.days), // накидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, CAT_APARTMENT, "02.06.2020", "16.06.2020", bucket0Hash, 43.days), // накидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, CAT_APARTMENT, "14.06.2020", "28.06.2020", bucket0Hash, 31.days), // накидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, CAT_APARTMENT, "15.06.2020", "15.07.2020", bucket29Hash, 30.days), // ровно 30 по новым
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, CAT_ROOMS, "10.06.2020", "17.06.2020", bucket0Hash, 35.days), // накидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, CAT_ROOMS, "10.06.2020", "17.06.2020", bucket29Hash, 6.days), // накидываем
      (OfferType.RENT, PricePaymentPeriod.PER_MONTH, CAT_ROOMS, "15.06.2020", "15.07.2020", bucket29Hash, 30.days) // ровно 30 по новым
    )

    "set ttl: 30 days for rent motnh in spec regions" in {
      forAll(rentPerMonthRegionsTable) {
        (offerType, paymentPeriod, category, dateTtlStart, dateTtlEnd, bucket, result) =>
          val offer = TestUtils.createOffer()
          offer.getOfferRealtyBuilder
            .setOfferType(offerType)
            .setCategory(category)
            .setPricePaymentPeriod(paymentPeriod)
            .getAddressBuilder
            .setRgid(681264)
          offer.getOfferRealtyBuilder.getAddressBuilder.setAddress("крым")
          setDateTtl(offer, dateTtlStart, dateTtlEnd)
          offer.setOfferID(bucket)
          val ttl = showDurationSelector.select(offer)
          val ttl1 = showDurationSelector.select(offer.setOfferTTLHours(ttl.toHours.toInt))
          assert(ttl == result)
          assert(ttl1 == result)
      }
    }
  }

  //scalastyle:off
  "ShowDurationSelector" when {

    "PaidPlacementRegionRule" should {
      "PaidPlacementRegionRule: set ttl=60d for commercial with quota applied and default value for others" in new PaidPlacementRegionRuleData {
        forAll(regions) { (_, rgid) =>
          forAll(commercialData) { (offerType, category, quotaApplied, meetsPaymentCriteria, expectedDuration) =>
            val offer = TestUtils
              .createOffer()
              .applySideEffect(
                _.getOfferRealtyBuilder
                  .setOfferType(offerType)
                  .setCategory(category)
                  .setPlacement {
                    Placement
                      .newBuilder()
                      .setQuotaApplied(quotaApplied)
                      .setMeetsPaymentCriteria(meetsPaymentCriteria)
                  }
                  .getAddressBuilder
                  .setRgid(rgid)
              )
            showDurationSelector.select(offer) shouldEqual expectedDuration
          }
        }
      }

      "PaidPlacementRegionRule: set ttl=[45d..90d] for apartments on sell/rent depend on price" in new PaidPlacementRegionRuleData {
        forAll(regions) { (_, rgid) =>
          forAll(apartmentData) { (offerType, category, price, meetsPaymentCriteria, result) =>
            val offer = TestUtils
              .createOffer()
              .applySideEffect(
                _.getOfferRealtyBuilder
                  .setOfferType(offerType)
                  .setCategory(category)
                  .setPrice(Price.newBuilder().setPriceValue(price).build())
                  .setPlacement(Placement.newBuilder().setMeetsPaymentCriteria(meetsPaymentCriteria).build())
                  .getAddressBuilder
                  .setRgid(rgid)
              )
            showDurationSelector.select(offer) shouldEqual result
          }
        }
      }

      "PaidPlacementRegionRule: set ttl=45d for rooms on sell and rent" in new PaidPlacementRegionRuleData {
        forAll(regions) { (_, rgid) =>
          forAll(roomsData) { (offerType, category, meetsPaymentCriteria, result) =>
            val offer = TestUtils
              .createOffer()
              .applySideEffect(
                _.getOfferRealtyBuilder
                  .setOfferType(offerType)
                  .setCategory(category)
                  .setPlacement(Placement.newBuilder().setMeetsPaymentCriteria(meetsPaymentCriteria).build())
                  .getAddressBuilder
                  .setRgid(rgid)
              )
            showDurationSelector.select(offer) shouldEqual result
          }
        }
      }

      "PaidPlacementRegionRule: set ttl for house, garage on rent/sell" in new PaidPlacementRegionRuleData {
        forAll(regions) { (_, rgid) =>
          forAll(otherData) { (offerType, category, meetsPaymentCriteria, result) =>
            val offer = TestUtils
              .createOffer()
              .applySideEffect(
                _.getOfferRealtyBuilder
                  .setOfferType(offerType)
                  .setCategory(category)
                  .setPlacement(Placement.newBuilder().setMeetsPaymentCriteria(meetsPaymentCriteria).build())
                  .getAddressBuilder
                  .setRgid(rgid)
              )
            showDurationSelector.select(offer) shouldEqual result
          }
        }
      }

    }
  }
  //scalastyle:on

  trait PaidPlacementRegionRuleData {

    val regions: TableFor2[Int, Long] = Table(
      ("geoId", "rgid"),
      (Regions.SVERDLOVSKAYA_OBLAST, NodeRgid.SVERDLOVSKAYA_OBLAST),
      (Regions.SVERDLOVSKAYA_OBLAST, NodeRgid.KAZAN),
      (Regions.NIZHNY_NOVGOROD_OBLAST, NodeRgid.NIZHNY_NOVGOROD_OBLAST),
      (Regions.NIZHNY_NOVGOROD_OBLAST, NodeRgid.KAZAN),
      (Regions.TATARSTAN, NodeRgid.KAZAN),
      (Regions.TATARSTAN, NodeRgid.TATARSTAN)
    )

    val DefaultResult: FiniteDuration = 30.days

    val commercialData: TableFor5[OfferType, RealtyCategory, Boolean, Boolean, FiniteDuration] = Table(
      ("offerType", "category", "quotaApplied", "meetsPaymentCriteria", "result"),
      (OfferType.RENT, CAT_COMMERCIAL, true, true, 60.days),
      (OfferType.RENT, CAT_COMMERCIAL, false, true, DefaultResult),
      (OfferType.RENT, CAT_COMMERCIAL, false, false, 60.days),
      (OfferType.SELL, CAT_COMMERCIAL, true, true, 60.days),
      (OfferType.SELL, CAT_COMMERCIAL, false, true, DefaultResult),
      (OfferType.SELL, CAT_COMMERCIAL, false, false, 60.days)
    )

    val apartmentData: TableFor5[OfferType, RealtyCategory, Int, Boolean, FiniteDuration] = Table(
      ("offerType", "category", "price", "meetsPaymentCriteria", "result"),
      // 0...4.5 Millions
      (OfferType.SELL, CAT_APARTMENT, 0, false, 45.days),
      (OfferType.SELL, CAT_APARTMENT, 1000000, false, 45.days),
      (OfferType.SELL, CAT_APARTMENT, 4499999, false, 45.days),
      // 4.5...10 Millions
      (OfferType.SELL, CAT_APARTMENT, 4500000, false, 60.days),
      (OfferType.SELL, CAT_APARTMENT, 5000000, false, 60.days),
      (OfferType.SELL, CAT_APARTMENT, 9999999, false, 60.days),
      // >10 Millions
      (OfferType.SELL, CAT_APARTMENT, 10000000, false, 90.days),
      (OfferType.SELL, CAT_APARTMENT, 20000000, false, 90.days),
      // rent
      (OfferType.RENT, CAT_APARTMENT, 0, false, 30.days),
      (OfferType.RENT, CAT_APARTMENT, 5000000, false, 30.days),
      (OfferType.RENT, CAT_APARTMENT, 10000000, false, 30.days),
      // meets payment criteria
      (OfferType.SELL, CAT_APARTMENT, 0, true, DefaultResult),
      (OfferType.SELL, CAT_APARTMENT, 5000000, true, DefaultResult),
      (OfferType.SELL, CAT_APARTMENT, 10000000, true, DefaultResult)
    )

    val roomsData: TableFor4[OfferType, RealtyCategory, Boolean, FiniteDuration] = Table(
      ("offerType", "category", "meetsPaymentCriteria", "result"),
      (OfferType.SELL, CAT_ROOMS, false, 45.days),
      (OfferType.RENT, CAT_ROOMS, false, 30.days),
      // meets payment criteria
      (OfferType.SELL, CAT_ROOMS, true, DefaultResult),
      (OfferType.RENT, CAT_ROOMS, true, DefaultResult)
    )

    val otherData: TableFor4[OfferType, RealtyCategory, Boolean, FiniteDuration] = Table(
      ("offerType", "category", "meetsPaymentCriteria", "result"),
      (OfferType.SELL, CAT_HOUSE, false, 90.days),
      (OfferType.SELL, CAT_HOUSE_WITH_LOT, false, 90.days),
      (OfferType.SELL, CAT_GARAGE, false, 90.days),
      (OfferType.RENT, CAT_HOUSE, false, 30.days),
      (OfferType.RENT, CAT_HOUSE_WITH_LOT, false, 30.days),
      (OfferType.RENT, CAT_GARAGE, false, 30.days),
      // meets payment criteria
      (OfferType.SELL, CAT_HOUSE, true, DefaultResult),
      (OfferType.SELL, CAT_HOUSE_WITH_LOT, true, DefaultResult),
      (OfferType.SELL, CAT_GARAGE, true, DefaultResult),
      (OfferType.RENT, CAT_HOUSE, true, DefaultResult),
      (OfferType.RENT, CAT_HOUSE_WITH_LOT, true, DefaultResult),
      (OfferType.RENT, CAT_GARAGE, true, DefaultResult)
    )
  }
}
