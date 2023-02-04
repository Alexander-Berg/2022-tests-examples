package ru.yandex.realty.componenttest.data.locations

import ru.yandex.realty.componenttest.data.addresses.AddressComponentUtils.asAddressComponentMessage
import ru.yandex.realty.componenttest.data.addresses.{
  AddressComponent_CityDistrict_AptekarskiiOstrov,
  AddressComponent_CityDistrict_Petrogradskii,
  AddressComponent_City_SPb,
  AddressComponent_District_SeveroZapadnii,
  AddressComponent_Russia,
  AddressComponent_Site_SkandiKlub
}
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.message.RealtySchema.{
  GeoPointMessage,
  LocationMessage,
  MetroWithDistanceMessage,
  StationWithDistanceMessage
}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.model.serialization.RealtySchemaVersions
import ru.yandex.realty.proto.RegionType
import ru.yandex.realty.proto.unified.offer.address.Address

object Location_Site_SkandiKlub {

  val Proto: LocationMessage =
    LocationMessage
      .newBuilder()
      .setVersion(RealtySchemaVersions.LOCATION_VERSION)
      .setGeocoderId(2)
      .setSubjectFederationId(Regions.SPB_AND_LEN_OBLAST)
      .setSubjectFederationRgid(NodeRgid.SPB_AND_LEN_OBLAST)
      .addDistrict(417963)
      .addStation(
        StationWithDistanceMessage
          .newBuilder()
          .setVersion(RealtySchemaVersions.STATION_WITH_DISTANCE_VERSION)
          .setEsr(38900)
          .setDistance(2250.0f)
          .build()
      )
      .addStation(
        StationWithDistanceMessage
          .newBuilder()
          .setVersion(RealtySchemaVersions.STATION_WITH_DISTANCE_VERSION)
          .setEsr(38215)
          .setDistance(2301.0f)
          .build()
      )
      .addStation(
        StationWithDistanceMessage
          .newBuilder()
          .setVersion(RealtySchemaVersions.STATION_WITH_DISTANCE_VERSION)
          .setEsr(37405)
          .setDistance(2533.0f)
          .build()
      )
      .setLocalityName("Санкт-Петербург")
      .setGeocoderPoint(
        GeoPointMessage
          .newBuilder()
          .setVersion(RealtySchemaVersions.GEO_POINT_VERSION)
          .setLatitude(59.975075f)
          .setLongitude(30.319542f)
          .build()
      )
      .setManualPoint(
        GeoPointMessage
          .newBuilder()
          .setVersion(RealtySchemaVersions.GEO_POINT_VERSION)
          .setLatitude(59.975075f)
          .setLongitude(30.319542f)
          .build()
      )
      .addMetro(
        MetroWithDistanceMessage
          .newBuilder()
          .setId(279216)
          .setGeoId(20336)
          .setTimeOnTransport(8)
          .setTimeOnFoot(16)
          .build()
      )
      .addMetro(
        MetroWithDistanceMessage
          .newBuilder()
          .setId(279217)
          .setGeoId(20330)
          .setTimeOnTransport(13)
          .setTimeOnFoot(27)
          .build()
      )
      .setGeocoderAddress(
        "Россия, Санкт-Петербург, Петроградский район, муниципальный округ Аптекарский Остров, жилой комплекс Сканди Клуб"
      )
      .setRawAddress("Россия, Санкт-Петербург, Аптекарский пр.")
      .setHasMap(false)
      .setAccuracyInt(1)
      .setRegionGraphId(311203)
      .addSecondaryDistricts(311203)
      .addSecondaryDistricts(417979)
      .setInexactPoint(
        GeoPointMessage
          .newBuilder()
          .setVersion(RealtySchemaVersions.GEO_POINT_VERSION)
          .setLatitude(GeoPoint.UNKNOWN.getLatitude)
          .setLongitude(GeoPoint.UNKNOWN.getLongitude)
          .build()
      )
      .setRegionName("Санкт-Петербург")
      .setFullStreetAddress(
        "Россия, Санкт-Петербург, Петроградский район, муниципальный округ Аптекарский Остров, жилой комплекс Сканди Клуб"
      )
      .addComponent(asAddressComponentMessage(AddressComponent_Russia.Proto))
      .addComponent(AddressComponent_District_SeveroZapadnii.Proto)
      .addComponent(asAddressComponentMessage(AddressComponent_City_SPb.Proto, RegionType.SUBJECT_FEDERATION))
      .addComponent(asAddressComponentMessage(AddressComponent_City_SPb.Proto))
      .addComponent(asAddressComponentMessage(AddressComponent_CityDistrict_Petrogradskii.Proto))
      .addComponent(asAddressComponentMessage(AddressComponent_CityDistrict_AptekarskiiOstrov.Proto))
      .addComponent(asAddressComponentMessage(AddressComponent_Site_SkandiKlub.Proto))
      .setStructuredAddress2(
        Address
          .newBuilder()
          .addComponent(AddressComponent_Russia.Proto)
          .addComponent(AddressComponent_City_SPb.Proto)
          .addComponent(AddressComponent_CityDistrict_Petrogradskii.Proto)
          .addComponent(AddressComponent_CityDistrict_AptekarskiiOstrov.Proto)
          .addComponent(AddressComponent_Site_SkandiKlub.Proto)
          .build()
      )
      .build()

}
