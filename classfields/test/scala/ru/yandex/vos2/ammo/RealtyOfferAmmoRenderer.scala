package ru.yandex.vos2.ammo

import play.api.libs.json._
import ru.yandex.vos2.BasicsModel
import ru.yandex.vos2.OfferModel._
import ru.yandex.vertis.vos2.model.realty.{PricePaymentPeriod, RealtyOffer}
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.util.model.DateFormatter

import scala.collection.JavaConverters._

/**
  * @author Leonid Nalchadzhi (nalchadzhi@yandex-team.ru)
  */
object RealtyOfferAmmoRenderer extends DateFormatter {

  def render(o: Offer): JsObject = {
    Json.obj(
      "telephones" → JsArray(o.getUserContacts.phones.map(JsString)),
      "common" → renderCommon(o, o.getOfferRealty),
      "price" → renderPrice(o, o.getOfferRealty),
      "location" → renderLocation(o, o.getOfferRealty)
    )
  }

  private def renderCommon(o: Offer, ro: RealtyOffer): JsObject =
    Json.obj(
      "offerType" → ro.getOfferType.toString,
      "propertyType" → ro.getPropertyType.toString,
      "category" → ro.getCategory.toString
    )

  private def renderPrice(o: Offer, ro: RealtyOffer): JsObject = {
    val price = ro.getPrice
    Json.obj(
      "value" → price.getPriceValue,
      "period" → PricePaymentPeriod.PER_MONTH.name(),
      "currency" → price.getCurrency.toString
    )
  }

  private def renderLocation(o: Offer, ro: RealtyOffer): JsObject = {
    val location = ro.getAddress
    Json.obj(
      "rgid" → location.getRgid,
      "country" → location.getCountry,
      "region" → location.getRegion,
      "district" → location.getDistrict,
      "localityName" → location.getLocalityName,
      "subLocalityName" → location.getSubLocalityName,
      "address" → location.getAddress,
      "direction" → location.getDirection,
      "distance" → location.getDistanceMKAD,
      "latitude" → location.getGeoPoint.getLatitude,
      "longitude" → location.getGeoPoint.getLongitude,
      "railwayStation" → location.getRailwayStation,
      "metro" → renderMetros(location.getMetroList.asScala)
    )
  }

  def renderMetros(metros: Seq[BasicsModel.Metro]): JsArray = {
    JsArray(
      metros.map(
        m ⇒ Json.obj("name" → m.getName, "timeOnTransport" → m.getTimeOnTransport, "timeOnFoot" → m.getTimeOnTransport)
      )
    )
  }
}
