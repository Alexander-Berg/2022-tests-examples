package ru.auto.salesman.tasks.user

import ru.auto.salesman.service.user.PeriodicalDiscountService
import ru.auto.salesman.test.BaseSpec

class PeriodicalDiscountUserExclusionTaskSpec extends BaseSpec {

  private val periodicalDiscountService = mock[PeriodicalDiscountService]

  private val periodicalDiscountUserExclusionTask =
    new PeriodicalDiscountUserExclusionTask(periodicalDiscountService)

  "PeriodicalDiscountUserExclusionTask" should {

    "save exclusions" in {
      (periodicalDiscountService.saveExcludedUsers _)
        .expects()
        .returningZ(())

      periodicalDiscountUserExclusionTask.task.success.value
    }

  }

}
