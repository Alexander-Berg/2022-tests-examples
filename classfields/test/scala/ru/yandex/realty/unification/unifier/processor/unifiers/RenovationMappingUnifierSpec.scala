package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.offer._
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper
import ru.yandex.realty.unification.unifier.processor.services.RenovationProvider

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class RenovationMappingUnifierSpec extends AsyncSpecBase with Matchers with PropertyChecks {

  import RenovationMappingUnifierSpec._

  implicit val trace: Traced = Traced.empty

  val RenovationMigrationUnifier = new RenovationMappingUnifier(new RenovationProvider)

  "RenovationMappingUnifier" should {
    "correctly migrate renovation for apartments and rooms" in {

      for (oType <- OfferType.values()) {
        for (category <- Set(CategoryType.APARTMENT, CategoryType.ROOMS)) {
          expectRenovation(oType, category, Renovation.NORMAL, Renovation.COSMETIC_DONE)
          expectRenovation(oType, category, Renovation.PARTIAL_RENOVATION, Renovation.NEEDS_RENOVATION)
          expectRenovation(oType, category, Renovation.PRIME_RENOVATION, Renovation.NEEDS_RENOVATION)
          expectRenovation(oType, category, Renovation.RENOVATED, Renovation.COSMETIC_DONE)
          expectRenovation(oType, category, Renovation.COSMETIC_REQUIRED, Renovation.NEEDS_RENOVATION)
          expectRenovation(oType, category, Renovation.GOOD, Renovation.COSMETIC_DONE)
          expectRenovation(oType, category, Renovation.CLEAN, Renovation.NEEDS_RENOVATION)
          expectRenovation(oType, category, Renovation.BEFORE_CLEAN, Renovation.NEEDS_RENOVATION)
          expectRenovation(oType, category, Renovation.TURNKEY, Renovation.COSMETIC_DONE)
        }
      }
    }

    "not migrate renovation for newFlat apartments" in {
      for {
        oType <- OfferType.values()
        renovation <- GoodRenovationValues
      } expectRenovation(oType, CategoryType.APARTMENT, renovation, renovation, newFlat = true)
    }

    "correctly migrate renovation for commercial" in {
      val category = CategoryType.COMMERCIAL
      val commercialTypes =
        Set(CommercialType.OFFICE, CommercialType.RETAIL, CommercialType.MANUFACTURING, CommercialType.FREE_PURPOSE)
      for (oType <- OfferType.values()) {
        for (cType <- commercialTypes) {
          expectRenovation(oType, category, Renovation.NORMAL, Renovation.COSMETIC_DONE, commercialType = Some(cType))
          expectRenovation(
            oType,
            category,
            Renovation.PARTIAL_RENOVATION,
            Renovation.NEEDS_RENOVATION,
            commercialType = Some(cType)
          )
          expectRenovation(
            oType,
            category,
            Renovation.PRIME_RENOVATION,
            Renovation.NEEDS_RENOVATION,
            commercialType = Some(cType)
          )
          expectRenovation(
            oType,
            category,
            Renovation.RENOVATED,
            Renovation.COSMETIC_DONE,
            commercialType = Some(cType)
          )
          expectRenovation(
            oType,
            category,
            Renovation.COSMETIC_REQUIRED,
            Renovation.NEEDS_RENOVATION,
            commercialType = Some(cType)
          )
          expectRenovation(oType, category, Renovation.GOOD, Renovation.COSMETIC_DONE, commercialType = Some(cType))
          expectRenovation(oType, category, Renovation.CLEAN, Renovation.NEEDS_RENOVATION, commercialType = Some(cType))
          expectRenovation(
            oType,
            category,
            Renovation.BEFORE_CLEAN,
            Renovation.NEEDS_RENOVATION,
            commercialType = Some(cType)
          )
          expectRenovation(oType, category, Renovation.TURNKEY, Renovation.COSMETIC_DONE, commercialType = Some(cType))
        }
      }
    }

    "not migrate renovation for house, lot, garage" in {
      for {
        oType <- OfferType.values()
        category <- Set(CategoryType.HOUSE, CategoryType.LOT, CategoryType.GARAGE)
        renovation <- GoodRenovationValues
      } expectRenovation(oType, category, renovation, renovation)
    }

    "map renovation status for primary sale offers" in {
      for (oType <- OfferType.values()) {
        expectRenovation(
          oType,
          CategoryType.APARTMENT,
          Renovation.NEEDS_RENOVATION,
          Renovation.PRIME_RENOVATION,
          isPrimarySale = true
        )
        expectRenovation(
          oType,
          CategoryType.APARTMENT,
          Renovation.PARTIAL_RENOVATION,
          Renovation.PRIME_RENOVATION,
          isPrimarySale = true
        )
        expectRenovation(oType, CategoryType.APARTMENT, Renovation.RENOVATED, Renovation.CLEAN, isPrimarySale = true)
        expectRenovation(oType, CategoryType.APARTMENT, Renovation.BEFORE_CLEAN, Renovation.CLEAN, isPrimarySale = true)
        expectRenovation(
          oType,
          CategoryType.APARTMENT,
          Renovation.COSMETIC_DONE,
          Renovation.CLEAN,
          isPrimarySale = true
        )
        expectRenovation(
          oType,
          CategoryType.APARTMENT,
          Renovation.COSMETIC_REQUIRED,
          Renovation.CLEAN,
          isPrimarySale = true
        )
        expectRenovation(oType, CategoryType.APARTMENT, Renovation.GOOD, Renovation.TURNKEY, isPrimarySale = true)
        expectRenovation(oType, CategoryType.APARTMENT, Renovation.EURO, Renovation.TURNKEY, isPrimarySale = true)
        expectRenovation(
          oType,
          CategoryType.APARTMENT,
          Renovation.DESIGNER_RENOVATION,
          Renovation.TURNKEY,
          isPrimarySale = true
        )
      }
    }
  }

  private def expectRenovation(
    offerType: OfferType,
    category: CategoryType,
    renovation: Renovation,
    expectedRenovation: Renovation,
    newFlat: Boolean = false,
    isPrimarySale: Boolean = false,
    commercialType: Option[CommercialType] = None
  ): Unit = {

    forAll(OfferModelGenerators.offerGen()) { offer =>
      offer.setOfferType(offerType)
      offer.setCategoryType(category)

      val apartmentInfo = new ApartmentInfo
      apartmentInfo.setNewFlat(newFlat)
      apartmentInfo.setRenovation(renovation)
      offer.setApartmentInfo(apartmentInfo)

      commercialType.foreach { cType =>
        val commercialInfo = new CommercialInfo
        offer.setCommercialInfo(commercialInfo)
        commercialInfo.setCommercialType(cType)
      }

      offer.setPrimarySaleV2(isPrimarySale)

      val offerWrapper = new OfferWrapper(new RawOfferImpl, offer, OfferHistory.justArrived)

      RenovationMigrationUnifier.unify(offerWrapper).futureValue

      offer.getApartmentInfo.getRenovation shouldEqual expectedRenovation
    }
  }
}

object RenovationMappingUnifierSpec {

  val GoodRenovationValues =
    Renovation.values().filter(r => r != Renovation.GRANDMOTHER && r != Renovation.NON_GRANDMOTHER)
}
