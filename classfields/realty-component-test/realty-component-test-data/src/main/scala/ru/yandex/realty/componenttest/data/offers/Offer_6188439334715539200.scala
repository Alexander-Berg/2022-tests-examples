package ru.yandex.realty.componenttest.data.offers

import java.util.Collections.singletonList
import com.google.common.collect.ImmutableList
import com.google.protobuf.{Duration, UInt32Value}
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.componenttest.data.offers.common.ComponentTestOffer
import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.{buildNew, initialize}
import ru.yandex.realty.model.location.{Location, LocationAccuracy}
import ru.yandex.realty.model.message.RealtySchema.AddressComponentMessage
import ru.yandex.realty.model.offer.{
  ApartmentInfo,
  AreaInfo,
  AreaPrice,
  AreaUnit,
  BuildingInfo,
  BuildingState,
  BuildingType,
  CategoryType,
  FlatType,
  HouseInfo,
  Money,
  OfferType,
  ParkingType,
  PaymentType,
  PhoneNumber,
  PriceInfo,
  PricingPeriod,
  Renovation,
  SalesAgentCategory,
  Transaction
}
import ru.yandex.realty.model.sites.Decoration
import ru.yandex.realty.proto.offer.{FreeReportAccessibility, PaidReportAccessibility}
import ru.yandex.realty.proto.unified.offer.address.TransportDistance.TransportType
import ru.yandex.realty.proto.unified.offer.address.{Address, Airport, CityCenter, TransportDistance}
import ru.yandex.realty.proto.unified.offer.cadastr.CadastrInfo
import ru.yandex.realty.proto.unified.vos.offer.Publishing.ShowStatus
import ru.yandex.realty.proto.{GeoPoint, RegionType}

object Offer_6188439334715539200 extends ComponentTestOffer {

