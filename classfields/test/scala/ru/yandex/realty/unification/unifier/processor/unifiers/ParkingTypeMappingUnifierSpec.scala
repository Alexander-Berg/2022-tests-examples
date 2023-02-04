package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.features.{Features, SimpleFeatures}
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.offer.{BuildingInfo, CategoryType, OfferType, ParkingType}
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class ParkingTypeMappingUnifierSpec extends AsyncSpecBase with Matchers with PropertyChecks {

  implicit val trace: Traced = Traced.empty

  private val ParkingTypeMigrationUnifier: ParkingTypeMappingUnifier = new ParkingTypeMappingUnifier

  "ParkingTypeMappingUnifier" should {
    "correctly migrate parkingType for apartment and rooms" in {
      for (oType <- OfferType.values()) {
        for (category <- Set(CategoryType.APARTMENT, CategoryType.ROOMS)) {
          expectParkingType(oType, category, ParkingType.SECURE, ParkingType.CLOSED)
          expectParkingType(oType, category, ParkingType.SEPARATE, ParkingType.CLOSED)
          expectParkingType(oType, category, ParkingType.NEARBY, ParkingType.OPEN)
          expectParkingType(oType, category, ParkingType.GROUND, ParkingType.CLOSED)
        }
      }
    }

    "not migrate parkingType for garages" in {
      for {
        oType <- OfferType.values()
        parkingType <- ParkingType.values()
      } expectParkingType(oType, CategoryType.GARAGE, parkingType, parkingType)
    }

    "not fail for empty parkingType" in {
      forAll(OfferModelGenerators.offerGen()) { offer =>
        offer.setOfferType(OfferType.SELL)
        offer.setCategoryType(CategoryType.APARTMENT)

        val offerWrapper = new OfferWrapper(new RawOfferImpl, offer, OfferHistory.justArrived)

        ParkingTypeMigrationUnifier.unify(offerWrapper).futureValue
      }
    }
  }

  private def expectParkingType(
    offerType: OfferType,
    category: CategoryType,
    parkingType: ParkingType,
    expectedParkingType: ParkingType
  ): Unit = {

    forAll(OfferModelGenerators.offerGen()) { offer =>
      offer.setOfferType(offerType)
      offer.setCategoryType(category)

      val buildingInfo = new BuildingInfo
      buildingInfo.setParkingType(parkingType)
      offer.setBuildingInfo(buildingInfo)

      val offerWrapper = new OfferWrapper(new RawOfferImpl, offer, OfferHistory.justArrived)

      ParkingTypeMigrationUnifier.unify(offerWrapper).futureValue

      offer.getBuildingInfo.getParkingType shouldEqual expectedParkingType
    }
  }
}
