package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Model

/**
  * Specs on [[InactiveReasons]]
  *
  * @author alesavin
  */
class InactiveReasonsSpec extends AnyWordSpec with Matchers {

  val reverseMap = Map(
    Model.InactiveReason.UNKNOWN_INACTIVE_REASON ->
      InactiveReasons.Unknown,
    Model.InactiveReason.MANUALLY_DISABLED ->
      InactiveReasons.ManuallyDisabledCampaign,
    Model.InactiveReason.NO_ENOUGH_FUNDS ->
      InactiveReasons.NoEnoughFunds,
    Model.InactiveReason.DAILY_LIMIT_EXCEEDED ->
      InactiveReasons.DailyLimitExceeded,
    Model.InactiveReason.MISMATCH_PARTNER_ID ->
      InactiveReasons.MismatchPartnerId,
    Model.InactiveReason.WEEKLY_LIMIT_EXCEEDED ->
      InactiveReasons.WeeklyLimitExceeded,
    Model.InactiveReason.NOT_ACTIVATED ->
      InactiveReasons.NotActivated,
    Model.InactiveReason.DEPOSIT_LIMIT_EXCEEDED ->
      InactiveReasons.DepositLimitExceeded,
    Model.InactiveReason.MONTHLY_LIMIT_EXCEEDED ->
      InactiveReasons.MonthlyLimitExceeded
  )

  "InactiveReasons descriptions" should {
    "be equal on model and proto" in {
      InactiveReasons.values.foreach { v =>
        val proto = Model.InactiveReason.forNumber(v.id)
        reverseMap(proto) should be(v)
      }
    }
  }
}