  {
    val area = AreaInfo.create(AreaUnit.SQUARE_METER, 25.55f)
    val transaction = buildNew[Transaction] { t =>
      t.setAreaPrice(
        new AreaPrice(
          PriceInfo.create(
            Money.of(Currency.RUR, 2056775),
            PricingPeriod.WHOLE_LIFE,
            AreaUnit.WHOLE_OFFER
          ),
          area
        )
      )
      t.setSqmInRubles(Money.of(Currency.RUR, 8050000), 25.55f)
      t.setWholeInRubles(Money.of(Currency.RUR, 205677500))
    }
    val buildingInfo = buildNew[BuildingInfo] { b =>
      b.setBuildingName("Янила Драйв", true)
      b.setBuildingState(BuildingState.UNFINISHED)
      b.setBuildingType(BuildingType.PANEL)
      b.setBuildingPhase("4")
      b.setBuildingSection("1")
      b.setFloorsTotal(9)
      b.setParkingType(ParkingType.OPEN)
      b.setSiteId(Site_57547.Id)
      b.setPhaseId(2243856L)
    }
    val location = buildNew[Location] { l =>
      l.setAccuracy(LocationAccuracy.EXACT)
      l.setAirports(
        singletonList(
          Airport
            .newBuilder()
            .addDistances(
              TransportDistance
                .newBuilder()
                .setTime(
                  Duration
                    .newBuilder()
                    .setSeconds(2133)
                    .build()
                )
                .setDistance(UInt32Value.of(32039))
                .setTransportType(TransportType.CAR)
                .build()
            )
            .build()
        )
      )
      l.setCityCenters(
        singletonList(
          CityCenter
            .newBuilder()
            .setCoordinates(
              GeoPoint
                .newBuilder()
                .setLatitude(59.933926f)
                .setLongitude(30.307991f)
                .build()
            )
            .addDistances(
              TransportDistance
                .newBuilder()
                .setTime(
                  Duration
                    .newBuilder()
                    .setSeconds(1760)
                    .build()
                )
                .setDistance(UInt32Value.of(17431))
                .setTransportType(TransportType.CAR)
                .build()
            )
            .build()
        )
      )
      l.setCombinedAddress("Россия, Санкт-Петербург, Голландская улица")
      l.setComponents(
        ImmutableList.of(
          AddressComponentMessage
            .newBuilder()
            .setRegionType(RegionType.COUNTRY)
            .setValue("Россия")
            .build(),
          AddressComponentMessage
            .newBuilder()
            .setRegionType(RegionType.SUBJECT_FEDERATION)
            .setValue("Северо-Западный федеральный округ")
            .build(),
          AddressComponentMessage
            .newBuilder()
            .setRegionType(RegionType.SUBJECT_FEDERATION)
            .setValue("Санкт-Петербург")
            .build(),
          AddressComponentMessage
            .newBuilder()
            .setRegionType(RegionType.CITY)
            .setValue("Санкт-Петербург")
            .build()
        )
      )
      l.setFullStreetAddress("Россия, Санкт-Петербург")
      l.setGeocoderId(2)
      l.setGeocoderLocation(
        "Россия, Санкт-Петербург",
        new ru.yandex.realty.model.location.GeoPoint(59.952316f, 30.578295f)
      )
      l.setInexactPoint(null)
      l.setModerationPoint(null)
      l.setRawAddress("Россия, Санкт-Петербург, Янино, Голландская улица")
      l.setRegionGraphId(417899L)
      l.setRegionName("Санкт-Петербург")
      l.setStreetAddress("Голландская улица")
      l.setStructuredAddress(
        Address
          .newBuilder()
          .addComponent(
            Address.Component
              .newBuilder()
              .setGeoId(225)
              .setRegionType(RegionType.COUNTRY)
              .setRgid(143)
              .setValue("Россия")
              .setValueForAddress("Россия")
              .build()
          )
          .addComponent(
            Address.Component
              .newBuilder()
              .setGeoId(2)
              .setRegionType(RegionType.CITY)
              .setRgid(417899)
              .setValue("Санкт-Петербург")
              .setValueForAddress("Санкт-Петербург")
              .build()
          )
          .build()
      )
      l.setSubjectFederation(10174, 741965)
    }
    val cadastrInfo = CadastrInfo
      .newBuilder()
      .setFreeReportAccessibility(FreeReportAccessibility.FRA_NONE)
      .setPaidReportAccessibility(PaidReportAccessibility.PPA_NOT_ALLOWED_TO_BUY)
      .build()
    val saleAgent = initialize(createAndGetSaleAgent()) { sa =>
      sa.setCategory(SalesAgentCategory.AGENCY)
      sa.setUnifiedPhones(
        singletonList(
          new PhoneNumber("812", "5048992", "7")
        )
      )
    }

    setApartmentInfo(OfferApartmentInfo)
    setArea(area)
    setTransaction(transaction)
    setBuildingInfo(buildingInfo)
    setCategoryType(CategoryType.APARTMENT)
    setClusterHeader(true)
    setClusterHeader2(true)
    setCreateTimeMillis(1581285801000L)
    setExclusive(true)
    setExportToVos(true)
    setHeadL(0)
    setHeadL2(0)
    setHeadR(1)
    setHeadR2(0)
    setHouseInfo(new HouseInfo)
    setImageOrderChangeAllowed(true)
    setInternal(true)
    setLocation(location)
    setModernCadastrInfo(cadastrInfo)
    setOfferType(OfferType.SELL)
    setPartnerId(1069191800)
    setPartnerInternalId("29526")
    setPaymentType(PaymentType.JURIDICAL_PERSON)
    setShowStatusAndRevoked(ShowStatus.PUBLISHED, false)
    setUid("662158695")
  }

  private object OfferApartmentInfo extends ApartmentInfo {

    setDecoration(Decoration.CLEAN)
    addEnrichedRenovation(Renovation.NON_GRANDMOTHER)
    setFlatType(FlatType.NEW_FLAT)
    setFloors(singletonList(2))
    setNewFlat(true)
    setStudio(true)

  }

}
