package ru.yandex.realty.feeds

import com.google.protobuf.BoolValue
import org.scalacheck.Gen
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.{
  ApartmentInfo,
  AreaInfo,
  AreaPrice,
  AreaUnit,
  CategoryType,
  FlatType,
  HouseInfo,
  HouseType,
  Money,
  Offer,
  OfferType,
  PriceInfo,
  PricingPeriod,
  SaleAgent,
  Transaction
}
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.proto.unified.offer.address.{Metro, TransportDistance}
import ru.yandex.realty.proto.unified.offer.images.{MdsImageId, RealtyPhotoInfo, UnifiedImages}

import scala.collection.JavaConverters._

object OfferBuilder {

  private val ignoreCategories = Set(CategoryType.UNKNOWN, CategoryType.UNUSED)

  def offerGen: Gen[Offer] =
    for {
      offerType <- Gen.oneOf(OfferType.RENT, OfferType.RENT)
      regionId <- Gen.oneOf(
        Regions.MOSCOW,
        Regions.RUSSIA,
        Regions.SPB,
        Regions.SPB_AND_LEN_OBLAST,
        Regions.MSK_AND_MOS_OBLAST
      )
      category <- Gen.oneOf(CategoryType.values().filterNot(category => ignoreCategories(category)))
      houseType <- Gen.oneOf(HouseType.values().filterNot(_ == HouseType.UNKNOWN))
    } yield build(regionId, offerType, category, houseType)

  private def metro(minutesOnFoot: Option[Int], minutesOnPublicTransport: Option[Int]): Metro = {
    val metro = Metro.newBuilder()
    minutesOnFoot.foreach { minutes =>
      metro.addDistances(
        TransportDistance
          .newBuilder()
          .setTransportType(TransportDistance.TransportType.ON_FOOT)
          .setTime(com.google.protobuf.Duration.newBuilder().setSeconds(minutes * 60))
      )
    }
    minutesOnPublicTransport.foreach { minutes =>
      metro.addDistances(
        TransportDistance
          .newBuilder()
          .setTransportType(TransportDistance.TransportType.PUBLIC_TRANSPORT)
          .setTime(com.google.protobuf.Duration.newBuilder().setSeconds(minutes * 60))
      )
    }
    metro.build()
  }

  def build(
    geoCode: Int = 213,
    offerType: OfferType = OfferType.SELL,
    categoryType: CategoryType = CategoryType.APARTMENT,
    houseType: HouseType = HouseType.DUPLEX,
    apartmentFlatType: FlatType = FlatType.NEW_FLAT,
    metroMinutesOnFoot: Option[Int] = None,
    metroMinutesOnPublicTransport: Option[Int] = None,
    id: Long = 0L
  ): Offer = {
    val offer = new Offer
    offer.setId(id)
    offer.setOfferType(offerType)
    offer.setCategoryType(categoryType)
    if (categoryType == CategoryType.APARTMENT) {
      val apInfo = new ApartmentInfo()
      apInfo.setFlatType(apartmentFlatType)
      offer.setApartmentInfo(apInfo)
    }
    val location = new Location
    location.setMetro(Seq(metro(metroMinutesOnFoot, metroMinutesOnPublicTransport)).asJava)
    location.setGeocoderId(geoCode)
    offer.setLocation(location)
    val hi = new HouseInfo
    hi.setHouseType(houseType)
    offer.setHouseInfo(hi)
    val money = Money.of(Currency.RUR, 1000000)
    val priceInfo = PriceInfo.create(money, PricingPeriod.PER_MONTH, AreaUnit.SQUARE_METER)
    val areaInfo = AreaInfo.create(AreaUnit.SQUARE_METER, 100f)
    val areaPrice = new AreaPrice(priceInfo, areaInfo)
    val transaction = new Transaction
    transaction.setAreaPrice(areaPrice)
    offer.setTransaction(transaction)
    val image = RealtyPhotoInfo.newBuilder()
    image.setAesthetic(BoolValue.newBuilder().setValue(true))
    image.setExternalUrl("realty.ru")
    image.setMdsId(
      MdsImageId
        .newBuilder()
        .setKnownNamespace(MdsImageId.KnownNamespace.REALTY)
        .setGroup(15)
        .setName("image")
        .setNamespaceName("verba")
        .build()
    )
    val ui = UnifiedImages.newBuilder()
    ui.addImage(image)
    offer.setPhotos(ui.build(), 1)
    offer.createAndGetSaleAgent().setOrganization("org")
    offer
  }

}
