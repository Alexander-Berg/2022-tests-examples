package ru.yandex.vos2.autoru.services.idx

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Inspectors, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{Category, PtsStatus, Section}
import ru.auto.api.scoring.ScoringModel.{HealthScoring, ScoringMessage}
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionEnums.ResolutionPart
import ru.auto.api.vin.VinResolutionModel.{ResolutionEntry, VinIndexResolution}
import ru.yandex.auto.message.AutoUtilsSchema.{Booking => MicrocoreBooking}
import ru.yandex.auto.message.CarAdSchema.AutoruPhotoEntryMessage
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vos2.autoru.model.AutoruModelUtils._
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.MotoInfo.MotoCategory
import ru.yandex.vos2.AutoruModel.AutoruOffer.TruckInfo.TruckCategory
import ru.yandex.vos2.AutoruModel.AutoruOffer.YandexVideo.YandexVideoStatus
import ru.yandex.vos2.AutoruModel.AutoruOffer.YoutubeVideo.YoutubeVideoStatus
import ru.yandex.vos2.AutoruModel.AutoruOffer._
import ru.yandex.vos2.BasicsModel
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Multiposting, Offer, OfferFlag, ScoringMessageWithHistory}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.model.delivery.DeliveryInfoMessageMapper
import ru.yandex.vos2.autoru.model.extdata.SearchTagWhiteList
import ru.yandex.vos2.model.CommonGen.{booleanGen, CompositeStatusGen}
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.model.offer.OfferGenerator
import ru.yandex.vos2.proto.ProtoMacro._

import scala.jdk.CollectionConverters._

/**
  * Created by andrey on 8/31/16.
  */
@RunWith(classOf[JUnitRunner])
class AutoruIdxRequestBuilderTest extends AnyFunSuite with OptionValues with InitTestDbs with Inspectors {

  val idxRequestBuilder: AutoruIdxRequestBuilder = components.idxRequestBuilder
  val trucksRequestBuilder: TrucksIdxRequestBuilder = components.trucksIdxRequestBuilder
  val motoRequestBuilder: MotoIdxRequestBuilder = components.motoIdxRequestBuilder

  val feature: Feature[Boolean] = components.featuresManager.PushBrandCert

