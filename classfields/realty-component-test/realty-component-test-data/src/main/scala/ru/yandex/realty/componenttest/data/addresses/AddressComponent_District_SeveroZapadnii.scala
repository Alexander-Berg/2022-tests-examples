package ru.yandex.realty.componenttest.data.addresses

import ru.yandex.realty.model.message.RealtySchema.AddressComponentMessage
import ru.yandex.realty.proto.RegionType

object AddressComponent_District_SeveroZapadnii {

  val Proto: AddressComponentMessage =
    AddressComponentMessage
      .newBuilder()
      .setValue("Северо-Западный федеральный округ")
      .setRegionType(RegionType.SUBJECT_FEDERATION)
      .build()

}
