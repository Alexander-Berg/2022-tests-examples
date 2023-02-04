package ru.yandex.vertis.general.users.model.testkit

import common.geobase.model.RegionIds.RegionId
import ru.yandex.vertis.general.users.model.SellingAddress._
import ru.yandex.vertis.general.users.model.User.{UserId, UserInput}
import ru.yandex.vertis.general.users.model.{SellingAddress, StoreInfo}
import zio.random.Random
import zio.test.{Gen, Sized}

object UserGen {

  def anyGeoPoint(): Gen[Random, GeoPoint] = {
    for {
      latitude <- Gen.anyDouble
      longitude <- Gen.anyDouble
    } yield GeoPoint(latitude = latitude, longitude = longitude)
  }

  def anyAddressInfo(): Gen[Random with Sized, AddressInfo] =
    for {
      name <- Gen.anyString
    } yield AddressInfo(name)

  def anyMetroInfo(): Gen[Random with Sized, MetroStationInfo] =
    for {
      id <- Gen.anyLong
      name <- Gen.anyString
    } yield MetroStationInfo(id, Seq(), name, Seq())

  def anyDistrictInfo(): Gen[Random with Sized, DistrictInfo] =
    for {
      id <- Gen.anyLong
      name <- Gen.anyString
    } yield DistrictInfo(id, name)

  def anyRegionInfo(): Gen[Random with Sized, RegionInfo] =
    for {
      id <- Gen.anyLong
      name <- Gen.anyString
    } yield RegionInfo(RegionId(id), name)

  def anyAddress(
      geopoint: Gen[Random, GeoPoint] = anyGeoPoint(),
      address: Gen[Random with Sized, Option[AddressInfo]] = Gen.option(anyAddressInfo()),
      nearestMetroStation: Gen[Random with Sized, Option[MetroStationInfo]] = Gen.option(anyMetroInfo()),
      district: Gen[Random with Sized, Option[DistrictInfo]] = Gen.option(anyDistrictInfo()),
      region: Gen[Random with Sized, Option[RegionInfo]] =
        Gen.option(anyRegionInfo())): Gen[Random with Sized, SellingAddress] =
    for {
      geopoint <- geopoint
      address <- address
      nearestMetroStation <- nearestMetroStation
      district <- district
      region <- region
    } yield SellingAddress(geopoint, address, nearestMetroStation, district, region)

  def anyPhone(): Gen[Random, String] = {
    Gen.listOfN(7)(Gen.numericChar).map { chars =>
      "+7495" + new String(chars.toArray)
    }
  }

  def anyStoreInfo(): Gen[Random with Sized, StoreInfo] = {
    for {
      description <- Gen.option(Gen.anyString)
      workingHours <- Gen.option(Gen.anyString)
      deliveryPaymentInfo <- Gen.option(Gen.anyString)
      websiteUrl <- Gen.option(Gen.anyString)
    } yield StoreInfo(description, workingHours, deliveryPaymentInfo, websiteUrl)
  }

  def anyUser(
      name: Gen[Random with Sized, Option[String]] = Gen.option(Gen.anyString),
      addresses: Gen[Random with Sized, Seq[SellingAddress]] = Gen.listOf(anyAddress()),
      ymlPhone: Gen[Random with Sized, Option[String]] = Gen.option(anyPhone()),
      isPhoneRedirectEnabled: Gen[Random with Sized, Option[Boolean]] = Gen.option(Gen.boolean),
      isStore: Gen[Random, Option[Boolean]] = Gen.option(Gen.boolean),
      storeInfo: Gen[Random with Sized, Option[StoreInfo]] = Gen.option(anyStoreInfo())) = for {
    name <- name
    addresses <- addresses
    phone <- ymlPhone
    phoneRedirect <- isPhoneRedirectEnabled
    isStore <- isStore
    storeInfo <- storeInfo
  } yield UserInput(name, addresses, phone, phoneRedirect, isStore, storeInfo)

  val anyUser: Gen[Random with Sized, UserInput] = anyUser().noShrink
  val anyUserWithName: Gen[Random with Sized, UserInput] = anyUser(name = Gen.some(Gen.anyString)).noShrink

  val anyUserId: Gen[Random with Sized, UserId] = Gen.anyLong.map(_.abs).filter(_ > 0).map(UserId.apply).noShrink
}
