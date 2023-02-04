package ru.auto.salesman.test.service.payment_model

import ru.auto.api.ApiOfferModel
import ru.auto.salesman.model.RegionId
import ru.auto.salesman.model.payment_model.PaymentModelChecker
import zio.{Task, ZIO}

class TestRegionMockedPaymentModelChecker(regionIds: Set[RegionId])
    extends PaymentModelChecker {

  override def singleWithCallsEnabled(
      category: ApiOfferModel.Category,
      section: ApiOfferModel.Section,
      regionId: RegionId
  ): Task[Boolean] =
    if (regionIds.contains(regionId))
      ZIO.succeed(true)
    else ZIO.succeed(false)

  override def singleWithCallsEnabledInRegion(
      regionId: RegionId
  ): Task[Boolean] =
    if (regionIds.contains(regionId))
      ZIO.succeed(true)
    else ZIO.succeed(false)
}
