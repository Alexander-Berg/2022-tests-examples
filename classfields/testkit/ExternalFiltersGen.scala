package ru.auto.comeback.model.testkit

import ru.auto.api.comeback_model.ComebackListingRequest.{CatalogFilter, Filter}
import ru.auto.comeback.model.testkit.CommonGen._
import ru.auto.comeback.model.testkit.OfferGen._
import zio.random.Random
import zio.test.{Gen, Sized}

import java.time.temporal.ChronoUnit

object ExternalFiltersGen {

  val nonEmptyCatalogFilter: Gen[Random with Sized, CatalogFilter] = for {
    mark <- Gen.some(anyMark)
    model <- Gen.some(anyModel)
    superGen <- Gen.some(Gen.long(0, Int.MaxValue - 1))
  } yield new CatalogFilter(mark = mark, model = model, superGen = superGen)

  private val creationDateFilters = for {
    creationDateFrom <- Gen.option(anyInstant)
    creationDateTo <- creationDateFrom match {
      case Some(date) => Gen.option(Gen.instant(date, date.plus(100, ChronoUnit.DAYS)))
      case _ => Gen.option(anyInstant)
    }
  } yield (creationDateFrom, creationDateTo)

  val anyComebackFilter: Gen[Random with Sized, Filter] = for {
    catalogFilter <- Gen.listOf(nonEmptyCatalogFilter)
    yearFrom <- Gen.option(Gen.int(1960, 2020))
    yearTo <- Gen.option(Gen.int(1960, 2020))
    priceFrom <- Gen.option(Gen.int(100000, 5000000))
    priceTo <- Gen.option(Gen.int(100000, 5000000))
    kmAgeFrom <- Gen.option(Gen.int(1000, 100000))
    kmAgeTo <- Gen.option(Gen.int(1000, 100000))
    (creationDateFrom, creationDateTo) <- creationDateFilters
    rid <- Gen.listOf(Gen.int(0, 50000))
    sellerType <- Gen.option(anySellerType)
    isSold <- Gen.option(Gen.boolean)
    pastOfferSection <- Gen.option(anySection)
    onlyLastSeller <- Gen.option(Gen.boolean)
    lastEventTypes <- Gen.listOf(anyEventType)
    vin <- Gen.option(anyVinCode)
  } yield new Filter(
    catalogFilter,
    yearFrom,
    yearTo,
    priceFrom = priceFrom,
    priceTo = priceTo,
    kmAgeFrom = kmAgeFrom,
    kmAgeTo = kmAgeTo,
    creationDateFrom = creationDateFrom.map(_.toEpochMilli),
    creationDateTo = creationDateTo.map(_.toEpochMilli),
    rid = rid,
    sellerType = sellerType,
    isSold = isSold,
    pastOfferSection = pastOfferSection,
    onlyLastSeller = onlyLastSeller,
    lastEventTypes = lastEventTypes,
    vin = vin
  )
}
