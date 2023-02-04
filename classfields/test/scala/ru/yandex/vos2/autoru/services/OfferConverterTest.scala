package ru.yandex.vos2.autoru.services

import java.text.SimpleDateFormat
import org.joda.time.{DateTime, LocalTime}
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.{ScalaCheckDrivenPropertyChecks, ScalaCheckPropertyChecks}
import org.scalatest.{BeforeAndAfterAll, Inspectors, OptionValues}
import ru.auto.api.ApiOfferModel.{Availability, PtsStatus, Section}
import ru.auto.api.CommonModel
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Editor
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService.ServiceType
import ru.yandex.vos2.AutoruModel.AutoruOffer.{SellerType, SourceInfo}
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo.{CabinType, TrailerType, WheelDrive}
import ru.yandex.vos2.AutoruModel.AutoruOffer.YandexVideo.YandexVideoStatus
import ru.yandex.vos2.AutoruModel.AutoruOffer.YoutubeVideo.YoutubeVideoStatus
import ru.yandex.vos2.BasicsModel._
import ru.yandex.vos2.OfferModel.{HolocronStatus, Offer, OfferFlag, OfferOrBuilder}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.AutoruSale.{SaleEmail, Setting}
import ru.yandex.vos2.autoru.model._
import ru.yandex.vos2.model.offer.OfferGenerator
import ru.yandex.vos2.model.{OfferRef, UserRefAutoru, UserRefAutoruClient}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 8/15/16.
  */
