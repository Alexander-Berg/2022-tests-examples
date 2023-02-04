package ru.yandex.realty.searcher.site.grouping

import java.lang.{Float => JFloat}

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.newbuilding.gen.NewbuildingOfferGenerators
import ru.yandex.realty.search.common.request.domain.SearchSortOrder
import ru.yandex.realty.sites.grouping.ApartmentKey

@RunWith(classOf[JUnitRunner])
class ApartmentKeyComparatorSpec extends SpecBase with PropertyChecks with NewbuildingOfferGenerators {

  val apartmentKeyComparatorGen: Gen[SearchSortOrder] = Gen.oneOf(SearchSortOrder.values())
  val apartmentKeyComparatorsGen: Gen[Seq[SearchSortOrder]] = Gen.listOf(apartmentKeyComparatorGen)

  val apartmentKeyGen: Gen[ApartmentKey] = for {
    commissioningDate <- commissioningDateGen
    rooms <- roomsOptionGen

    areaValue: JFloat <- areaValueGen
    area <- Gen.option(areaValue)

    kitchenSpaceValue <- kitchenSpaceGen(areaValue)
    kitchenSpace <- Gen.option(kitchenSpaceValue)

    livingSpace <- livingSpaceOptionGen(area, kitchenSpace)

    price <- priceGen
    priceSqM = priceSqMGen(area, price)
    isFinished = isFinishedGen(commissioningDate)

    bathroomUnit <- bathroomUnitOptionGen
    balcony <- balconyOptionGen
  } yield new ApartmentKey(
    commissioningDate.orNull,
    rooms.orNull,
    price.orNull,
    priceSqM.orNull,
    area.orNull,
    livingSpace.orNull,
    kitchenSpace.orNull,
    isFinished.orNull,
    bathroomUnit.orNull,
    balcony.orNull
  )

  val listOfApartmentKeysGen: Gen[Seq[ApartmentKey]] = Gen.listOf(apartmentKeyGen)

  "ApartmentKeyComparator" when {
    "handling all SearchSortOrder values" should {
      "work without producing NPE" in {

        forAll(apartmentKeyComparatorsGen, listOfApartmentKeysGen) { (apartmentKeyComparators, listOfApartmentKeys) =>
          for (apartmentKeyComparator <- apartmentKeyComparators) {
            val ordering: Ordering[ApartmentKey] =
              Ordering.comparatorToOrdering(ApartmentKeyComparator.get(apartmentKeyComparator))
            val ordered = listOfApartmentKeys.sorted(ordering)

            ordered.size shouldBe listOfApartmentKeys.size
            ordered.toSet shouldBe listOfApartmentKeys.toSet
          }
        }
      }
    }
  }

}
