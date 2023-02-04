package ru.yandex.vertis.story.api.util

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.State.{C2bAuctionInfo, StoryPhotos}
import ru.auto.api.ApiOfferModel._
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CatalogModel.{Mark, Model}
import ru.auto.api.CommonModel.{GeoPoint, PaidService, Photo, PhotoType, PriceInfo, SmallPhotoPreview}
import ru.auto.api.CounterModel.AggregatedCounter
import ru.yandex.vertis.generators.NetGenerators.asProducer

import scala.collection.JavaConverters._

object TestOffer {

  def createTestOffer(userId: String, category: Category, description: String): Offer = {
    val now = new DateTime()
    val placement = PaidService.newBuilder
      .setService(BasicGenerators.readableString.next)
      .setCreateDate(now.getMillis)
      .setExpireDate(now.plusDays(6).getMillis)
      .setIsActive(BasicGenerators.bool.next)
      .build

    val callsCounters = AggregatedCounter.newBuilder
      .setCallsAll(BasicGenerators.choose.next)
      .setCallsDaily(BasicGenerators.choose.next)
      .build()

    val priceInfo = PriceInfo.newBuilder
      .setPrice(BasicGenerators.choose.next)
      .setCurrency("RUR")
      .setCreateTimestamp(now.getMillis)
      .build()

    val vv = PaidServicePrice.newBuilder
      .setService(BasicGenerators.readableString.next)
      .setName(BasicGenerators.readableString.next)
      .setTitle(BasicGenerators.readableString.next)
      .setDays(BasicGenerators.choose.next)
      .setPrice(BasicGenerators.choose.next)
      .setCurrency("RUR")
      .setNeedConfirm(BasicGenerators.bool.next)
      .build()

    val tags: Vector[String] = Vector(
      "stable_price",
      "allowed_for_credit",
      "vin_resolution_unknown",
      "vin_resolution_ok",
      "vin_resolution_error",
      "vin_resolution_invalid",
      "vin_resolution_in_progress",
      "vin_resolution_undefined",
      "vin_resolution_untrusted",
      "vin_service_history",
      "vin_offers_history",
      "available_for_checkup",
      "autoru_exclusive",
      "external_panoramas",
      "good_price",
      "excellent_price",
      "online_view_available",
      "proven_owner",
      "interior_panoramas",
      "has_exterior_poi",
      "gosuslugi_linked",
      "reseller_status_accepted",
      "allowed_for_safe_deal",
      "no_accidents",
      "one_owner",
      "certificate_manufacturer",
      "warranty",
      "almost_new",
      "safe_car",
      "high_reviews_mark",
      "chats_enabled",
      "has_discount_options",
      "near_to_you"
    )

    val metroStation = MetroStation.newBuilder
      .setRid(BasicGenerators.choose.next)
      .setDistance(BasicGenerators.choose.next)
      .setName(BasicGenerators.readableString.next)
      .build

    val geoPoint = GeoPoint.newBuilder
      .setLatitude(BasicGenerators.choose.next)
      .setLongitude(BasicGenerators.choose.next)
      .build

    val location = Location.newBuilder
      .setAddress(BasicGenerators.readableString.next)
      .setGeobaseId(BasicGenerators.choose.next)
      .addAllMetro(List(metroStation).asJava)
      .setCoord(geoPoint)

    location.getRegionInfoBuilder
      .setName(BasicGenerators.readableString.next)
      .build()

    val offer = Offer.newBuilder
      .setId(BasicGenerators.readableString.next)
      .setUserRef(userId)
      .setCategory(category)
      .setSection(Section.USED)
      .setCarInfo(
        CarInfo.newBuilder
          .setTransmission("MECHANICAL")
          .setMarkInfo(Mark.newBuilder().setName("mark"))
          .setModelInfo(Model.newBuilder().setName("model"))
          .build()
      )
      .setColorHex(BasicGenerators.readableString.next)
      .setIsFavorite(BasicGenerators.bool.next)
      .setDeliveryInfo(DeliveryInfo.newBuilder().build())
      .addAllServices(Seq(placement).asJava)
      .setCounters(callsCounters)
      .addAllPriceHistory(List(priceInfo).asJava)
      .addAllServicePrices(List(vv).asJava)
      .addAllTags(tags.asJava)
      .setAvailability(Availability.values().head)
      .setSellerType(SellerType.PRIVATE)
      .setStatus(OfferStatus.ACTIVE)
      .setDescription(description)

    offer.getDocumentsBuilder
      .setYear(BasicGenerators.choose.next)

    val smallPhotoPreview = SmallPhotoPreview.newBuilder().build()
    val photo = Photo
      .newBuilder()
      .setName(BasicGenerators.readableString.next)
      .setNamespace(BasicGenerators.readableString.next)
      .setPreview(smallPhotoPreview)
      .setPhotoType(PhotoType.STS_BACK)
      .putSizes(BasicGenerators.readableString.next, BasicGenerators.readableString.next)
      .build()

    val storyPhotos = StoryPhotos
      .newBuilder()
      .setStoryImage(photo)
      .setPreviewImage(photo)

    val c2bAuctionInfo = C2bAuctionInfo
      .newBuilder()
      .setStoryPhotosAuction(storyPhotos)

    offer.getStateBuilder.setStoryPhotos(storyPhotos)
    offer.getStateBuilder.setC2BAuctionInfo(c2bAuctionInfo)

    offer.build
  }
}