//scalastyle:off line.size.limit
@RunWith(classOf[JUnitRunner])
class OfferConverterTest
  extends AnyFunSuite
  with OptionValues
  with InitTestDbs
  with Inspectors
  with ScalaCheckPropertyChecks
  with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    initOldSalesDbs()
  }

  implicit val t: Traced = Traced.empty

  val sdf1: SimpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val sdf2: SimpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd")

  private val someSale = AutoruSale(
    1043067068,
    "a5ee5",
    DateTime.now,
    DateTime.now,
    DateTime.now,
    None,
    AutoruSaleStatus.STATUS_SHOW,
    sectionId = 2,
    userId = 18318774,
    None,
    225,
    213,
    213,
    225,
    213,
    213,
    clientId = 0,
    contactId = 0,
    newClientId = 333,
    salonId = 0,
    _salonContactId = 0,
    year = 2000,
    price = 1000000,
    currency = "RUR",
    priceRur = 1000000,
    _markId = 109,
    modificationId = 58238,
    _folderId = 30907,
    description = "some text",
    discountPrice = Some(AutoruSale.DiscountPrice(1, 1043067068, 123, 0, 100000, "active"))
  )

  private def convertCarsStrict(sale: AutoruSale, offer: Option[Offer] = None): Offer = {
    val result = components.carOfferConverter.convertStrict(sale, offer)
    assert(result.converted.nonEmpty, s"Sale ${sale.id} not converted, result: $result")
    result.converted.value
  }

  private def convertTrucksStrict(sale: AutoruTruck, offer: Option[Offer] = None) = {
    val result = components.truckOfferConverter.convertStrict(sale, offer)
    assert(result.converted.nonEmpty, s"Sale ${sale.id} not converted, result: $result")
    result.converted.value
  }

  private def convertTrucksNonStrict(sale: AutoruTruck, offer: Option[Offer] = None) = {
    val result = components.truckOfferConverter.convertForReading(sale, offer)
    assert(result.converted.nonEmpty, s"Sale ${sale.id} not converted, result: $result")
    result.converted.value
  }

  test("Check hash moderation") {

    // объявление с турбо-пакетом и стикерами
    val sale = components.autoruSalesDao.getOffer(1043270830).value

    val offer = convertCarsStrict(sale).toBuilder
      .setHashModeration("asd")
      .build()

    val offer2 = convertCarsStrict(sale, Some(offer))

    assert(offer.getHashModeration == offer2.getHashModeration)
  }

  test("testDontTouchBrandCertification") {

    val sale = components.autoruSalesDao.getOffer(1043026846).value

    val offer = convertCarsStrict(sale)

    assert(offer.hasOfferAutoru)

    assert(!offer.getOfferAutoru.hasBrandCertInfo)
  }

  /**
    * Убеждаемся, что непустой хеш салона успешно конвертируется
    */
  test("testSalonWithNonEmptyHash") {

    // объявление от автосалона с непустым хешом
    val sale = components.autoruSalesDao.getOffer(1037186558).value
    val salonPoi = sale.salonPoi.value
    assert(salonPoi.hash.value == "d281")

    val offer = convertCarsStrict(sale)

    assert(offer.hasOfferAutoru)

    assert(offer.getOfferAutoru.hasSalon)
    assert(offer.getOfferAutoru.getSalon.hasSalonHash)
    assert(offer.getOfferAutoru.getSalon.getSalonHash == "d281")
  }

  /**
    * Убеждаемся, что salonPoi успешно загружается, проверяем также телефоны и клиента внутри него.
    */
  test("test load and convert SalonPoi") {

    // объявление от имени автосалона
    val sale = components.autoruSalesDao.getOffer(1042409964).value

    val offer = convertCarsStrict(sale)

    assert(offer.hasOfferAutoru)

    assert(offer.getOfferAutoru.hasSalon)
    assert(offer.getOfferAutoru.getSalon.getSalonId == "9542")
    assert(!offer.getOfferAutoru.getSalon.hasSalonHash)
    assert(offer.getOfferAutoru.getSalon.getPlace.getAddress == "44 км МКАД (внешняя сторона), владение 1")
    assert(offer.getOfferAutoru.getSalon.getPlace.getCoordinates.getLatitude == 55.628967)
    assert(offer.getOfferAutoru.getSalon.getPlace.getCoordinates.getLongitude == 37.469810)
  }

  /**
    * проверяем, что корректно загружается из базы и конвертируется объявление с яндекс-видео
    */
  test("test load and convert YandexVideo") {

    // объявление с активным яндекс-видео
    val sale = components.autoruSalesDao.getOffer(1043211458).value

    val offer = convertCarsStrict(sale)
    assert(offer.hasOfferAutoru)

    val autoruOffer = offer.getOfferAutoru
    assert(autoruOffer.getVideoCount == 1)
    val convertedVideo = autoruOffer.getVideo(0)
    assert(convertedVideo.hasYandexVideo)
    assert(convertedVideo.getYandexVideo.getStatus == YandexVideoStatus.AVAILABLE)
    assert(!convertedVideo.hasYoutubeVideo)

    val convertedYandexVideo = convertedVideo.getYandexVideo
    assert(convertedYandexVideo.hasVideoIdent)
    assert(convertedYandexVideo.getVideoIdent == "m-63774-156a07376d0-87d395248cef988d")

    assert(convertedYandexVideo.getVideoUrlsCount == 10)
    val convertedYaVideoUrl0 = convertedYandexVideo.getVideoUrls(0)
    assert(convertedYaVideoUrl0.getName == "sq.mp4")
    assert(convertedYaVideoUrl0.getSize == 5160903)
    assert(
      convertedYaVideoUrl0.getUrl == "https://storage.mds.yandex.net/get-video-autoru-office/117893/156a0747a38/5ddb436e77b44098/sq.mp4?redirect=yes&sign=5bc646938f5d064116e7feb163488a7dacf08ecbd4776b7389d7fdfe3cc96ab1&ts=6a82762b"
    )
    // TODO: провалидировать остальные 9 видео

    assert(convertedYandexVideo.getVideoThumbsCount == 6)
    val convertedYaVideoTumb0 = convertedYandexVideo.getVideoThumbs(0)
    assert(
      convertedYaVideoTumb0.getUrl == "https://static.video.yandex.ru/get/office-autoru/m-63774-156a07376d0-87d395248cef988d/120x90.jpg"
    )
    assert(convertedYaVideoTumb0.getWidth == 120)
    assert(convertedYaVideoTumb0.getHeight == 90)
    // TODO: провалидировать остальные 5 картинок

    assert(!convertedYandexVideo.hasTargetUrl)
    assert(!convertedYandexVideo.hasProgressUrl)

    assert(convertedVideo.getCreated == sdf1.parse("2016-08-22 04:31:25").getTime)
    assert(convertedVideo.getUpdated == sdf1.parse("2016-08-22 04:31:25").getTime)
  }

  /**
    * проверяем, что корректно загружается из базы и конвертируется объявление с ютуб-видео
    */
  test("test load and convert YoutubeVideo") {

    // объявление с активным ютуб-видео
    val sale = components.autoruSalesDao.getOffer(1043026846).value
    val videos = sale.videos.value
    assert(videos.length == 1)
    assert(videos.length == 1)
    val video1 = videos.head
    assert(video1.id == 2406154)
    assert(video1.saleId == 1043026846)
    assert(video1.provider == "Youtube")
    assert(video1.value == "https://youtu.be/6cylm0mka_c")
    assert(video1.parseValue == "6cylm0mka_c")
    assert(video1.videoId == 0)
    assert(video1.createDate.getMillis == sdf1.parse("2016-08-22 16:01:50").getTime)
    assert(video1.updateDate.getMillis == sdf1.parse("2016-08-22 16:01:50").getTime)

    val offer = convertCarsStrict(sale)
    assert(offer.hasOfferAutoru)
    val autoruOffer = offer.getOfferAutoru

    assert(autoruOffer.getVideoCount == 1)
    val convertedVideo = autoruOffer.getVideo(0)
    assert(convertedVideo.hasYoutubeVideo)
    assert(!convertedVideo.hasYandexVideo)
    val youtubeVideo = convertedVideo.getYoutubeVideo
    assert(youtubeVideo.getStatus == YoutubeVideoStatus.UNKNOWN)
    assert(youtubeVideo.getYoutubeId == "6cylm0mka_c")
    assert(youtubeVideo.getLastCheckMoment == 0L)
  }

  test("testLoadAndConvertServicesAndBadges") {

    // объявление с турбо-пакетом и стикерами
    val sale = components.autoruSalesDao.getOffer(1043270830).value

    val offer = convertCarsStrict(sale)
    assert(offer.hasOfferAutoru)
    val autoruOffer = offer.getOfferAutoru

    assert(autoruOffer.getServicesCount == 7)

    val List(os1, os2, os3, os4, os5, os6, os7) = autoruOffer.getServicesList.asScala.toList

    assert(os1.getServiceType == ServiceType.TURBO)
    assert(os1.getCreated == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(os1.getIsActive)
    assert(!os1.hasBadge)
    assert(!os1.hasOfferBilling)
    assert(!os1.hasOfferBillingDeadline)
    assert(os1.hasExpireDate)
    assert(os1.getExpireDate == sdf1.parse("2016-08-26 14:38:06").getTime)

    assert(os2.getServiceType == ServiceType.COLOR)
    assert(os2.getCreated == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(os2.getIsActive)
    assert(!os2.hasBadge)
    assert(!os2.hasOfferBilling)
    assert(!os2.hasOfferBillingDeadline)

    assert(os3.getServiceType == ServiceType.SPECIAL)
    assert(os3.getCreated == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(os3.getIsActive)
    assert(!os3.hasBadge)
    assert(!os3.hasOfferBilling)
    assert(!os3.hasOfferBillingDeadline)

    assert(os4.getServiceType == ServiceType.TOP)
    assert(os4.getCreated == sdf1.parse("2016-08-21 14:38:06").getTime)
    assert(os4.getIsActive)
    assert(!os4.hasBadge)
    assert(!os4.hasOfferBilling)
    assert(!os4.hasOfferBillingDeadline)

    assert(os5.getServiceType == ServiceType.BADGE)
    assert(os5.getCreated == sdf1.parse("2016-08-21 14:37:15").getTime)
    assert(os5.getIsActive)
    assert(os5.getBadge == "Камера заднего вида")
    assert(!os5.hasOfferBilling)
    assert(!os5.hasOfferBillingDeadline)

    assert(os6.getServiceType == ServiceType.BADGE)
    assert(os6.getCreated == sdf1.parse("2016-08-21 14:37:15").getTime)
    assert(os6.getIsActive)
    assert(os6.getBadge == "Два комплекта резины")
    assert(!os6.hasOfferBilling)
    assert(!os6.hasOfferBillingDeadline)

    assert(os7.getServiceType == ServiceType.BADGE)
    assert(os7.getCreated == sdf1.parse("2016-08-21 14:37:15").getTime)
    assert(os7.getIsActive)
    assert(os7.getBadge == "Кожаный салон")
    assert(!os7.hasOfferBilling)
    assert(!os7.hasOfferBillingDeadline)
  }

  test("testActualizationDate") {
    // убеждаемся, что для объявления без сервисов sale.actualizationDate возвращает createDate
    assert(getSaleByIdFromDb(1043045004).actualizationDate == sdf1.parse("2016-08-14 18:33:14").getTime)

    // объявление с турбо-пакетом и стикерами
    val sale = getSaleByIdFromDb(1043270830)

    // убеждаемся, что sale.actualizationDate возвращает дату от активных сервисов, если они есть
    assert(sale.actualizationDate == sdf1.parse("2016-08-21 14:38:06").getTime)

    // если optOffer = None, то offerConverter.convert ставит timestampTtlStart = sale.actualizationDate
    val offer = convertCarsStrict(sale)
    assert(offer.hasTimestampTtlStart)
    assert(offer.getTimestampTtlStart == sdf1.parse("2016-08-21 14:38:06").getTime)

    // если optOffer = Some(...), и там дата меньше, то offerConverter.convert ставит TimestampTtlStart = sale.actualizationDate
    val optOffer2 = {
      Some(
        getOfferById(1043270830).toBuilder
          .setTimestampTtlStart(sdf1.parse("2016-08-21 14:38:06").getTime - 10000)
          .build()
      )
    }
    val offer2 = convertCarsStrict(sale, optOffer2)
    assert(offer2.hasTimestampTtlStart)
    assert(offer2.getTimestampTtlStart == sdf1.parse("2016-08-21 14:38:06").getTime)

    // если optOffer = Some(...), и там дата больше, то offerConverter.convert ставит TimestampTtlStart = optOffer....
    val optOffer3 = {
      Some(
        getOfferById(1043270830).toBuilder
          .setTimestampTtlStart(sdf1.parse("2016-08-21 14:38:06").getTime + 10000)
          .build()
      )
    }
    val offer3 = convertCarsStrict(sale, optOffer3)
    assert(offer3.hasTimestampTtlStart)
    assert(offer3.getTimestampTtlStart == sdf1.parse("2016-08-21 14:38:06").getTime + 10000)
  }

  /**
    * Проверим методы userRef, offerId, offerRef
    */
  test("test UserRef, OfferId, OfferRef") {
    // объявление не от автосалона
    val sale1 = getSaleByIdFromDb(1043045004)
    assert(sale1.userRef == UserRefAutoru(10591660))
    assert(sale1.offerId == "1043045004-977b3")
    assert(sale1.offerRef == OfferRef("a_10591660", "1043045004-977b3"))
    // объявление от автосалона
    val sale2 = getSaleByIdFromDb(1037186558)
    assert(sale2.userRef == UserRefAutoruClient(21029))
    assert(sale2.offerId == "1037186558-e06d8")
    assert(sale2.offerRef == OfferRef("ac_21029", "1037186558-e06d8"))

    // объявление с пустым хешом
    val sale3 = sale1.copy(hash = "")
    assert(sale3.offerId == "1043045004")
    assert(sale3.offerRef == OfferRef("a_10591660", "1043045004"))
  }

  test("convert phones from SalonPoiContacts if edit_contacts is enabled") {
    val salonContactPhone1 = "74732024115"
    val salonPhone1 = "74732284736"
    val salonPhone2 = "74732284936"

    val salonContacts = SalonPoiContacts(
      13025283,
      10254,
      "Отдел продаж",
      Seq(
        SalonPoiContacts.SalonContact(
          29163308,
          13025283,
          "Менеджер",
          salonContactPhone1,
          "1:3:7",
          LocalTime.parse("09:00:00.000"),
          LocalTime.parse("21:00:00.000")
        )
      )
    )

    val salon = SalonPoi(
      9615,
      None,
      1,
      None,
      None,
      None,
      None,
      None,
      None,
      Some("Выборгское шоссе, д.23, корп.1, литера А"),
      Some(60.059559),
      Some(30.306742),
      DateTime.now,
      DateTime.now(),
      Map.empty,
      salonPhones = Seq(
        SalonPoiContacts.PoiPhone(432234, 4828, None, salonPhone1, "1:3:7", Some(9), Some(20)),
        SalonPoiContacts.PoiPhone(432236, 4828, None, salonPhone2, "1:3:7", Some(9), Some(20))
      ),
      client = Some(
        SalonPoiContacts.SalonClient(
          123,
          productId = 2,
          countryId = None,
          regionId = None,
          cityId = None,
          yaCountryId = None,
          yaRegionId = None,
          yaCityId = None,
          url = "",
          email = "",
          phone = None,
          phoneMask = None,
          fax = None,
          faxMask = None,
          description = "",
          isAgent = false,
          agentId = 0L,
          status = "active",
          createDate = DateTime.now,
          contactName = "",
          address = "",
          isGoldPartner = false,
          usePremiumTariff = false,
          newBillingAvailable = false,
          callsAuctionAvailable = false,
          idClients = None,
          name = None
        )
      ),
      oldContactIds = Seq.empty,
      callTrackingOn = false,
      virtualPhones = Map.empty
    )

    val sale1 = someSale.copy(
      id = 1043685236,
      hash = "8c03f3",
      salonPoiContacts = Some(salonContacts),
      salonPoi = Some(salon.withCanEditContacts(true))
    )
    val offer1 = convertCarsStrict(sale1)

    val seller1 = offer1.getOfferAutoru.getSeller
    seller1.getPhoneCount shouldBe 1
    seller1.getPhone(0).getNumber shouldBe salonContactPhone1

    val sale2 = someSale.copy(
      id = 1043685236,
      hash = "8c03f3",
      salonPoiContacts = Some(salonContacts),
      salonPoi = Some(salon.withCanEditContacts(false))
    )
    val offer2 = convertCarsStrict(sale2)

    val seller2 = offer2.getOfferAutoru.getSeller
    seller2.getPhoneCount shouldBe 2
    seller2.getPhone(0).getNumber shouldBe salonPhone1
    seller2.getPhone(1).getNumber shouldBe salonPhone2
  }

  test("convert address from sale.poi if edit_address is enabled") {
    val salonAddress = "Выборгское шоссе, д.23, корп.1, литера А"
    val salonLat = 60.059559
    val salonLon = 30.306742
    val salonRegionId = 2L
    val customAddress = "Нововыборгская набережная, д. 13"
    val customLat = 65.059559
    val customLon = 31.306742
    val customRegionId = 9999L

    val salon = SalonPoi(
      9615,
      None,
      1,
      None,
      None,
      None,
      None,
      None,
      Some(salonRegionId),
      Some(salonAddress),
      latitude = Some(salonLat),
      longitude = Some(salonLon),
      DateTime.now,
      DateTime.now(),
      Map.empty,
      salonPhones = Seq.empty,
      client = Some(
        SalonPoiContacts.SalonClient(
          123,
          productId = 2,
          countryId = None,
          regionId = None,
          cityId = None,
          yaCountryId = None,
          yaRegionId = None,
          yaCityId = None,
          url = "",
          email = "",
          phone = None,
          phoneMask = None,
          fax = None,
          faxMask = None,
          description = "",
          isAgent = false,
          agentId = 0L,
          status = "active",
          createDate = DateTime.now,
          contactName = "",
          address = "",
          isGoldPartner = false,
          usePremiumTariff = false,
          newBillingAvailable = false,
          callsAuctionAvailable = false,
          idClients = None,
          name = None
        )
      ),
      oldContactIds = Seq.empty,
      callTrackingOn = false,
      virtualPhones = Map.empty
    )

    val poi = AutoruPoi(
      0,
      None,
      None,
      None,
      None,
      None,
      yaCityId = Some(customRegionId),
      address = Some(customAddress),
      latitude = Some(customLat),
      longitude = Some(customLon)
    )

    val sale1 =
      someSale.copy(id = 1043685236, hash = "8c03f3", poi = Some(poi), salonPoi = Some(salon.withCanEditAddress(true)))
    val offer1 = convertCarsStrict(sale1)

    val seller1 = offer1.getOfferAutoru.getSeller
    seller1.getPlace.getAddress shouldBe customAddress
    seller1.getPlace.getGeobaseId shouldBe customRegionId
    seller1.getPlace.getCoordinates.getLatitude shouldBe customLat
    seller1.getPlace.getCoordinates.getLongitude shouldBe customLon

    val sale2 =
      someSale.copy(id = 1043685236, hash = "8c03f3", poi = Some(poi), salonPoi = Some(salon.withCanEditAddress(false)))
    val offer2 = convertCarsStrict(sale2)

    val seller2 = offer2.getOfferAutoru.getSeller
    seller2.getPlace.getAddress shouldBe salonAddress
    seller2.getPlace.getGeobaseId shouldBe salonRegionId
    seller2.getPlace.getCoordinates.getLatitude shouldBe salonLat
    seller2.getPlace.getCoordinates.getLongitude shouldBe salonLon
  }

  test("do not disturb") {
    val sale = someSale.copy(
      newClientId = 0L,
      user = Some(
        AutoruUser(
          id = 18318774,
          email = Some("superscalper@yandex.ru"),
          phones = Seq(
            UserPhone(
              id = 32624814,
              userId = Some(18318774),
              number = None,
              phone = 79161793608L,
              status = 1,
              isMain = false,
              code = 0,
              created = new DateTime(2016, 8, 21, 14, 36, 31, 0),
              updated = None
            )
          )
        )
      ),
      settings = Some(someSale.settings.toSeq.flatten :+ Setting(someSale.id, SettingAliases.NOTDISTURB, 458L, "1"))
    )

    val offer = convertCarsStrict(sale)

    val seller = offer.getOfferAutoru.getSeller
    seller.getDoNotDisturb shouldBe true
  }

  test("phones redirect") {
    val data = Seq(
      1044216699 -> true,
      1044214673 -> false,
      1043211458 -> false
    )
    forEvery(data) {
      case (saleId, havePhoneRedirect) =>
        val sale = getSaleByIdFromDb(saleId)
        assert(sale.phonesRedirect.exists(_.active) == havePhoneRedirect, s"phones redirect failed for sale $saleId")
        assert(
          convertCarsStrict(sale).getOfferAutoru.getRedirectPhones == havePhoneRedirect,
          s"phones redirect convert failed for sale $saleId"
        )
    }
  }

  test("DiscountPrice") {

    val sale = components.autoruSalesDao.getOffer(1037186558).value
    val discountPrice = convertCarsStrict(sale, None).getOfferAutoru.getDiscountPrice

    assert(discountPrice.getPrice == 100000)
    assert(discountPrice.getStatus.getNumber == 1)
  }

  test("Discount Options") {

    val sale = components.autoruSalesDao.getOffer(1044159039).value
    val discountOptions = convertCarsStrict(sale, None).getOfferAutoru.getDiscountOptions

    assert(discountOptions.getCredit == 100)
    assert(discountOptions.getInsurance == 200)
    assert(discountOptions.getTradein == 300)
  }

  // проверяем, что статус проверки ютуб видео подтянется из существующего объявления
  test("youtubeStatusMerge") {

    val offer = getOfferById(1043026846)
    val builder = offer.toBuilder
    val videoBuilder = builder.getOfferAutoru.getVideo(0).toBuilder
    videoBuilder.getYoutubeVideoBuilder.setStatus(YoutubeVideoStatus.AVAILABLE)
    videoBuilder.getYoutubeVideoBuilder.setLastCheckMoment(1482335072153L)
    builder.getOfferAutoruBuilder.setVideo(0, videoBuilder.build())
    val updatedOffer = builder.build()
    val sale = components.autoruSalesDao.getOffer(1043026846).value
    val converted = convertCarsStrict(sale, Some(updatedOffer))
    val video = converted.getOfferAutoru.getVideo(0)
    assert(video.hasYoutubeVideo)
    assert(!video.hasYandexVideo)
    assert(video.getCreated == 1471870910000L)
    val youtubeVideo = video.getYoutubeVideo
    assert(youtubeVideo.getYoutubeId == "6cylm0mka_c")
    assert(youtubeVideo.getStatus == YoutubeVideoStatus.AVAILABLE)
    assert(youtubeVideo.getLastCheckMoment == 1482335072153L)

    // если никакого существующего объявления нет, то статус должен быть UNKNOWN
    val converted2 = convertCarsStrict(sale, None)
    val video2 = converted2.getOfferAutoru.getVideo(0)
    assert(video2.hasYoutubeVideo)
    assert(!video2.hasYandexVideo)
    assert(video2.getCreated - System.currentTimeMillis() < 1000)
    val youtubeVideo2 = video2.getYoutubeVideo
    assert(youtubeVideo2.getYoutubeId == "6cylm0mka_c")
    assert(youtubeVideo2.getStatus == YoutubeVideoStatus.UNKNOWN)
    assert(!youtubeVideo2.hasLastCheckMoment)
  }

  test("Unknown Availability") {

    val x: AutoruSale = components.autoruSalesDao.getOffer(1044214673).value
    assert(x.settings.getOrElse(Seq.empty).map(_.alias).contains(SettingAliases.AVAILABILITY))
    // делаем unknown availability
    val sale = x.copy(
      settings = x.settings.map(seq =>
        seq.map(s =>
          if (s.alias == SettingAliases.AVAILABILITY) {
            s.copy(value = "0")
          } else s
        )
      )
    )
    assert(convertCarsStrict(sale, None).getOfferAutoru.getAvailability == Availability.AVAILABILITY_UNKNOWN)
  }

  test("keep original images") {

    val saleId: Long = 1044216699

    val sale = components.autoruSalesDao.getOffer(saleId).value
    val offerOld = TestUtils.createOffer()
    assert(offerOld.getOfferAutoru.getPhotoList.isEmpty) // VOS-2980 images not converted

    val photo1 = TestUtils.createPhoto("1-a", main = true, 1).build()
    val photo2 = TestUtils
      .createPhoto("2-b", main = false, 2)
      .setExternalUrl("abc")
      .build()

    offerOld.getOfferAutoruBuilder
      .addPhoto(photo1)
      .addPhoto(photo2)

    val offer = components.carOfferConverter.convertStrict(sale, Some(offerOld.build)).converted.value

    assert(offer.getOfferAutoru.getPhotoCount == 2)
    assert(offer.getOfferAutoru.getPhotoList.asScala == Seq(photo1, photo2))
  }

  test("keep reseller flag after conversion") {

    val id = 1043026846
    forAll(
      Table(
        "reseller",
        Some(true),
        Some(false),
        None
      )
    ) { value =>
      val offer = getOfferById(id).toBuilder
      value.foreach(offer.getOfferAutoruBuilder.setReseller)
      val updatedOffer = offer.build()
      val sale = components.autoruSalesDao.getOffer(id).value

      val converted = convertCarsStrict(sale, Some(updatedOffer))

      value match {
        case None =>
          converted.getOfferAutoru.hasReseller shouldBe false
        case Some(reseller) =>
          converted.getOfferAutoru.hasReseller shouldBe true
          converted.getOfferAutoru.getReseller shouldBe reseller
      }
    }
  }

  test("registered_in_russia conversion") {
    val sale: AutoruSale = components.autoruSalesDao.getOfferForMigration(1044216699).value
    val offer = convertCarsStrict(sale, None).toBuilder
    offer.getOfferAutoruBuilder.getDocumentsBuilder.setNotRegisteredInRussia(true).build()
    val offer2 = convertCarsStrict(sale, Some(offer.build()))

    assert(offer2.getOfferAutoru.getDocuments.getNotRegisteredInRussia)
  }

  test("convert ip") {
    val sale: AutoruSale = components.autoruSalesDao.getOfferForMigration(1044214673).value
    val offer: Offer = convertCarsStrict(sale, None)
    assert(offer.getOfferAutoru.getIp == "95.220.131.125")
  }

  test("car allow_photo_reorder") {

    val sale = components.autoruSalesDao.getOffer(1037186558).value
    val salonPoi = sale.salonPoi.value

    val saleWithFalseAllowPhotoReorder = sale.copy(salonPoi = Some(sale.salonPoi.value.withAllowPhotoReorder(false)))

    val offer = convertCarsStrict(saleWithFalseAllowPhotoReorder)
    assert(!offer.getOfferAutoru.getSalon.getAllowPhotoReorder)

    val saleWithTrueAllowPhotoReorder = sale.copy(salonPoi = Some(sale.salonPoi.value.withAllowPhotoReorder(true)))

    val newOffer = convertCarsStrict(saleWithTrueAllowPhotoReorder)
    assert(newOffer.getOfferAutoru.getSalon.getAllowPhotoReorder)
  }

  test("trucks conversion, services") {

    val sale1 = components.autoruTrucksDao.getOffer(6229746L).value
    val offer1 = convertTrucksStrict(sale1, None)
    assert(offer1.getOfferAutoru.getServicesCount > 0)
  }

  test("user Contacts in trucks") {

    val sale1 = components.autoruTrucksDao.getOffer(6229746L).value
    val offer1 = convertTrucksStrict(sale1, None)
    assert(offer1.getUser.hasUserContacts)
    assert(offer1.getUser.getUserContacts.getEmail == "igor.2410@yandex.ru")
    assert(offer1.getUser.getUserContacts.getPhonesCount == 3)
    assert(offer1.getUser.getUserContacts.getPhones(0).getNumber == "79252425950")
    assert(offer1.getUser.getUserContacts.getPhones(1).getNumber == "79299621531")
    assert(offer1.getUser.getUserContacts.getPhones(2).getNumber == "79265762480")
  }

  test("dealers conversion in trucks") {

    val sale1 = components.autoruTrucksDao.getOffer(6542117L).value
    val offer1 = convertTrucksStrict(sale1, None)
    assert(offer1.getOfferAutoru.getSeller.getUserRef == "ac_24813")
    assert(offer1.getOfferAutoru.getSeller.getUserName == "Ориент")
    assert(offer1.getOfferAutoru.getSalon.getSalonHash == "a579")

    assert(offer1.getOfferAutoru.getSeller.getPhoneCount == 1)
    assert(offer1.getOfferAutoru.getSeller.getPhone(0).getTitle == "Менеджер")
    assert(offer1.getOfferAutoru.getSeller.getPhone(0).getNumber == "78124263564")
    assert(offer1.getOfferAutoru.getSeller.getPhone(0).getCallHourStart == 9)
    assert(offer1.getOfferAutoru.getSeller.getPhone(0).getCallHourEnd == 22)
    assert(offer1.getOfferAutoru.getSeller.getPlace.getAddress == "Софийская ул., 6")
    assert(offer1.getOfferAutoru.getSeller.getPlace.getCoordinates.getLatitude == 59.886242)
    assert(offer1.getOfferAutoru.getSeller.getPlace.getCoordinates.getLongitude == 30.386539)
    assert(offer1.getOfferAutoru.getSeller.getPlace.getGeobaseId == 2)
  }

  test("tags") {

    val sale = components.autoruSalesDao.getOffer(1043045004).value
    val offerBuilder = convertCarsStrict(sale).toBuilder
    offerBuilder.addAllTag(Seq("tag1", "tag2", "tag3").asJava)
    val offer = offerBuilder.build()

    val offer2 = convertCarsStrict(sale, Some(offer))
    assert(offer2.getTagList.asScala.toList == List("tag1", "tag2", "tag3"))
  }

  test("autoservice review info") {
    val sale1 = getSaleByIdFromDb(1043270830L)
    val convertedSale = convertCarsStrict(sale1)
    val autoserviceInfo = CommonModel.AutoserviceReviewInfo
      .newBuilder()
      .setAutoserviceId("autoservice_123")
      .setReviewId("review_213")
      .build()
    assert(convertedSale.getOfferAutoru.getAutoserviceReviewCount == 1)
    assert(convertedSale.getOfferAutoru.getAutoserviceReview(0) == autoserviceInfo)

    val sale2 = getSaleByIdFromDb(1044216699L)
    assert(convertCarsStrict(sale2).getOfferAutoru.getAutoserviceReviewCount == 0)
  }

  def checkPhotoTransforms(offer: Offer, offer2: Offer): Unit = {
    assert(offer.getOfferAutoru.getPhotoCount == offer2.getOfferAutoru.getPhotoCount + 1)
    val photo1: Photo = offer.getOfferAutoru.getPhoto(0)
    val photo2: Photo = offer2.getOfferAutoru.getPhoto(0)
    assert(photo2.getCurrentTransform.getAngle == photo1.getCurrentTransform.getAngle)
    assert(photo2.getCurrentTransform.getBlur == photo1.getCurrentTransform.getBlur)
    assert(photo2.getOrigName == photo1.getOrigName)
    assert(photo2.getTransformHistoryCount == photo1.getTransformHistoryCount)
    photo2.getTransformHistoryList.asScala.zip(photo1.getTransformHistoryList.asScala).zipWithIndex.foreach {
      case ((h1, h2), idx) =>
        assert(h1.getName == h2.getName, s"$idx transform history name is wrong")
        assert(h1.getTransform.getAngle == h2.getTransform.getAngle, s"$idx transform history angle is wrong")
        assert(h1.getTransform.getBlur == h2.getTransform.getBlur, s"$idx transform history blur is wrong")
    }
  }

  test("Source platform") {

    val sale1 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
    val offerBuilder1 = convertCarsStrict(sale1).toBuilder
    val offer1 = offerBuilder1.build()

    assert(offer1.getOfferAutoru.getSourceInfo.getPlatform == SourceInfo.Platform.DESKTOP)

    val sale2 = components.autoruSalesDao.getOfferForMigration(1044159039L).value
    val offerBuilder2 = convertCarsStrict(sale2).toBuilder
    val offer2 = offerBuilder2.build()

    assert(!offer2.getOfferAutoru.hasSourceInfo)

    // для траксов храним источник в вос и не теряем при перемиграции
    val sale3 = components.autoruTrucksDao.getOffer(6229746L).value
    val offer3 = convertTrucksStrict(sale3, None)

    assert(!offer3.getOfferAutoru.hasSourceInfo)

    val sourceInfo = SourceInfo
      .newBuilder()
      .setPlatform(SourceInfo.Platform.ANDROID)
      .setSource(SourceInfo.Source.AUTO_RU)
      .setUserRef("a_123")
      .build()
    val offer4Builder = offer3.toBuilder
    offer4Builder.getOfferAutoruBuilder.setSourceInfo(sourceInfo)
    val offer4 = offer4Builder.build()

    val offer5 = convertTrucksStrict(sale3, Some(offer4))
    assert(offer5.getOfferAutoru.getSourceInfo == sourceInfo)
  }

  test("prices history for trucks") {

    // история цен хранится в восе. При миграции она не теряется и туда вписывается еще цена
    val sale = components.autoruTrucksDao.getOffer(6229746L).value
    val offer1 = convertTrucksStrict(sale.copy(price = 1000), None)
    val offer2 = convertTrucksStrict(sale.copy(price = 1001), Some(offer1))
    val offer3 = convertTrucksStrict(sale.copy(price = 1002), Some(offer2))
    val offer4 = convertTrucksStrict(sale.copy(price = 1003), Some(offer3))
    val offer5 = convertTrucksStrict(sale.copy(price = 1003), Some(offer4)) // тут цена не поменялась

    assert(offer5.getOfferAutoru.getPriceHistoryCount == 4)
    assert(offer5.getOfferAutoru.getPriceHistory(0).getPrice == 1000)
    assert(offer5.getOfferAutoru.getPriceHistory(1).getPrice == 1001)
    assert(offer5.getOfferAutoru.getPriceHistory(2).getPrice == 1002)
    assert(offer5.getOfferAutoru.getPriceHistory(3).getPrice == 1003)
  }

  test("recall reason for trucks") {

    // причину скрытия храним только в вос и не теряем при миграции
    val sale = components.autoruTrucksDao.getOffer(6229746L).value
    val offer1 = convertTrucksStrict(sale, None)
    assert(offer1.getOfferAutoru.getRecallInfo.getReason == CommonModel.RecallReason.RECALL_REASON_UNKNOWN)

    val offer2Builder = offer1.toBuilder
    offer2Builder.getOfferAutoruBuilder.getRecallInfoBuilder
      .setReason(CommonModel.RecallReason.RETHINK)
      .setRecallTimestamp(ru.yandex.vos2.getNow)
    val offer2 = offer2Builder.build()

    val offer3 = convertTrucksStrict(sale, Some(offer2))
    assert(offer3.getOfferAutoru.getRecallInfo.getReason == CommonModel.RecallReason.RETHINK)
  }

  test("unconfirmed email") {
    // не теряем этот имейл при миграции
    {
      val sale1 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
      val offerBuilder1 = convertCarsStrict(sale1).toBuilder
      offerBuilder1.getOfferAutoruBuilder.getSellerBuilder.setUnconfirmedEmail("xxx@yyy.ru")
      val offer1 = offerBuilder1.build()
      val offer2 = convertCarsStrict(sale1, Some(offer1))
      assert(offer2.getOfferAutoru.getSeller.getUnconfirmedEmail == "xxx@yyy.ru")
      val offer3 =
        convertCarsStrict(sale1.copy(email = Some(SaleEmail(0, sale1.id, "zzz@yyy.ru", "hash1"))), Some(offer1))
      assert(offer3.getOfferAutoru.getSeller.getUnconfirmedEmail == "zzz@yyy.ru")
    }

    {
      {
        val sale1 = components.autoruTrucksDao.getOffer(6229746L).value
        val offerBuilder1 = convertTrucksStrict(sale1).toBuilder
        offerBuilder1.getOfferAutoruBuilder.getSellerBuilder.setUnconfirmedEmail("xxx@yyy.ru")
        val offer1 = offerBuilder1.build()
        val offer2 = convertTrucksStrict(sale1, Some(offer1))
        assert(offer2.getOfferAutoru.getSeller.getUnconfirmedEmail == "xxx@yyy.ru")
      }
    }
  }

  test("truck trailer types trailer_moto and overpass") {

    val sale1 = components.autoruTrucksDao.getOffer(6229746L).value.copy(trailerType = 1468)
    val offer1 = convertTrucksStrict(sale1)
    assert(offer1.getOfferAutoru.getTruckInfo.getTrailerType == TrailerType.TRUCK_TRAILER_OVERPASS)

    val sale2 = components.autoruTrucksDao.getOffer(6229746L).value.copy(trailerType = 1470)
    val offer2 = convertTrucksStrict(sale2)
    assert(offer2.getOfferAutoru.getTruckInfo.getTrailerType == TrailerType.TRUCK_TRAILER_MOTO_TRAILER)
  }

  test("truck unknown trailer strict non strict conversion") {

    val sale1 = components.autoruTrucksDao.getOffer(6229746L).value.copy(trailerType = 100500)
    val offer1 = convertTrucksNonStrict(sale1)
    assert(!offer1.getOfferAutoru.getTruckInfo.hasTrailerType)
    assert(offer1.getOfferAutoru.getConversionError(0) == "Unexpected trailer for offer 6229746-6d65: 100500")

    val result = components.truckOfferConverter.convertStrict(sale1, None)
    assert(result.converted.isEmpty)
  }

  test("do not lose status history") {
    // проверим, что история статусов не теряется
    val sale1 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
    val offerBuilder1 = convertCarsStrict(sale1).toBuilder
    offerBuilder1
      .addStatusHistoryBuilder()
      .setOfferStatus(CompositeStatus.CS_ACTIVE)
      .setComment("test")
      .setTimestamp(0L)
    val offer1 = offerBuilder1.build()
    val offer2 = convertCarsStrict(sale1, Some(offer1))
    assert(offer2.getStatusHistoryCount == 1)
    assert(offer2.getStatusHistory(0).getOfferStatus == CompositeStatus.CS_ACTIVE)
    assert(offer2.getStatusHistory(0).getComment == "test")
    assert(offer2.getStatusHistory(0).getTimestamp == 0L)
  }

  test("trucks cabinType = 6") {

    // https://st.yandex-team.ru/AUTORUAPPS-4825
    val sale = components.autoruTrucksDao.getOffer(6229746L).value.copy(cabinKey = Some(6), categoryId = 34)
    val offer = convertTrucksNonStrict(sale)
    assert(offer.getOfferAutoru.getTruckInfo.hasCabinType)
    assert(offer.getOfferAutoru.getTruckInfo.getCabinType == CabinType.SEAT_6)
  }

  test("truck contact_id is zero") {

    val sale = components.autoruTrucksDao.getOffer(6542117L).value
    assert(sale.contactId == 0, "Expect zero contact_id in test data")
    val offer = convertTrucksNonStrict(sale)

    assert(offer.getUserRef == "ac_24813")
    assert(offer.getOfferAutoru.getSalon.getSalonId == "19063")
  }

  test("truck sale_edit_contacts is disabled") {

    val sale = components.autoruTrucksDao.getOffer(6542117L).value
    val salon = sale.dealer.value.salon

    val offer = convertTrucksStrict(sale)

    offer.getOfferAutoru.getSeller.getPhoneCount shouldBe 1
    offer.getOfferAutoru.getSeller.getPhone(0).getNumber shouldBe "78124263564"

    val newSalon = salon.withCanEditContacts(true)
    val newSale = sale.withSalon(newSalon)

    val newOffer = convertTrucksStrict(newSale)

    newOffer.getOfferAutoru.getSeller.getPhoneCount shouldBe 1
    newOffer.getOfferAutoru.getSeller.getPhone(0).getNumber shouldBe "79218498363"
  }

  test("truck sale_edit_address is disabled") {

    val rawSale = components.autoruTrucksDao.getOffer(6542117L).value
    val salon = rawSale.dealer.value.salon
      .withCanEditAddress(value = false)
      .copy(
        yaCityId = Some(213L),
        salonAddress = Some("Some test address"),
        latitude = Some(32d),
        longitude = Some(-54d)
      )
    val sale = rawSale
      .withSalon(salon)
      .copy(
        yaRegionId = Some(999L),
        coord = GeoCoordinates(lat = 12d, lon = 33d),
        address = Some("Софийская ул., 6")
      )
    val offer = convertTrucksStrict(sale)

    offer.getOfferAutoru.getSeller.getPlace.getGeobaseId shouldBe 213L
    offer.getOfferAutoru.getSeller.getPlace.getCoordinates.getLatitude shouldBe 32d +- 0.001d
    offer.getOfferAutoru.getSeller.getPlace.getCoordinates.getLongitude shouldBe -54d +- 0.001d
    offer.getOfferAutoru.getSeller.getPlace.getAddress shouldBe "Some test address"

    val newSalon = salon.withCanEditAddress(value = true)
    val newSale = sale.withSalon(newSalon)
    val newOffer = convertTrucksStrict(newSale)

    newOffer.getOfferAutoru.getSeller.getPlace.getGeobaseId shouldBe 999L
    newOffer.getOfferAutoru.getSeller.getPlace.getCoordinates.getLatitude shouldBe 12d +- 0.001d
    newOffer.getOfferAutoru.getSeller.getPlace.getCoordinates.getLongitude shouldBe 33d +- 0.001d
    newOffer.getOfferAutoru.getSeller.getPlace.getAddress shouldBe "Софийская ул., 6"
  }

  test("trucks migration with wheel_drive 1486") {

    val sale = components.autoruTrucksDao.getOffer(6542117L).value.copy(wheelDrive = 1486)
    val offer = convertTrucksStrict(sale)
    offer.getOfferAutoru.getTruckInfo.getWheelDrive == WheelDrive.WD_4_2
  }

  test("truck allow_photo_reorder") {

    val sale = components.autoruTrucksDao.getOffer(6542117L).value
    val salonPoi = sale.dealer.map(_.salon).value

    val salonWithFalseAllowPhotoReorder = sale.dealer.value.salon.withAllowPhotoReorder(false)
    val saleWithFalseAllowPhotoReoreder = sale.withSalon(salonWithFalseAllowPhotoReorder)

    val offer = convertTrucksStrict(saleWithFalseAllowPhotoReoreder)
    assert(!offer.getOfferAutoru.getSalon.getAllowPhotoReorder)

    val salonWithTrueAllowPhotoReorder = sale.dealer.value.salon.withAllowPhotoReorder(true)
    val saleWithTrueAllowPhotoReoreder = sale.withSalon(salonWithTrueAllowPhotoReorder)

    val newOffer = convertTrucksStrict(saleWithTrueAllowPhotoReoreder)
    assert(newOffer.getOfferAutoru.getSalon.getAllowPhotoReorder)
  }

  test("migrate feedprocessor_unique_id") {
    val sale1 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
    val offerBuilder1 = convertCarsStrict(sale1).toBuilder
    offerBuilder1.getOfferAutoruBuilder.setFeedprocessorUniqueId("AABBCC")
    val offer1 = offerBuilder1.build()
    val offer2 = convertCarsStrict(sale1, Some(offer1))

    assert(offer2.getOfferAutoru.getFeedprocessorUniqueId.nonEmpty)
    assert(offer2.getOfferAutoru.getFeedprocessorUniqueId == offer1.getOfferAutoru.getFeedprocessorUniqueId)
  }

  test("save last phone") {
    val sale1 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
    val offer0 = convertCarsStrict(sale1.copy(phones = None))
    assert(offer0.getOfferAutoru.getLastPhone == "")
    val offer1 = convertCarsStrict(sale1)
    assert(offer1.getOfferAutoru.getLastPhone == "79255188588")
    val offer2 = convertCarsStrict(sale1.copy(phones = None), Some(offer1))
    assert(offer2.getOfferAutoru.getLastPhone == "79255188588")
  }

  test("do not lose parsing info") {

    val sale = components.autoruTrucksDao.getOffer(6542117L).value
    val offer = convertTrucksStrict(sale)
    val b = offer.toBuilder
    b.getOfferAutoruBuilder.getParsingInfoBuilder.addFailedPhotosUpload("photo1")
    b.getOfferAutoruBuilder.getParsingInfoBuilder.setPhotosUploaded(true)
    val offer2 = convertTrucksStrict(sale, Some(b.build()))
    assert(offer2.getOfferAutoru.hasParsingInfo)
    assert(offer2.getOfferAutoru.getParsingInfo.getPhotosUploaded)
    assert(offer2.getOfferAutoru.getParsingInfo.getFailedPhotosUploadCount == 1)
    assert(offer2.getOfferAutoru.getParsingInfo.getFailedPhotosUpload(0) == "photo1")
  }

  test("comtrans pts original") {
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateSettings.name, true)
    val sale = components.autoruTrucksDao.getOffer(6542117L).value
    val offer = convertTrucksStrict(sale)
    val b = offer.toBuilder

    val offer2 = convertTrucksStrict(sale, Some(b.build()))
    assert(!offer2.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(!offer2.getOfferAutoru.getDocuments.hasPtsStatus)

    b.getOfferAutoruBuilder.getDocumentsBuilder.setIsPtsOriginal(false)
    val offer3 = convertTrucksStrict(sale, Some(b.build()))
    assert(offer3.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(!offer3.getOfferAutoru.getDocuments.getIsPtsOriginal)
    assert(offer3.getOfferAutoru.getDocuments.hasPtsStatus)
    assert(offer3.getOfferAutoru.getDocuments.getPtsStatus == PtsStatus.DUPLICATE)

    b.getOfferAutoruBuilder.getDocumentsBuilder.setIsPtsOriginal(true)
    val offer4 = convertTrucksStrict(sale, Some(b.build()))
    assert(offer4.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(offer4.getOfferAutoru.getDocuments.getIsPtsOriginal)
    assert(offer4.getOfferAutoru.getDocuments.hasPtsStatus)
    assert(offer4.getOfferAutoru.getDocuments.getPtsStatus == PtsStatus.ORIGINAL)

    b.getOfferAutoruBuilder.getDocumentsBuilder.clearIsPtsOriginal()
    val offer5 = convertTrucksStrict(sale, Some(b.build()))
    assert(!offer5.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(!offer5.getOfferAutoru.getDocuments.hasPtsStatus)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateSettings.name, false)
  }

  test("comtrans pts status") {
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateSettings.name, true)
    val sale = components.autoruTrucksDao.getOffer(6542117L).value
    val offer = convertTrucksStrict(sale)
    val b = offer.toBuilder

    val offer2 = convertTrucksStrict(sale, Some(b.build()))
    assert(!offer2.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(!offer2.getOfferAutoru.getDocuments.hasPtsStatus)

    b.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.DUPLICATE)
    val offer3 = convertTrucksStrict(sale, Some(b.build()))
    assert(offer3.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(!offer3.getOfferAutoru.getDocuments.getIsPtsOriginal)
    assert(offer3.getOfferAutoru.getDocuments.hasPtsStatus)
    assert(offer3.getOfferAutoru.getDocuments.getPtsStatus == PtsStatus.DUPLICATE)

    b.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.ORIGINAL)
    val offer4 = convertTrucksStrict(sale, Some(b.build()))
    assert(offer4.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(offer4.getOfferAutoru.getDocuments.getIsPtsOriginal)
    assert(offer4.getOfferAutoru.getDocuments.hasPtsStatus)
    assert(offer4.getOfferAutoru.getDocuments.getPtsStatus == PtsStatus.ORIGINAL)

    b.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.NO_PTS)
    val offer5 = convertTrucksStrict(sale, Some(b.build()))
    assert(!offer5.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(offer5.getOfferAutoru.getDocuments.hasPtsStatus)
    assert(offer5.getOfferAutoru.getDocuments.getPtsStatus == PtsStatus.NO_PTS)

    b.getOfferAutoruBuilder.getDocumentsBuilder.clearPtsStatus()
    val offer6 = convertTrucksStrict(sale, Some(b.build()))
    assert(!offer6.getOfferAutoru.getDocuments.hasIsPtsOriginal)
    assert(!offer6.getOfferAutoru.getDocuments.hasPtsStatus)
    components.featureRegistry.updateFeature(components.featuresManager.DontMigrateSettings.name, false)
  }

  test("do not lose ban reasons") {
    val sale1 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
    val offer0 = convertCarsStrict(sale1).toBuilder.addReasonsBan("test").build()
    val offer1 = convertCarsStrict(sale1, Some(offer0))
    assert(offer1.getReasonsBanCount == 1)
    assert(offer1.getReasonsBan(0) == "test")
  }

  test("cars license plate migration") {
    val sale0 = components.autoruSalesDao.getOfferForMigration(1043045004L).value
    val sale1 = sale0.copy(
      settings = Some(sale0.settings.value :+ Setting(sale0.id, SettingAliases.GOS_NUMBER, 490, "Х936РА77"))
    )
    val offer1 = convertCarsStrict(sale1)
    assert(offer1.getOfferAutoru.getDocuments.getLicensePlate == "Х936РА77")
    val sale2 = sale0.copy(
      settings = Some(sale0.settings.value :+ Setting(sale0.id, SettingAliases.GOS_NUMBER, 490, "Х512АА77"))
    )
    val offer2 = convertCarsStrict(sale2, Some(offer1))
    assert(offer2.getOfferAutoru.getDocuments.getLicensePlate == "Х512АА77")
  }

  test("trucks license plate migration") {

    val sale1: AutoruTruck = components.autoruTrucksDao.getOffer(6542117L).value
    val b = convertTrucksStrict(sale1).toBuilder
    b.getOfferAutoruBuilder.getDocumentsBuilder.setLicensePlate("Х936РА77")
    val offer1 = b.build()
    val offer2 = convertTrucksStrict(sale1, Some(offer1))
    assert(offer2.getOfferAutoru.getDocuments.getLicensePlate == "Х936РА77")
  }

  test("comtrans discount options") {
    val sale = components.autoruTrucksDao.getOffer(6542117L).value
    val offer = convertTrucksStrict(sale)
    val b = offer.toBuilder

    val offer2 = convertTrucksStrict(sale, Some(b.build()))
    assert(!offer2.getOfferAutoru.hasDiscountOptions)

    b.getOfferAutoruBuilder.setDiscountOptions(
      DiscountOptions
        .newBuilder()
        .setCredit(100)
        .setTradein(200)
        .setInsurance(300)
        .setMaxDiscount(600)
    )
    val offer3 = convertTrucksStrict(sale, Some(b.build()))
    assert(offer3.getOfferAutoru.getDiscountOptions.getCredit == 100)
    assert(offer3.getOfferAutoru.getDiscountOptions.getTradein == 200)
    assert(offer3.getOfferAutoru.getDiscountOptions.getInsurance == 300)
    assert(offer3.getOfferAutoru.getDiscountOptions.getMaxDiscount == 600)
  }

  test("allow_chats_creation") {
    // комтранс, дилер с chat_enabled = 1
    val sale1 = components.autoruTrucksDao.getOffer(6542117L).value
    val offer1 = convertTrucksStrict(sale1)
    assert(offer1.getOfferAutoru.getSeller.getAllowChatsCreation)
    assert(offer1.getOfferAutoru.getSalon.getAllowChatsCreation)

    // комтранс, дилер с chat_enabled = 0
    val sale2 = components.autoruTrucksDao.getOffer(6541923L).value
    val offer2 = convertTrucksStrict(sale2)
    assert(!offer2.getOfferAutoru.getSeller.getAllowChatsCreation)
    assert(!offer2.getOfferAutoru.getSalon.getAllowChatsCreation)

    // легковые, дилер с chat_enabled = 1
    val sale3 = components.autoruSalesDao.getOffer(1042409964L).value
    val offer3 = convertCarsStrict(sale3)
    assert(offer3.getOfferAutoru.getSeller.getAllowChatsCreation)
    assert(offer3.getOfferAutoru.getSalon.getAllowChatsCreation)

    // легковые, дилер с chat_enabled = 0
    val sale4 = components.autoruSalesDao.getOffer(1043211458L).value
    val offer4 = convertCarsStrict(sale4)
    assert(!offer4.getOfferAutoru.getSeller.getAllowChatsCreation)
    assert(!offer4.getOfferAutoru.getSalon.getAllowChatsCreation)

    // легковые, частник с chat_enabled = 0
    val sale5 = components.autoruSalesDao.getOffer(1043045004L).value
    val maybeOfferBuilder = TestUtils.createOffer()
    maybeOfferBuilder.getOfferAutoruBuilder
      .setSellerType(SellerType.PRIVATE)
      .getSellerBuilder
      .setAllowChatsCreation(false)
    val offer5 = convertCarsStrict(sale5, Option(maybeOfferBuilder.build()))
    assert(!offer5.getOfferAutoru.getSeller.getAllowChatsCreation)
    assert(!offer5.getOfferAutoru.getSalon.getAllowChatsCreation)

    // легковые, частник с chat_enabled = 1
    val sale6 = components.autoruSalesDao.getOffer(1043045004L).value
    maybeOfferBuilder.getOfferAutoruBuilder.getSellerBuilder
      .setAllowChatsCreation(true)
    val offer6 = convertCarsStrict(sale6, Option(maybeOfferBuilder.build()))
    assert(offer6.getOfferAutoru.getSeller.getAllowChatsCreation)
    assert(!offer6.getOfferAutoru.getSalon.getAllowChatsCreation)
  }

  test("isBanByInheritance") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer = convertCarsStrict(sale).toBuilder
    assert(!offer.getOfferAutoru.getIsBanByInheritance)
    offer.getOfferAutoruBuilder.setIsBanByInheritance(true)

    val offer2 = convertCarsStrict(sale, Some(offer.build()))
    assert(offer2.getOfferAutoru.getIsBanByInheritance)
  }

  test("moderation editor") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer = convertCarsStrict(sale).toBuilder
    assert(!offer.getOfferAutoru.hasEditor)
    val editor = Editor.newBuilder().setName("test name")
    offer.getOfferAutoruBuilder.setEditor(editor)

    val offer2 = convertCarsStrict(sale, Some(offer.build()))
    assert(offer2.getOfferAutoru.getEditor.getName == "test name")
  }

  test("holocron status") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer = convertCarsStrict(sale).toBuilder
    assert(!offer.hasHolocronStatus)
    val holocronStatus = HolocronStatus.newBuilder().setLastSentChangeVersion(5).setLastSentHolocronHash("hash").build()
    offer.setHolocronStatus(holocronStatus)

    val offer2 = convertCarsStrict(sale, Some(offer.build()))
    assert(offer2.hasHolocronStatus)
    assert(offer2.getHolocronStatus.getLastSentChangeVersion == 5)
    assert(offer2.getHolocronStatus.getLastSentHolocronHash == "hash")
  }

  test("vin edit counter") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer = convertCarsStrict(sale).toBuilder
    offer.getOfferAutoruBuilder.getDocumentsBuilder.setVin("vin1")

    val offer2 = convertCarsStrict(sale, Some(offer.build()))
    assert(offer2.getOfferAutoru.getVinEditCounter == 1)
    assert(offer2.getOfferAutoru.getDocuments.getVin == "ABCDEFGH1JKLM0123")

    val offer3 = convertCarsStrict(sale, Some(offer2))
    assert(offer3.getOfferAutoru.getVinEditCounter == 1)
    assert(offer3.getOfferAutoru.getDocuments.getVin == "ABCDEFGH1JKLM0123")

    val offer4 = offer3.toBuilder
    offer4.getOfferAutoruBuilder.getDocumentsBuilder.setVin("vin1")

    val offer5 = convertCarsStrict(sale, Some(offer4.build()))
    assert(offer5.getOfferAutoru.getVinEditCounter == 2)
    assert(offer5.getOfferAutoru.getDocuments.getVin == "ABCDEFGH1JKLM0123")
  }

  test("license plate edit counter") {
    val sale0 = components.autoruSalesDao.getOffer(1042409964L).value
    val sale1 = sale0.copy(
      settings = Some(sale0.settings.value :+ Setting(sale0.id, SettingAliases.GOS_NUMBER, 490, "Х936РА77"))
    )
    val offer = convertCarsStrict(sale1).toBuilder
    offer.getOfferAutoruBuilder.getDocumentsBuilder.setLicensePlate("licensePlate1")

    val offer2 = convertCarsStrict(sale1, Some(offer.build()))
    assert(offer2.getOfferAutoru.getLicensePlateEditCounter == 1)
    assert(offer2.getOfferAutoru.getDocuments.getLicensePlate == "Х936РА77")

    val offer3 = convertCarsStrict(sale1, Some(offer2))
    assert(offer3.getOfferAutoru.getLicensePlateEditCounter == 1)
    assert(offer3.getOfferAutoru.getDocuments.getLicensePlate == "Х936РА77")

    val offer4 = offer3.toBuilder
    offer4.getOfferAutoruBuilder.getDocumentsBuilder.setLicensePlate("licensePlate1")

    val offer5 = convertCarsStrict(sale1, Some(offer4.build()))
    assert(offer5.getOfferAutoru.getLicensePlateEditCounter == 2)
    assert(offer5.getOfferAutoru.getDocuments.getLicensePlate == "Х936РА77")
  }

  test("timestampAnyUpdate cannot decrease") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val saleTimestamp = sale.anyUpdateTime.getMillis
    val offer = convertCarsStrict(sale).toBuilder
    assert(offer.getTimestampAnyUpdate == saleTimestamp)
    val offerTimestamp = saleTimestamp + 1000
    offer.setTimestampAnyUpdate(offerTimestamp)

    val offer2 = convertCarsStrict(sale, Some(offer.build()))
    assert(offer2.getTimestampAnyUpdate == offerTimestamp)

    val offerTimestamp2 = saleTimestamp - 1000
    offer.setTimestampAnyUpdate(offerTimestamp2)

    val offer3 = convertCarsStrict(sale, Some(offer.build()))
    assert(offer3.getTimestampAnyUpdate == saleTimestamp)
  }

  test("ProvenOwnerModerationState") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer = convertCarsStrict(sale).toBuilder
    ScalaCheckDrivenPropertyChecks.forAll(OfferGenerator.provenOwnerModerationStateGen) { expected =>
      offer.getOfferAutoruBuilder.setProvenOwnerModerationState(expected)
      val actual = convertCarsStrict(sale, Some(offer.build())).getOfferAutoru.getProvenOwnerModerationState
      assert(actual == expected)
    }
  }

  test("Booking") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer = convertCarsStrict(sale).toBuilder
    ScalaCheckDrivenPropertyChecks.forAll(OfferGenerator.bookingGen) { expected =>
      offer.getOfferAutoruBuilder.setBooking(expected)
      val actual = convertCarsStrict(sale, Some(offer.build())).getOfferAutoru.getBooking
      assert(actual == expected)
    }
  }

  test("do not migrate statuses when feature enabled") {
    components.featureRegistry.updateFeature(components.featuresManager.DisableStatusesMigration.name, true)
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer = convertCarsStrict(sale).toBuilder
    offer.clearFlag().addFlag(OfferFlag.OF_EXPIRED)
    val actual = convertCarsStrict(sale, Some(offer.build()))
    assert(actual.getFlagCount == 2)
    assert(actual.getFlag(0) == OfferFlag.OF_EXPIRED)
    assert(actual.getFlag(1) == OfferFlag.OF_MIGRATED)
    components.featureRegistry.updateFeature(components.featuresManager.DisableStatusesMigration.name, false)
  }

  test("migrate statuses when feature disabled") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer = convertCarsStrict(sale).toBuilder
    offer.clearFlag().addFlag(OfferFlag.OF_EXPIRED)
    val actual = convertCarsStrict(sale, Some(offer.build()))
    assert(actual.getFlagCount == 1)
    assert(actual.getFlag(0) == OfferFlag.OF_MIGRATED)
  }

  test("keep pts status defaults during migrations") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer1 = convertCarsStrict(sale).toBuilder
    offer1.getOfferAutoruBuilder.getDocumentsBuilder.clearPtsStatus().clearIsPtsOriginal()
    val offer2 = convertCarsStrict(sale, Some(offer1.build()))
    assert(!offer2.getOfferAutoru.getDocuments.hasPtsStatus)
    assert(!offer2.getOfferAutoru.getDocuments.hasIsPtsOriginal)
  }

  test("skip updating TimestampCountersStart: no vas reset") {
    val sale = components.autoruSalesDao.getOffer(1042409964L).value
    val offer1 = convertCarsStrict(sale).toBuilder
    assert(offer1.getTimestampCountersStart == 0L)
  }

  test("skip updating TimestampCountersStart: no active vas reset") {
    val resetVasStartDate = DateTime.now().withMillisOfDay(0)
    val offer1 = getOfferWithVasReset(resetVasStartDate, isActive = false)
    assert(offer1.getTimestampCountersStart == 0L)
  }

  test("skip updating TimestampCountersStart: current Offer already have active vas reset") {
    val resetVasStartDate1 = DateTime.now().withMillisOfDay(0)
    val resetVasStartDate2 = DateTime.now().withMillisOfDay(0).plusHours(1)
    val offer1 = getOfferWithVasReset(resetVasStartDate1)
    val offer2 = getOfferWithVasReset(resetVasStartDate2, currentOffer = Some(offer1))
    assert(offer1.getTimestampCountersStart == resetVasStartDate1.getMillis)
  }

  test("skip updating TimestampCountersStart on reset vas receive: offer is in new section") {
    val resetVasStartDate = DateTime.now().withMillisOfDay(0)
    val offer1 = getOfferWithVasReset(resetVasStartDate, section = Section.NEW)
    assert(offer1.getTimestampCountersStart == 0L)
  }

  test("update TimestampCountersStart on reset vas receive: no current offer") {
    val resetVasStartDate = DateTime.now().withMillisOfDay(0)
    val offer1 = getOfferWithVasReset(resetVasStartDate)
    assert(offer1.getTimestampCountersStart == resetVasStartDate.getMillis)
  }

  test("update TimestampCountersStart on reset vas receive: current offer doesn't have vas reset") {
    val resetVasStartDate1 = DateTime.now().withMillisOfDay(0)
    val resetVasStartDate2 = DateTime.now().withMillisOfDay(0).plusHours(1)
    val offer1 = getOfferWithVasReset(resetVasStartDate1, isActive = false)
    val offer2 = getOfferWithVasReset(resetVasStartDate2, currentOffer = Some(offer1))
    assert(offer2.getTimestampCountersStart == resetVasStartDate2.getMillis)
  }

  private def getOfferWithVasReset(resetVasStartDate: DateTime,
                                   isActive: Boolean = true,
                                   currentOffer: Option[Offer.Builder] = None,
                                   section: Section = Section.USED) = {
    val sale = components.autoruSalesDao
      .getOffer(1042409964L)
      .value
      .copy(
        services = Some(
          List(
            AutoruSale
              .PaidService(0L, 1042409964L, "reset", resetVasStartDate, isActivated = isActive, None, None, None)
          )
        )
      )
      .copy(sectionId = if (section == Section.USED) 1 else 2)
    convertCarsStrict(sale, currentOffer.map(_.build())).toBuilder
  }
}
