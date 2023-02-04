package ru.auto.salesman.test.service.payment_model

import ru.auto.salesman.model.RegionId
import ru.auto.salesman.model.payment_model.PaymentModelFactory

object TestPaymentModelFactory {

  def withoutSingleWithCalls() = new PaymentModelFactory(
    new TestAlwaysFalsePaymentModelChecker()
  )

  def withSingleWithCalls() = new PaymentModelFactory(
    new TestAlwaysTruePaymentModelChecker()
  )

  def withMockRegions(regionIds: Set[RegionId]) = new PaymentModelFactory(
    new TestRegionMockedPaymentModelChecker(regionIds)
  )

}
