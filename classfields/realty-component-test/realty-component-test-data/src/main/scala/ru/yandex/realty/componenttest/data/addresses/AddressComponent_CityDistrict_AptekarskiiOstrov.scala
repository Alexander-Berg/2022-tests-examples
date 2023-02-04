package ru.yandex.realty.componenttest.data.addresses

import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.proto.RegionType
import ru.yandex.realty.proto.unified.offer.address.Address

object AddressComponent_CityDistrict_AptekarskiiOstrov {

  val Proto: Address.Component =
    Address.Component
      .newBuilder()
      .setValue("муниципальный округ Аптекарский Остров")
      .setValueForAddress("муниципальный округ Аптекарский Остров")
      .setRegionType(RegionType.CITY_DISTRICT)
      .setGeoId(Regions.SPB)
      .setRgid(NodeRgid.SPB)
      .build()

}
