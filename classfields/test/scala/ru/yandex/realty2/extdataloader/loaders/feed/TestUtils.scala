package ru.yandex.realty2.extdataloader.loaders.feed

import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.model.offer._

object TestUtils {

  // next time use MockOfferBuilder
  val offerMoscowApartmentSell: Offer = {
    val offer = new Offer()
    offer.setId(6302897158613069191L)
    offer.setCategoryType(CategoryType.APARTMENT)
    offer.setOfferType(OfferType.SELL)
    offer.setLocation(new Location)
    offer.getLocation.setGeocoderId(1)
    val geoPoint = new GeoPoint(37.242355f, 55.777267f)
    offer.getLocation.setManualPoint(geoPoint)
    offer.getLocation.setStreetAddress("Глухово д, жилой комплекс Ильинские Луга, к38")

    offer.setApartmentInfo(new ApartmentInfo)
    offer.getApartmentInfo.setRooms(3)
    offer.getApartmentInfo.setFlatType(FlatType.NEW_FLAT)

    offer.setTransaction(new Transaction)
    val money = Money.scaledOf(Currency.RUR, 691898000L)
    offer.getTransaction.setWholeInRubles(money)

    val priceInfo = PriceInfo.create(Currency.RUR, 10f, PricingPeriod.WHOLE_LIFE, AreaUnit.SQUARE_METER)
    val areaPrice = new AreaPrice(priceInfo, AreaInfo.create(AreaUnit.SQUARE_METER, 91.4f))
    offer.getTransaction.setAreaPrice(areaPrice)
    offer.getTransaction.getArea

    offer
  }

}
