package ru.yandex.realty.componenttest.data.addresses

import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.proto.RegionType
import ru.yandex.realty.proto.unified.offer.address.Address

object AddressComponent_Site_SkandiKlub {

  val Proto: Address.Component =
    Address.Component
      .newBuilder()
      .setValue("жилой комплекс Сканди Клуб")
      .setValueForAddress("жилой комплекс Сканди Клуб")
      .setRegionType(RegionType.CITY_DISTRICT)
      .setGeoId(Regions.SPB)
      .setRgid(NodeRgid.SPB)
      .build()

}
