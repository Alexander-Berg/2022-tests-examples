package ru.yandex.vos2.model

import org.scalacheck.Gen
import ru.yandex.vertis.vos2.model.realty.Price
import ru.yandex.vos2.BasicsModel.{Currency, GeoPoint, Location}
import ru.yandex.vos2.model.CommonGen.limitedStr

/**
 * @author Leonid Nalchadzhi (nalchadzhi@yandex-team.ru)
  */
object BasicGenerator {

  val geoPointGen = for {
    lon ← Gen.Choose.chooseDouble.choose(0, 360)
    lat ← Gen.Choose.chooseDouble.choose(0, 360)
  } yield GeoPoint.newBuilder().
    setLatitude(lat).
    setLongitude(lon).
    build()

  val LocationGen = for {
    country ← limitedStr()
    region ← limitedStr()
    district ← limitedStr()
    localityName ← limitedStr()
    subLocalityName ← limitedStr()
    nonAdminLocality ← limitedStr()
    address ← limitedStr()
    direction ← limitedStr()
    railway ← limitedStr()
    kadDistance ← Gen.Choose.chooseFloat.choose(0, 100)
    geoPoint ← geoPointGen
  } yield Location.newBuilder().
    setCountry(country).
    setRegion(region).
    setDistrict(district).
    setLocalityName(localityName).
    setSubLocalityName(subLocalityName).
    setNonAdminSubLocality(nonAdminLocality).
    setAddress(address).
    setDirection(direction).
    setRailwayStation(railway).
    setDistanceMKAD(kadDistance).
    setGeoPoint(geoPoint).
    build()

  // TODO add new fields
  val PriceGen = for {
    priceValue ← Gen.Choose.chooseDouble.choose(0, 1000000)
    currency ← Gen.oneOf(Currency.values())
  } yield Price.newBuilder()
    .setPriceValue(priceValue)
    .setCurrency(currency)

}
