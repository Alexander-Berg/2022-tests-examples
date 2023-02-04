package ru.yandex.realty.componenttest.extdata.stubs

import com.google.protobuf.{BoolValue, FloatValue, Int32Value, Timestamp}
import org.joda.time.Instant
import ru.yandex.realty.componenttest.extdata.core.ExtdataResourceStub
import ru.yandex.realty.context.RealtyDataTypes.RealtyDataType
import ru.yandex.realty.proto.{FloatRange, GeoPoint, Int32Range, Polygon}
import ru.yandex.realty.proto.village.{
  CottageStatus,
  Facilities,
  Heating,
  Infrastructure,
  InfrastructureType,
  LandType,
  Security,
  Sewerage,
  VillageClass,
  VillageOfferStatus,
  VillagePhase,
  VillagePhaseStatus,
  VillageType,
  WallsType,
  WaterSupply
}
import ru.yandex.realty.proto.village.raw.{RawVillage, RawVillageLocation, RawVillageOfferType, VillageTypeInfo}

import scala.collection.JavaConverters._

trait ExtdataRawVillagesResourceStub extends ExtdataResourceStub {

  private val rawVillages: Seq[RawVillage] = {
    Seq(
      RawVillage
        .newBuilder()
        .setId(1757888)
        .setVillageType(VillageType.VILLAGE_TYPE_COTTAGE)
        .setName("Истринская Ривьера")
        .setOfficialUrl("https://honka.ru/cottage_villages/istrinskaya-rivera/o-poselke/")
        .setDescription("Небольшая и уютная — «Истринская Ривьера» раскинулась на берегу Истринского водохранилища.")
        .addDeveloperId(1757819)
        .addAgencyPhone("+7 (495) 921-30-01")
        .setLocation(
          RawVillageLocation
            .newBuilder()
            .setRegion("Московская обл.")
            .setGeocoderAddress(
              "Московская обл., Солнечногорский район, пос. Соколовское, д. Трусово, КП Истринская Ривьера"
            )
            .setPostAddress("Солнечногорский район, пос. Соколовское, д. Трусово")
            .setGeoPoint(
              GeoPoint
                .newBuilder()
                .setLatitude(56.048203f)
                .setLongitude(36.881706f)
                .build()
            )
            .setPolygon(
              Polygon
                .newBuilder()
                .addAllLatitudes(
                  Seq(
                    56.047127f, 56.04672f, 56.04859f, 56.04889f, 56.050007f, 56.050297f, 56.050045f, 56.049282f,
                    56.048637f, 56.048134f
                  ).map(Float.box).asJava
                )
                .addAllLongitudes(
                  Seq(
                    36.879375f, 36.882164f, 36.88294f, 36.883625f, 36.883408f, 36.881477f, 36.879524f, 36.878925f,
                    36.879677f, 36.88004f
                  ).map(Float.box).asJava
                )
                .build()
            )
            .build()
        )
        .setVillageClass(VillageClass.VILLAGE_CLASS_ELITE)
        .setLandType(LandType.LAND_TYPE_IZHS)
        .setTotalObjects(Int32Value.of(25))
        .setSoldObjects(Int32Value.of(24))
        .setFacilities(
          Facilities
            .newBuilder()
            .setElectricity(BoolValue.of(true))
            .setHeating(Heating.HEATING_GAS)
            .setSewerage(Sewerage.SEWERAGE_CENTRAL)
            .setWaterSupply(WaterSupply.WATER_SUPPLY_CENTRAL)
            .build()
        )
        .addVillageTypeInfo(
          VillageTypeInfo
            .newBuilder()
            .setOfferType(RawVillageOfferType.RAW_VILLAGE_OFFER_TYPE_COTTAGE)
            .setOfferStatus(VillageOfferStatus.VILLAGE_OFFER_STATUS_ON_SALE)
            .addCottageStatus(CottageStatus.COTTAGE_STATUS_READY)
            .setCottageArea(
              FloatRange
                .newBuilder()
                .setFrom(FloatValue.of(420.0f))
                .setTo(FloatValue.of(420.0f))
                .build()
            )
            .setLotArea(
              FloatRange
                .newBuilder()
                .setFrom(FloatValue.of(20.2f))
                .setTo(FloatValue.of(20.2f))
                .build()
            )
            .setTotalPrice(
              FloatRange
                .newBuilder()
                .setFrom(FloatValue.of(25900000f))
                .setTo(FloatValue.of(25900000f))
                .build()
            )
            .setDescription(
              "Неотъемлемой частью каждого из проектов являются довольно большие площади остекления. " +
                "Этот архитектурный прием был использован не случайно – благодаря множеству окон из каждого дома " +
                "открывается великолепный вид на Истринское водохранилище."
            )
            .addWallsTypes(WallsType.WALLS_TYPE_WOOD)
            .setFloors(
              Int32Range
                .newBuilder()
                .setFrom(Int32Value.of(2))
                .setTo(Int32Value.of(2))
                .build()
            )
            .build()
        )
        .addAllInfrastructures(
          Seq(
            Infrastructure
              .newBuilder()
              .setType(InfrastructureType.INFRASTRUCTURE_TYPE_GUEST_PARKING)
              .build(),
            Infrastructure
              .newBuilder()
              .setType(InfrastructureType.INFRASTRUCTURE_TYPE_PLAYGROUND)
              .build(),
            Infrastructure
              .newBuilder()
              .setType(InfrastructureType.INFRASTRUCTURE_TYPE_FOREST)
              .build(),
            Infrastructure
              .newBuilder()
              .setType(InfrastructureType.INFRASTRUCTURE_TYPE_MARKET)
              .build()
          ).asJava
        )
        .addSecurity(Security.SECURITY_PSC_VIP)
        .addPhase(
          VillagePhase
            .newBuilder()
            .setDescription("1 очередь")
            .setIndex(1)
            .setStatus(VillagePhaseStatus.VILLAGE_PHASE_STATUS_HAND_OVER)
            .setFinishDate(
              Timestamp
                .newBuilder()
                .setSeconds(Instant.now().getMillis / 1000)
                .build()
            )
            .build()
        )
        .build()
    )
  }

  stubGzipped(RealtyDataType.RawVillages, rawVillages)

}
