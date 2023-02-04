package ru.auto.salesman.service.impl.payment_model

import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.ApiOfferModel.Section._
import ru.auto.salesman.service.DealerFeatureService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.GeoUtils._

class PaymentModelCheckerImplSpec extends BaseSpec {
  private val dealerFeatureService = mock[DealerFeatureService]

  private val featureCarsUsedRegions =
    (dealerFeatureService.carsUsedCallsRegions _)
      .expects()

  val checker = new PaymentModelCheckerImpl(dealerFeatureService)

  "singleWithCallsEnabledInRegion" should {
    "return TRUE" in {
      featureCarsUsedRegions.returning {
        Set(RegMoscow, RegSPb)
      }

      val result = checker.singleWithCallsEnabledInRegion(RegMoscow).success.value
      result shouldBe true
    }

    "return FALSE" in {
      featureCarsUsedRegions.returning {
        Set(RegMoscow, RegSPb)
      }

      val result = checker.singleWithCallsEnabledInRegion(RegKaluga).success.value
      result shouldBe false
    }
  }

  "singleWithCallsEnabled" should {
    "return TRUE for cars:used" in {
      featureCarsUsedRegions.returning {
        Set(RegMoscow, RegSPb)
      }

      val result = checker.singleWithCallsEnabled(CARS, USED, RegMoscow).success.value
      result shouldBe true
    }

    "return FALSE for wrong section" in {
      featureCarsUsedRegions.returning {
        Set(RegMoscow, RegSPb)
      }

      val result = checker.singleWithCallsEnabled(CARS, NEW, RegMoscow).success.value
      result shouldBe false
    }

    "return FALSE for wrong category" in {
      featureCarsUsedRegions.returning {
        Set(RegMoscow, RegSPb)
      }

      val result = checker.singleWithCallsEnabled(MOTO, USED, RegMoscow).success.value
      result shouldBe false
    }

    "return FALSE for wrong region" in {
      featureCarsUsedRegions.returning {
        Set(RegMoscow, RegSPb)
      }

      val result = checker.singleWithCallsEnabled(CARS, USED, RegKaluga).success.value
      result shouldBe false
    }
  }

}
