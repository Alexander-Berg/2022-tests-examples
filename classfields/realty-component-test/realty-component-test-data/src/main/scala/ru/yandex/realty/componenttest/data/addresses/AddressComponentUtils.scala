package ru.yandex.realty.componenttest.data.addresses

import ru.yandex.realty.model.message.RealtySchema.AddressComponentMessage
import ru.yandex.realty.proto.RegionType
import ru.yandex.realty.proto.unified.offer.address.Address

object AddressComponentUtils {

  def asAddressComponentMessage(address: Address.Component): AddressComponentMessage = {
    AddressComponentMessage
      .newBuilder()
      .setValue(address.getValue)
      .setRegionType(address.getRegionType)
      .build()
  }

  def asAddressComponentMessage(address: Address.Component, targetRegionType: RegionType): AddressComponentMessage = {
    AddressComponentMessage
      .newBuilder()
      .setValue(address.getValue)
      .setRegionType(targetRegionType)
      .build()
  }

}
