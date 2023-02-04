package ru.yandex.realty.componenttest.data.addresses

import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.proto.RegionType
import ru.yandex.realty.proto.unified.offer.address.Address

object AddressComponent_City_SPb {

  val Proto: Address.Component =
    Address.Component
      .newBuilder()
      .setValue("Санкт-Петербург")
      .setValueForAddress("Санкт-Петербург")
      .setRegionType(RegionType.CITY)
      .setGeoId(Regions.SPB)
      .setRgid(NodeRgid.SPB)
      .build()

}
