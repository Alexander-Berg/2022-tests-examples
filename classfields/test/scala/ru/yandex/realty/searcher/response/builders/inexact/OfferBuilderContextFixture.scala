package ru.yandex.realty.searcher.response.builders.inexact

import java.util

import org.scalamock.scalatest.MockFactory
import ru.yandex.common.util.currency.Currency
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.model.offer.{ApartmentInfo, OfficeClass, _}
import ru.yandex.realty.searcher.response.OfferResponse.{Building, Commercial, House, Lot}
import ru.yandex.realty.searcher.response.location.MetroResponse

trait OfferBuilderContextFixture extends MockFactory {

  val dummyPriceInfo50Rub = PriceInfo.create(Currency.RUR, 50f, PricingPeriod.PER_MONTH, AreaUnit.WHOLE_OFFER)
  val dummyPriceInfo600Rub = PriceInfo.create(Currency.RUR, 600f, PricingPeriod.PER_MONTH, AreaUnit.WHOLE_OFFER)
  val temporaryRentValue40000Rub = 40000L
  val dummyPriceInfo50000Rub = PriceInfo.create(Currency.RUR, 50000f, PricingPeriod.PER_MONTH, AreaUnit.WHOLE_OFFER)
  val offer = mock[Offer]
  val building = mock[Building]
  val house = mock[House]
  val commercial = mock[Commercial]
  val officeClass = OfficeClass.UNKNOWN
  val apartmentInfo = mock[ApartmentInfo]
  val transaction = mock[Transaction]
  val lot = mock[Lot]
  val livingSpace = mock[AreaInfo]
  val kitchenSpace = mock[AreaInfo]
  val metro = mock[MetroResponse]
  val regionGraph = mock[RegionGraph]

  val dummyOfferBuilderContext: OfferBuilderContext = OfferBuilderContext(
    offer = offer,
    price = dummyPriceInfo50Rub,
    improvements = util.Collections.emptySet(),
    building = building,
    dealStatus = DealStatus.UNKNOWN,
    taxationForm = TaxationForm.UNKNOWN,
    house = house,
    commercial = commercial,
    officeClass = officeClass,
    apartmentInfo = apartmentInfo,
    rentCondition = util.Collections.emptySet(),
    commissioningDateIndexValue = 0,
    transaction = transaction,
    lot = lot,
    livingSpace = livingSpace,
    kitchenSpace = kitchenSpace,
    metro = metro,
    regionGraph = regionGraph
  )

  val offerBuilderContextWithTemporaryPrice: OfferBuilderContext = OfferBuilderContext(
    offer = offer,
    price = dummyPriceInfo50000Rub,
    improvements = util.Collections.emptySet(),
    building = building,
    dealStatus = DealStatus.UNKNOWN,
    taxationForm = TaxationForm.UNKNOWN,
    house = house,
    commercial = commercial,
    officeClass = officeClass,
    apartmentInfo = apartmentInfo,
    rentCondition = util.Collections.emptySet(),
    commissioningDateIndexValue = 0,
    transaction = transaction,
    lot = lot,
    livingSpace = livingSpace,
    kitchenSpace = kitchenSpace,
    metro = metro,
    regionGraph = regionGraph
  )
}