  test("ProtoMacro.opt test") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getDocumentsBuilder.setStsCode("sts")
    val offer = builder.build()
    assert(?(offer.getOfferAutoru.getDocuments).nonEmpty)
    assert(?(offer.getOfferAutoru.getDocuments.getStsCode).nonEmpty)
    assert(?(offer.getOfferAutoru.getDocuments.getVin).isEmpty)
  }

  test("mds format") {
    val builder = TestUtils.createOffer()
    val name = "fffaaa"
    val resName = name

    builder.getOfferAutoruBuilder
      .addPhotoBuilder()
      .setCreated(System.currentTimeMillis())
      .setCvHash("hash1")
      .setIsMain(true)
      .setOrder(1)
      .setName(s"123-$name")

    val offer = builder.build()

    val message = idxRequestBuilder.build(offer)

    message.getAutoruPhotosCount shouldBe 1
    val photo = message.getAutoruPhotos(0)
    photo.getCvHash shouldBe "hash1"
    photo.getOrder shouldBe 1

    // Урлы должны быть всегда без схемы VERTISADMIN-12915
    forEvery(photo.getAliasesValuesList) { url =>
      (url should fullyMatch).regex(s"^//[^/]+/get-autoru-all/123/$resName/.+$$")
    }
  }

  test("photo in non mds format") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder
      .addPhotoBuilder()
      .setCreated(System.currentTimeMillis())
      .setCvHash("")
      .setIsMain(true)
      .setOrder(1)
      .setName("123fff")
    val offer = builder.build()

    val message = idxRequestBuilder.build(offer)

    message.getAutoruPhotosCount shouldBe 0
  }

  test("fill need_replacement_phone") {
    forEvery(Seq(true, false)) { value =>
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.setRedirectPhones(value)
      val message = idxRequestBuilder.build(builder.build())

      message.hasNeedReplacementPhone shouldBe true
      message.getNeedReplacementPhone shouldBe value
    }
  }

  test("fill no_disturb") {
    forEvery(Seq(true, false)) { value =>
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.getSellerBuilder.setDoNotDisturb(value)
      val message = idxRequestBuilder.build(builder.build())

      message.hasNoDisturb shouldBe true
      message.getNoDisturb shouldBe value
    }
  }

  test("has brand certification in user offer") {
    components.featureRegistry.updateFeature(feature.name, true)

    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder
      .setBrandCertInfo(TestUtils.createBrandCertInfo(isActive = true))
    val offer = builder.build()

    val message = idxRequestBuilder.build(offer)

    assert(message.hasAutoruCertificate)

    components.featureRegistry.updateFeature(feature.name, false)
  }

  test("has brand certification, and dealer is official") {
    components.featureRegistry.updateFeature(feature.name, true)

    val builder = TestUtils.createOffer(dealer = true)
    builder.getOfferAutoruBuilder
      .setBrandCertInfo(TestUtils.createBrandCertInfo(isActive = true))
    builder.getOfferAutoruBuilder.getSalonBuilder
      .setSalonId("123")
      .setTitle("JLR")
      .setIsOfficial(true)
    val offer = builder.build()

    offer.getOfferAutoru.getSalon

    val message = idxRequestBuilder.build(offer)

    assert(message.getAutoruCertificate.getId == "")
    assert(message.getAutoruCertificate.getVersion == 1)
    assert(message.getAutoruCertificate.getType == "JaguarApproved")

    components.featureRegistry.updateFeature(feature.name, false)
  }

  test("has brand certification, and dealer is not official") {
    components.featureRegistry.updateFeature(feature.name, true)

    val builder = TestUtils.createOffer(dealer = true)
    builder.getOfferAutoruBuilder
      .setBrandCertInfo(TestUtils.createBrandCertInfo(isActive = true))
    val offer = builder.build()

    offer.getOfferAutoru.getSalon

    val message = idxRequestBuilder.build(offer)

    assert(message.hasAutoruCertificate)

    components.featureRegistry.updateFeature(feature.name, false)
  }

  test("has inactive brand certification") {
    components.featureRegistry.updateFeature(feature.name, true)

    val builder = TestUtils.createOffer(dealer = true)
    builder.getOfferAutoruBuilder
      .setBrandCertInfo(TestUtils.createBrandCertInfo(isActive = false))
    val offer = builder.build()

    val message = idxRequestBuilder.build(offer)

    assert(!message.hasAutoruCertificate)

    components.featureRegistry.updateFeature(feature.name, false)
  }

  test("do not send haggle and exchange for new trucks") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder
      .setCategory(Category.TRUCKS)
      .setHagglePossible(true)
      .setExchangeStatus(Exchange.POSSIBLE)
      .setSection(Section.USED)
      .setTruckInfo(TruckInfo.newBuilder().setAutoCategory(TruckCategory.TRUCK_CAT_BUS))
    val offer1 = builder.build()
    val idxRequest1 = trucksRequestBuilder.build(offer1)
    assert(idxRequest1.hasHaggleKey)
    assert(idxRequest1.getHaggleKey == "POSSIBLE")
    assert(idxRequest1.getChangeKey == "CONSIDER_OPTIONS")

    builder.getOfferAutoruBuilder.setSection(Section.NEW)
    val offer2 = builder.build()
    val idxRequest2 = trucksRequestBuilder.build(offer2)
    assert(!idxRequest2.hasHaggleKey)
    assert(!idxRequest2.hasChangeKey)
  }

  test("handle main flag in photos for comtrans") {
    // для мото и комтранса при отправке фото в индексер пересортировываем их так, чтобы первым шло фото со свойствам
    // main = true, а в остальном согласно order
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder
      .addPhotoBuilder()
      .setName("group1-photo1")
      .setOrigName("group1-photo1")
      .setOrder(2)
      .setCreated(1L)
      .setIsMain(false)
    builder.getOfferAutoruBuilder
      .addPhotoBuilder()
      .setName("group1-photo2")
      .setOrigName("group1-photo2")
      .setOrder(1)
      .setCreated(1L)
      .setIsMain(false)
    builder.getOfferAutoruBuilder
      .addPhotoBuilder()
      .setName("group1-photo3")
      .setOrigName("group1-photo3")
      .setOrder(3)
      .setCreated(1L)
      .setIsMain(true)
    builder.getOfferAutoruBuilder
      .setCategory(Category.TRUCKS)
      .setHagglePossible(true)
      .setExchangeStatus(Exchange.POSSIBLE)
      .setSection(Section.USED)
      .setTruckInfo(TruckInfo.newBuilder().setAutoCategory(TruckCategory.TRUCK_CAT_BUS))
    val offer = builder.build()
    val idxRequest = trucksRequestBuilder.build(offer)
    assert(idxRequest.getAutoruPhotos(0).getAliasesValues(0).contains("photo3"))
    assert(idxRequest.getAutoruPhotos(1).getAliasesValues(0).contains("photo2"))
    assert(idxRequest.getAutoruPhotos(2).getAliasesValues(0).contains("photo1"))
  }

  test("comtrans: isilon image (no group) will not be sent to indexer") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.addPhotoBuilder().setName("photo1").setOrder(2).setCreated(1L).setIsMain(false)
    builder.getOfferAutoruBuilder
      .setCategory(Category.TRUCKS)
      .setHagglePossible(true)
      .setExchangeStatus(Exchange.POSSIBLE)
      .setSection(Section.USED)
      .setTruckInfo(TruckInfo.newBuilder().setAutoCategory(TruckCategory.TRUCK_CAT_BUS))
    val offer = builder.build()
    val idxRequest = trucksRequestBuilder.build(offer)
    assert(idxRequest.getAutoruPhotosCount == 0)
  }

  test("autoservices") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder
      .addAutoserviceReviewBuilder()
      .setAutoserviceId("asid_123")
      .setReviewId("r_123")

    val offer = builder.build()
    val idxRequest = idxRequestBuilder.build(offer)
    assert(idxRequest.getAutoserviceReviewCount == 1)
    assert(idxRequest.getAutoserviceReview(0).getAutoserviceId == "asid_123")
    assert(idxRequest.getAutoserviceReview(0).getReviewId == "r_123")

  }

  test("isBan") {
    val builder = TestUtils.createOffer()
    builder.putFlag(OfferFlag.OF_BANNED)
    val idxRequest = idxRequestBuilder.build(builder.build())
    assert(idxRequest.getIsBan)
    builder.getOfferAutoruBuilder.setCategory(Category.TRUCKS)
    builder.getOfferAutoruBuilder.getTruckInfoBuilder.setAxis(6)
    val trucksIdxRequest = trucksRequestBuilder.build(builder.build())
    assert(trucksIdxRequest.getIsBan)
    builder.getOfferAutoruBuilder.setCategory(Category.MOTO)
    builder.getOfferAutoruBuilder.getMotoInfoBuilder.setEnginePower(100)
    val motoIdxRequest = motoRequestBuilder.build(builder.build())
    assert(motoIdxRequest.getIsBan)
  }

  test("phoneInfo") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getSellerBuilder
      .addPhoneBuilder()
      .setNumber("79344358787")
      .setCallHourStart(12)
      .setCallHourEnd(20)
      .setCallMinuteStart(0)
      .setCallMinuteEnd(0)
    val idxRequest = idxRequestBuilder.build(builder.build())
    assert(!idxRequest.getPhoneInfoList.isEmpty)
    builder.getOfferAutoruBuilder.setCategory(Category.TRUCKS)
    builder.getOfferAutoruBuilder.getTruckInfoBuilder.setAxis(6)
    val trucksIdxRequest = trucksRequestBuilder.build(builder.build())
    assert(!trucksIdxRequest.getPhoneInfoList.isEmpty)
    builder.getOfferAutoruBuilder.setCategory(Category.MOTO)
    builder.getOfferAutoruBuilder.getMotoInfoBuilder.setEnginePower(100)
    val motoIdxRequest = motoRequestBuilder.build(builder.build())
    assert(!motoIdxRequest.getPhoneInfoList.isEmpty)
  }

  test("priceHistory") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder
      .addPriceHistoryBuilder()
      .setPrice(750000.0)
      .setCurrency(BasicsModel.Currency.RUB)
      .setCreated(1504258461)
    val idxRequest = idxRequestBuilder.build(builder.build())
    assert(!idxRequest.getPriceHistoryList.isEmpty)
    builder.getOfferAutoruBuilder.setCategory(Category.TRUCKS)
    builder.getOfferAutoruBuilder.getTruckInfoBuilder.setAxis(6)
    val trucksIdxRequest = trucksRequestBuilder.build(builder.build())
    assert(!trucksIdxRequest.getPriceHistoryList.isEmpty)
    builder.getOfferAutoruBuilder.setCategory(Category.MOTO)
    builder.getOfferAutoruBuilder.getMotoInfoBuilder.setEnginePower(100)
    val motoIdxRequest = motoRequestBuilder.build(builder.build())
    assert(!motoIdxRequest.getPriceHistoryList.isEmpty)
  }

  test("autoruClientId in comtrans") {
    val builder = TestUtils.createOffer(dealer = true)
    builder.getUserBuilder.getAlternativeIdsBuilder.setExternal(145)
    builder.getOfferAutoruBuilder
      .setCategory(Category.TRUCKS)
      .setHagglePossible(true)
      .setExchangeStatus(Exchange.POSSIBLE)
      .setSection(Section.USED)
      .setTruckInfo(TruckInfo.newBuilder().setAutoCategory(TruckCategory.TRUCK_CAT_BUS))
    val idxRequest = trucksRequestBuilder.build(builder.build())
    assert(idxRequest.getUserId == "145")
    assert(idxRequest.getAutoruClientId == "321")
  }

  test("autoruClientId in moto") {
    val builder = TestUtils.createOffer(dealer = true)
    builder.getUserBuilder.getAlternativeIdsBuilder.setExternal(145)
    builder.getOfferAutoruBuilder
      .setCategory(Category.MOTO)
      .setHagglePossible(true)
      .setExchangeStatus(Exchange.POSSIBLE)
      .setSection(Section.USED)
      .setMotoInfo(MotoInfo.newBuilder().setCategory(MotoCategory.MOTO_CAT_ATV))
    val idxRequest = motoRequestBuilder.build(builder.build())
    assert(idxRequest.getUserId == "145")
    assert(idxRequest.getAutoruClientId == "321")
  }

  test("deliveryInfo in comtrans") {
    val deliveryInfo = AutoruOffer.DeliveryInfo
      .newBuilder()
      .addDeliveryRegions(
        AutoruOffer.DeliveryRegion
          .newBuilder()
          .setLocation(
            AutoruOffer.Location
              .newBuilder()
              .setAddress("asd")
              .setGeobaseId(123)
              .setCoordinates(
                BasicsModel.GeoPoint
                  .newBuilder()
                  .setLongitude(1)
                  .setLatitude(2)
              )
          )
      )
      .build()

    val builder = TestUtils.createOffer(dealer = true)
    builder.getUserBuilder.getAlternativeIdsBuilder.setExternal(145)
    builder.getOfferAutoruBuilder
    builder.getOfferAutoruBuilder
      .setCategory(Category.TRUCKS)
      .setHagglePossible(true)
      .setExchangeStatus(Exchange.POSSIBLE)
      .setSection(Section.USED)
      .setTruckInfo(TruckInfo.newBuilder().setAutoCategory(TruckCategory.TRUCK_CAT_BUS))
      .setDeliveryInfo(deliveryInfo)
    val idxRequest = trucksRequestBuilder.build(builder.build())
    assert(idxRequest.getDeliveryInfo == DeliveryInfoMessageMapper.toDeliveryInfoMessage(deliveryInfo))
  }

  test("deliveryInfo in moto") {
    val deliveryInfo = AutoruOffer.DeliveryInfo
      .newBuilder()
      .addDeliveryRegions(
        AutoruOffer.DeliveryRegion
          .newBuilder()
          .setLocation(
            AutoruOffer.Location
              .newBuilder()
              .setAddress("asd")
              .setGeobaseId(123)
              .setCoordinates(
                BasicsModel.GeoPoint
                  .newBuilder()
                  .setLongitude(1)
                  .setLatitude(2)
              )
          )
      )
      .build()

    val builder = TestUtils.createOffer(dealer = true)
    builder.getUserBuilder.getAlternativeIdsBuilder.setExternal(145)
    builder.getOfferAutoruBuilder
      .setCategory(Category.MOTO)
      .setHagglePossible(true)
      .setExchangeStatus(Exchange.POSSIBLE)
      .setSection(Section.USED)
      .setMotoInfo(MotoInfo.newBuilder().setCategory(MotoCategory.MOTO_CAT_ATV))
      .setDeliveryInfo(deliveryInfo)
    val idxRequest = motoRequestBuilder.build(builder.build())
    assert(idxRequest.getDeliveryInfo == DeliveryInfoMessageMapper.toDeliveryInfoMessage(deliveryInfo))
  }

  test("deliveryInfo in cars") {
    val deliveryInfo = AutoruOffer.DeliveryInfo
      .newBuilder()
      .addDeliveryRegions(
        AutoruOffer.DeliveryRegion
          .newBuilder()
          .setLocation(
            AutoruOffer.Location
              .newBuilder()
              .setAddress("asd")
              .setGeobaseId(123)
              .setCoordinates(
                BasicsModel.GeoPoint
                  .newBuilder()
                  .setLongitude(1)
                  .setLatitude(2)
              )
          )
      )
      .build()

    val builder = TestUtils.createOffer(dealer = true)
    builder.getUserBuilder.getAlternativeIdsBuilder.setExternal(145)
    builder.getOfferAutoruBuilder.setDeliveryInfo(deliveryInfo)
    val idxRequest = idxRequestBuilder.build(builder.build())
    assert(idxRequest.getDeliveryInfo == DeliveryInfoMessageMapper.toDeliveryInfoMessage(deliveryInfo))
  }

  test("convert vin resolution") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(1)
      .setResolution(
        VinIndexResolution
          .newBuilder()
          .setVersion(1)
          .addEntries(
            ResolutionEntry
              .newBuilder()
              .setPart(ResolutionPart.SUMMARY)
              .setStatus(VinResolutionEnums.Status.OK)
          )
          .build()
      )
    val idxRequest = idxRequestBuilder.build(builder.build())
    assert(idxRequest.getVinDecoderResolution.getEntriesCount > 0)
  }

  test("convert source info") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getSourceInfoBuilder
      .setPlatform(SourceInfo.Platform.DESKTOP)
      .setSource(SourceInfo.Source.DROM)
      .setIsCallcenter(true)
    val idxRequest = idxRequestBuilder.build(builder.build())
    assert(idxRequest.getSourceInfo.getPlatform == "desktop")
    assert(idxRequest.getSourceInfo.getSource == "drom")
    assert(idxRequest.getSourceInfo.getIsCallcenter)
  }

  test("drop tags which are not allowed") {
    val builder = TestUtils.createOffer()
    builder.addTag("tag1")
    builder.addTag("tag2")
    builder.addTag("tag3")

    val sTWL = new SearchTagWhiteList(Set("tag2", "tag4"))

    val localIdx =
      new AutoruIdxRequestBuilder(components.featuresManager, components.mdsPhotoUtils, sTWL, components.regionTree)

    val res = localIdx.build(builder.build())
    val tags = res.getSearchTagsList
    tags.size() shouldBe 1
    tags.get(0) shouldBe "tag2"
  }

  test("convert original price") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getOriginalPriceBuilder
      .setPrice(100.05)
      .setCreated(System.currentTimeMillis())
      .setCurrency(BasicsModel.Currency.RUB)
    val idxRequest = idxRequestBuilder.build(builder.build())
    assert(idxRequest.getOriginalPrice == 100)
  }

  test("use RUR in c2b auction info") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getC2BAuctionInfoBuilder.getPriceRangeBuilder.setCurrency(BasicsModel.Currency.RUB)
    val idxRequest = idxRequestBuilder.build(builder.build())

    assert(idxRequest.getPriceRange.getCurrency == "RUR")
  }

  for {
    category <- Seq(Category.CARS, Category.TRUCKS, Category.MOTO)
  } test(s"set seller from user.profile.alias if allowOffersShow = true, category=$category") {
    val builder = TestUtils.createOffer(category = category)
    builder.getUserBuilder.getProfileBuilder.setAlias("alias")
    builder.getOfferAutoruBuilder.getSellerBuilder.setUserName("userName")
    builder.getUserBuilder.getProfileBuilder.setAllowOffersShow(true)

    category match {
      case Category.CARS =>
        assert(idxRequestBuilder.build(builder.build()).getSeller == "alias")
      case Category.TRUCKS =>
        assert(trucksRequestBuilder.build(builder.build()).getUserName == "alias")
      case Category.MOTO =>
        assert(motoRequestBuilder.build(builder.build()).getUserName == "alias")
    }

    builder.getUserBuilder.getProfileBuilder.setAllowOffersShow(false)

    category match {
      case Category.CARS =>
        assert(idxRequestBuilder.build(builder.build()).getSeller == "userName")
      case Category.TRUCKS =>
        assert(trucksRequestBuilder.build(builder.build()).getUserName == "userName")
      case Category.MOTO =>
        assert(motoRequestBuilder.build(builder.build()).getUserName == "userName")
    }
  }

  trait VideoOfferFixture {

    def builder(cat: ApiOfferModel.Category): Offer.Builder =
      TestUtils.createOffer(category = cat)

    def video: Video

    def videoAlias: String

    def videoId: String

    def offerAutoru(cat: ApiOfferModel.Category): AutoruOffer =
      builder(cat).getOfferAutoruBuilder
        .setMotoInfo(MotoInfo.newBuilder())
        .addVideo(video)
        .build()

    def assertVideo(photo: AutoruPhotoEntryMessage): Unit = {
      photo.getAliasesKeys(0) shouldBe "video"
      photo.getAliasesValues(0) shouldBe "%s:%s".format(videoAlias, videoId)
      photo.getOrder shouldBe 0
    }
  }

  test("should pass yandex video links along") {
    new VideoOfferFixture {
      val videoId = "ya ident"
      val videoAlias = "yandex"
      val video = Video
        .newBuilder()
        .setYandexVideo(
          AutoruOffer.YandexVideo
            .newBuilder()
            .setVideoIdent(videoId)
            .setStatus(YandexVideoStatus.AVAILABLE)
            .build()
        )
        .setUpdated(System.currentTimeMillis())
        .setCreated(System.currentTimeMillis())
        .build()

      val message = idxRequestBuilder.build(
        builder(cat = Category.CARS)
          .setOfferAutoru(offerAutoru(Category.CARS))
          .build()
      )
      message.getAutoruPhotosCount shouldBe 1
      assertVideo(message.getAutoruPhotos(0))

      val messageMoto = motoRequestBuilder.build(
        builder(cat = Category.MOTO)
          .setOfferAutoru(offerAutoru(Category.MOTO))
          .build()
      )
      messageMoto.getAutoruPhotosCount shouldBe 1
      assertVideo(messageMoto.getAutoruPhotos(0))

      val messageTrucks = trucksRequestBuilder.build(
        builder(cat = Category.TRUCKS)
          .setOfferAutoru(offerAutoru(Category.TRUCKS))
          .build()
      )
      messageTrucks.getAutoruPhotosCount shouldBe 1
      assertVideo(messageTrucks.getAutoruPhotos(0))
    }
  }

  test("should pass youtube video links along") {
    new VideoOfferFixture {
      val videoId = "youtube id"
      val videoAlias = "youtube"
      val video = Video
        .newBuilder()
        .setYoutubeVideo(
          AutoruOffer.YoutubeVideo
            .newBuilder()
            .setYoutubeId(videoId)
            .setStatus(YoutubeVideoStatus.AVAILABLE)
            .build()
        )
        .setUpdated(System.currentTimeMillis())
        .setCreated(System.currentTimeMillis())
        .build()

      val message = idxRequestBuilder.build(
        builder(cat = Category.CARS)
          .setOfferAutoru(offerAutoru(Category.CARS))
          .build()
      )
      message.getAutoruPhotosCount shouldBe 1
      assertVideo(message.getAutoruPhotos(0))

      val messageMoto = motoRequestBuilder.build(
        builder(cat = Category.MOTO)
          .setOfferAutoru(offerAutoru(Category.MOTO))
          .build()
      )
      messageMoto.getAutoruPhotosCount shouldBe 1
      assertVideo(messageMoto.getAutoruPhotos(0))

      val messageTrucks = trucksRequestBuilder.build(
        builder(cat = Category.TRUCKS)
          .setOfferAutoru(offerAutoru(Category.TRUCKS))
          .build()
      )
      messageTrucks.getAutoruPhotosCount shouldBe 1
      assertVideo(messageTrucks.getAutoruPhotos(0))
    }
  }

  private def checkBooking(booking: Booking, actual: MicrocoreBooking): Unit = {
    assert(actual.getAllowed == booking.getAllowed)

    ?(actual.getState).foreach { state =>
      assert(state.getUpdated == booking.getState.getUpdated)
      assert(state.hasBooked == booking.getState.hasBooked)
      assert(state.hasNotBooked == booking.getState.hasNotBooked)

      ?(state.getBooked).foreach { booked =>
        assert(?(booked.getBookingId) == ?(booking.getState.getBooked.getBookingId))
        assert(?(booked.getPeriod.getFrom) == ?(booking.getState.getBooked.getPeriod.getFrom))
        assert(?(booked.getPeriod.getTo) == ?(booking.getState.getBooked.getPeriod.getTo))
        assert(?(booked.getUserRef) == ?(booking.getState.getBooked.getUserRef))
      }
    }
  }

  test("booking cars") {
    ScalaCheckDrivenPropertyChecks.forAll(OfferGenerator.bookingGen) { booking =>
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder.setBooking(booking)
      val offer = builder.build
      val idxRequest = idxRequestBuilder.build(offer)
      val actual = idxRequest.getBooking
      checkBooking(offer.getOfferAutoru.getBooking, actual)
    }
  }

  test("scoring without health shouldbe empty") {
    val builder = TestUtils
      .createOffer()
      .setScoring(
        ScoringMessageWithHistory
          .newBuilder()
          .setCurrentScoring(ScoringMessage.newBuilder().setHealthScoring(HealthScoring.newBuilder()))
      )
    val idxRequest = idxRequestBuilder.build(builder.build())
    idxRequest.getScore.hasHealth shouldBe false
  }

  test("booking moto") {
    ScalaCheckDrivenPropertyChecks.forAll(OfferGenerator.bookingGen) { booking =>
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder
        .setCategory(Category.MOTO)
        .setMotoInfo(MotoInfo.newBuilder().setCategory(MotoCategory.MOTO_CAT_ATV))
        .setBooking(booking)
      val offer = builder.build
      val idxRequest = motoRequestBuilder.build(offer)
      val actual = idxRequest.getBooking
      checkBooking(offer.getOfferAutoru.getBooking, actual)
    }
  }

  test("booking trucks") {
    ScalaCheckDrivenPropertyChecks.forAll(OfferGenerator.bookingGen) { booking =>
      val builder = TestUtils.createOffer()
      builder.getOfferAutoruBuilder
        .setCategory(Category.TRUCKS)
        .setTruckInfo(TruckInfo.newBuilder().setAutoCategory(TruckCategory.TRUCK_CAT_BUS))
        .setBooking(booking)
      val offer = builder.build
      val idxRequest = trucksRequestBuilder.build(offer)
      val actual = idxRequest.getBooking
      checkBooking(offer.getOfferAutoru.getBooking, actual)
    }
  }

  test("is_revoked flag with dealer multiposting offer") {
    val b = TestUtils
      .createOffer(dealer = true, withMultiposting = true)
      .addFlag(OfferFlag.OF_INACTIVE)

    b.getOfferAutoruBuilder
      .setRecallInfo {
        RecallInfo
          .newBuilder()
          .setSoldPrice(10000000L)
          .setRecallTimestamp(1L)
      }

    val idxRequest = idxRequestBuilder.build(b.build())
    assert(!idxRequest.getIsRevoked)
  }

  test("is_revoked flag with non-active dealer multiposting offer") {
    val b = TestUtils
      .createOffer(dealer = true, withMultiposting = true)
      .addFlag(OfferFlag.OF_INACTIVE)

    b.getMultipostingBuilder
      .setStatus {
        CompositeStatus.CS_INACTIVE
      }

    b.getOfferAutoruBuilder
      .setRecallInfo {
        RecallInfo
          .newBuilder()
          .setSoldPrice(10000000L)
          .setRecallTimestamp(1L)
      }

    val idxRequest = idxRequestBuilder.build(b.build())
    assert(idxRequest.getIsRevoked)
  }

  test("multiposting cars") {
    ScalaCheckDrivenPropertyChecks.forAll(multipostingOfferGen(Category.CARS)) { offer =>
      val idxRequest = idxRequestBuilder.build(offer)

      assert(
        idxRequest.getDealerShowcase ==
          (offer.isActive ||
            (offer.getAutoruCompositeStatus != CompositeStatus.CS_BANNED &&
              ?(offer.getMultiposting).exists(_.getStatus == CompositeStatus.CS_ACTIVE)))
      )
      assert(idxRequest.getDealerHidden == (offer.getAutoruCompositeStatus != CompositeStatus.CS_ACTIVE))
      assert(
        ?(idxRequest.getMultipostingStatus) == ?(offer.getMultiposting)
          .map(_.getStatus)
          .map(compositeStatusToOfferStatus)
      )
    }
  }

  test("multiposting trucks") {
    ScalaCheckDrivenPropertyChecks.forAll(multipostingOfferGen(Category.TRUCKS)) { offer =>
      val idxRequest = idxRequestBuilder.build(offer)

      assert(
        idxRequest.getDealerShowcase ==
          (offer.isActive ||
            (offer.getAutoruCompositeStatus != CompositeStatus.CS_BANNED &&
              ?(offer.getMultiposting).exists(_.getStatus == CompositeStatus.CS_ACTIVE)))
      )
      assert(idxRequest.getDealerHidden == (offer.getAutoruCompositeStatus != CompositeStatus.CS_ACTIVE))
      assert(
        ?(idxRequest.getMultipostingStatus) == ?(offer.getMultiposting)
          .map(_.getStatus)
          .map(compositeStatusToOfferStatus)
      )
    }
  }

  test("multiposting moto") {
    ScalaCheckDrivenPropertyChecks.forAll(multipostingOfferGen(Category.MOTO)) { offer =>
      val idxRequest = idxRequestBuilder.build(offer)

      assert(
        idxRequest.getDealerShowcase ==
          (offer.isActive ||
            (offer.getAutoruCompositeStatus != CompositeStatus.CS_BANNED &&
              ?(offer.getMultiposting).exists(_.getStatus == CompositeStatus.CS_ACTIVE)))
      )
      assert(idxRequest.getDealerHidden == (offer.getAutoruCompositeStatus != CompositeStatus.CS_ACTIVE))
      assert(
        ?(idxRequest.getMultipostingStatus) == ?(offer.getMultiposting)
          .map(_.getStatus)
          .map(compositeStatusToOfferStatus)
      )
    }
  }

  test("check chatsEnabled") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getSellerBuilder.setAllowChatsCreation(true)
    val offer = builder.build()
    val message = idxRequestBuilder.build(offer)
    assert(message.getChatEnabled)

    builder.getOfferAutoruBuilder.getSellerBuilder.setAllowChatsCreation(false)
    val offer2 = builder.build()
    val message2 = idxRequestBuilder.build(offer2)
    assert(!message2.getChatEnabled)
  }

  test("check phoneCallsCounter") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.getPhoneCallsCounterBuilder
      .setTotal(6)
      .putDaily(1642453200000L, 2)
      .putDaily(1642539600000L, 1)
      .putDaily(1642626000000L, 1)
      .putDaily(1642712400000L, 2)
    val offer = builder.build()
    val message = idxRequestBuilder.build(offer)
    assert(message.hasPhoneCallsCounter)
    assert(message.getPhoneCallsCounter.getTotal == 6)
    assert(message.getPhoneCallsCounter.getDailyOrDefault(1642453200000L, 0) == 2)
    assert(message.getPhoneCallsCounter.getDailyOrDefault(1642539600000L, 0) == 1)
    assert(message.getPhoneCallsCounter.getDailyOrDefault(1642626000000L, 0) == 1)
    assert(message.getPhoneCallsCounter.getDailyOrDefault(1642453200000L, 0) == 2)
  }

  test("check trustedDealerCallsAccepted") {
    val builder = TestUtils.createOffer()
    builder.getOfferAutoruBuilder.setTrustedDealerCallsAccepted(true)
    val offer = builder.build()
    val message = idxRequestBuilder.build(offer)
    assert(message.getTrustedDealerCallsAccepted())

    builder.getOfferAutoruBuilder.setTrustedDealerCallsAccepted(false)
    val offer2 = builder.build()
    val message2 = idxRequestBuilder.build(offer2)
    assert(!message2.getTrustedDealerCallsAccepted())
  }

  test("check ptsStatus, AllowNoPts disabled") {
    components.featureRegistry.updateFeature(components.featuresManager.AllowNoPts.name, false)
    val builder1 = TestUtils.createOffer()
    builder1.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.ORIGINAL)
    val idxRequest1 = idxRequestBuilder.build(builder1.build())
    assert(idxRequest1.getIsPtsOriginal == "1")

    val builder2 = TestUtils.createOffer()
    builder2.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.DUPLICATE)
    val idxRequest2 = idxRequestBuilder.build(builder2.build())
    assert(idxRequest2.getIsPtsOriginal == "2")

    val builder3 = TestUtils.createOffer()
    builder3.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.NO_PTS)
    val idxRequest3 = idxRequestBuilder.build(builder3.build())
    assert(!idxRequest3.hasIsPtsOriginal)
  }

  test("check ptsStatus, AllowNoPts enabled") {
    components.featureRegistry.updateFeature(components.featuresManager.AllowNoPts.name, true)
    val builder1 = TestUtils.createOffer()
    builder1.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.ORIGINAL)
    val idxRequest1 = idxRequestBuilder.build(builder1.build())
    assert(idxRequest1.getIsPtsOriginal == "1")

    val builder2 = TestUtils.createOffer()
    builder2.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.DUPLICATE)
    val idxRequest2 = idxRequestBuilder.build(builder2.build())
    assert(idxRequest2.getIsPtsOriginal == "2")

    val builder3 = TestUtils.createOffer()
    builder3.getOfferAutoruBuilder.getDocumentsBuilder.setPtsStatus(PtsStatus.NO_PTS)
    val idxRequest3 = idxRequestBuilder.build(builder3.build())
    assert(idxRequest3.getIsPtsOriginal == "3")
    components.featureRegistry.updateFeature(components.featuresManager.AllowNoPts.name, false)
  }

  private def multipostingOfferGen(category: Category): Gen[Offer] =
    for {
      flags <- Gen.oneOf(Gen.const(Seq.empty), Gen.oneOf(OfferFlag.values()).map(Seq(_)))
      multipostingEnabled <- booleanGen
      multipostingStatus <- CompositeStatusGen
    } yield {
      val b = TestUtils.createOffer(dealer = true, category = category).addAllFlag(flags.asJava)

      if (multipostingEnabled) {
        b.setMultiposting(Multiposting.newBuilder().setStatus(multipostingStatus))
      }

      b.build()
    }
}
