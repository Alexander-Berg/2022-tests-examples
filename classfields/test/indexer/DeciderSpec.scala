package ru.yandex.vertis.billing.indexer

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.indexer.status.DeciderImpl
import ru.yandex.vertis.billing.model_core.InactiveReasons._
import ru.yandex.vertis.billing.model_core.OfferBilling.{Active, Inactive}
import ru.yandex.vertis.billing.model_core.{Business, InactiveReasons, OfflineBizRef}
import ru.yandex.vertis.billing.status.DefaultActiveDeadlineResolver.activeHours
import ru.yandex.vertis.billing.status.DefaultActiveDeadlineResolver
import ru.yandex.vertis.billing.util.DateTimeUtils.{endOfDay, now}

/**
  * Specs on [[ru.yandex.vertis.billing.indexer.status.Decider]].
  *
  * @author alesavin
  */
class DeciderSpec extends AnyWordSpec with Matchers {

  val decider =
    new DeciderImpl(paidOffers, limitHelper, DefaultActiveDeadlineResolver)

  "Decider" should {
    "return correct status for mismatch partner id" in {
      decider.getStatus(offer1.copy(partnerId = "___"), EnabledCampaign) match {
        case Inactive(InactiveReasons.MismatchPartnerId) =>
        case other => fail(s"Unexpected status $other")
      }
    }

    "return correct status for enabled/disabled" in {
      decider.getStatus(offer1, EnabledCampaign) match {
        case Active(time, _) =>
          val currentTime = now()
          assert(
            time.isAfter(currentTime) &&
              time.isBefore(currentTime.plusHours(activeHours).plus(1))
          )
        case other => fail(s"Unexpected status $other")
      }
      decider.getStatus(offer1, DisabledCampaign) match {
        case Inactive(InactiveReasons.ManuallyDisabledCampaign) =>
        case other => fail(s"Unexpected status $other")
      }
    }

    "return correct status for campaign without funds" in {
      decider.getStatus(offer1, NoFundsPerDayCampaign) match {
        case Inactive(_) =>
        case other => fail(s"Unexpected status $other")
      }
      decider.getStatus(offer2, NoFundsPerDayCampaign) match {
        case Inactive(_) =>
        case other => fail(s"Unexpected status $other")
      }
      decider.getStatus(offer1, NoFundsPerIndexingCampaign) match {
        case Inactive(_) =>
        case other => fail(s"Unexpected status $other")
      }
      decider.getStatus(offer2, NoFundsPerIndexingCampaign) match {
        case Inactive(_) =>
        case other => fail(s"Unexpected status $other")
      }
    }

    "return correct status related to daily weekly spending" in {
      decider.getStatus(offer3, CampaignWithDailyLimitOverdrafted) match {
        case Inactive(InactiveReasons.DailyLimitExceeded) =>
        case other => fail(s"Unexpected status $other")
      }

      decider.getStatus(offer3, CampaignWithWeeklyLimitOverdrafted) match {
        case Inactive(InactiveReasons.WeeklyLimitExceeded) =>
        case other => fail(s"Unexpected status $other")
      }
      decider.getStatus(offer3, CampaignWithLimitNotOverdrafted) match {
        case Active(time, _) =>
          val currentTime = now()
          assert(
            time.isAfter(currentTime) &&
              time.isBefore(currentTime.plusHours(activeHours).plus(1))
          )
        case other => fail(s"Unexpected status $other")
      }
    }
    "return disabled status in the case of lack of funds" in {
      decider.getStatus(offer1, NotEnoughFundsCampaign) match {
        case Inactive(InactiveReasons.NoEnoughFunds) =>
        case other => fail(s"Unexpected status $other")
      }
    }

    "return active status in the case enough funds" in {
      decider.getStatus(offer1, EnoughFundsCampaign) match {
        case Active(time, _) =>
          val currentTime = now()
          val before = currentTime.plusHours(activeHours + 1)
          assert(time.isAfter(currentTime) && time.isBefore(before))
        case other => fail(s"Unexpected status $other")
      }
    }

    "return active status for non-partner based offers" in {
      decider.getStatus(
        Business("1"),
        EnabledCampaign.copy(customer = EnabledCampaign.customer.copy(resources = Seq(OfflineBizRef)))
      ) match {
        case Active(time, None) =>
        case other => fail(s"Unexpected status $other")
      }
    }
    "weekly limit exceeded when weekly overdraft more than daily" in {
      decider.getStatus(offer3, CampaignWithWeeklyHighOverdraft) match {
        case Inactive(WeeklyLimitExceeded) =>
        case other => fail(s"Unexpected status $other")
      }
    }

    "daily limit exceeded when daily overdraft more than weekly" in {
      decider.getStatus(offer3, CampaignWithDailyHighOverdraft) match {
        case Inactive(DailyLimitExceeded) =>
        case other => fail(s"Unexpected status $other")
      }
    }

  }
}
