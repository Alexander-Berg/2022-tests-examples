package ru.yandex.realty.componenttest.data.addresses

import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.proto.RegionType
import ru.yandex.realty.proto.unified.offer.address.Address

object AddressComponent_Russia {

  val Proto: Address.Component =
    Address.Component
      .newBuilder()
      .setValue("Россия")
      .setValueForAddress("Россия")
      .setRegionType(RegionType.COUNTRY)
      .setGeoId(Regions.RUSSIA)
      .setRgid(NodeRgid.RUSSIA)
      .build()

}
