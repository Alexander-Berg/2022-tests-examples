package ru.yandex.realty.unification.unifier.processor.unifiers

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.gen.OfferModelGenerators
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.offer.{ApartmentInfo, CategoryType, OfferType}
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class OpenPlanToStudioMappingUnifierSpec extends AsyncSpecBase with Matchers with PropertyChecks {

  implicit val trace: Traced = Traced.empty
  val OpenPlanToStudioMigrationUnifier: OpenPlanToStudioMappingUnifier = new OpenPlanToStudioMappingUnifier

  "OpenPlanToStudioMappingUnifier" should {

    "migrate open plan to studio for apartment" in {
      for (oType <- OfferType.values()) {
        expectMapOpenPlanToStudio(oType, CategoryType.APARTMENT, expectedOpenPlan = None, expectedStudio = Some(true))
      }
    }

    "not migrate open plan to studio for rooms, house, lot, garage" in {
      for {
        oType <- OfferType.values()
        category <- CategoryType.values()
        if category != CategoryType.APARTMENT
      } expectMapOpenPlanToStudio(oType, category, expectedOpenPlan = Some(true), expectedStudio = None)
    }
  }

  private def expectMapOpenPlanToStudio(
    offerType: OfferType,
    category: CategoryType,
    expectedOpenPlan: Option[Boolean],
    expectedStudio: Option[Boolean]
  ): Unit = {
    forAll(OfferModelGenerators.offerGen()) { offer =>
      offer.setOfferType(offerType)
      offer.setCategoryType(category)

      val apartmentInfo = new ApartmentInfo
      apartmentInfo.setOpenPlan(true)
      apartmentInfo.setStudio(None.orNull)
      offer.setApartmentInfo(apartmentInfo)

      val offerWrapper = new OfferWrapper(new RawOfferImpl, offer, OfferHistory.justArrived)

      OpenPlanToStudioMigrationUnifier.unify(offerWrapper).futureValue

      Option(offer.getApartmentInfo.isOpenPlan).map(_.booleanValue()) shouldEqual expectedOpenPlan
      Option(offer.getApartmentInfo.getStudio).map(_.booleanValue()) shouldEqual expectedStudio
    }
  }
}
