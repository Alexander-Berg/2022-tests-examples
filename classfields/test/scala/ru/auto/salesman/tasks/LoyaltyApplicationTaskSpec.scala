package ru.auto.salesman.tasks

import ru.auto.salesman.service.LoyaltyReportService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.feature.TestDealerFeatureService

class LoyaltyApplicationTaskSpec extends BaseSpec {
  private val service = mock[LoyaltyReportService]

  private val featureService =
    TestDealerFeatureService(loyaltyApplicationEnabled = true)
  private val task = new LoyaltyApplicationTask(service, featureService)

  "LoyaltyApplicationTask" should {
    "apply loyalty resolution" in {
      (service.applyPositiveLoyalty _).expects().once().returningZ(unit)
      (service.applyNegativeLoyalty _).expects().once().returningZ(unit)

      task.task.success.value
    }
  }
}
