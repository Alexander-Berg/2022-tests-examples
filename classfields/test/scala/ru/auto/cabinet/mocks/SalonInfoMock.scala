package ru.auto.cabinet.mocks

import ru.auto.cabinet.model.SalonInfo
import ru.auto.cabinet.model.SalonInfo.FileUpdate

object SalonInfoMock {

  val poi = SalonInfo.Poi(
    id = 1,
    address = "poi_address",
    geoId = 1234,
    lat = 2.0,
    lng = 2.6,
    cityId = Some(1),
    regionId = Some(2),
    countryId = Some(3),
    yaCityId = 4,
    yaRegionId = 5,
    yaCountryId = 6
  )

  val phone = SalonInfo.Phone(
    id = Some("phone_id"),
    deleteRow = false,
    title = Some("phone_title"),
    countryCode = "country_code",
    cityCode = "city_code",
    phone = "phone",
    localPhone = "local_phone",
    phoneMask = "1:2:3",
    callFrom = 1,
    callTill = 2,
    extention = "extention"
  )

  val logo = FileUpdate(
    origin = List("321-logo_origin"),
    delete = List("321-logo_origin"),
    `new` = List("123-logo_new")
  )

  val photo = FileUpdate(
    origin = List("123-photo_origin"),
    delete = List("123-photo_origin"),
    `new` = List("234-photo_new")
  )

  val rentalCertificate = FileUpdate(
    origin =
      List("1345-rent_certificate_origin1", "532-rent_certificate_origin2.pdf"),
    delete = List.empty,
    `new` = List.empty)

  val dealership = SalonInfo.Dealership(
    id = Some("dealership_id"),
    markId = "VOLVO",
    markName = "Volvo",
    origin = List("4324-dealership_origin", "1234-dealership_origin.pdf"),
    `new` = Nil,
    delete = List("4324-dealership_origin"),
    deleteRow = false
  )

  val salonInfo = SalonInfo(
    title = "salon_title",
    description = Some("salon_description"),
    url = Some("salon_url"),
    poi = poi,
    phones = List(phone),
    logo,
    photo,
    rentalCertificate,
    lessor = Some("lessor"),
    dealership = List(dealership)
  )

}
