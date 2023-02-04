package ru.yandex.vertis.billing.indexer

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.InactiveReasons._
import ru.yandex.vertis.billing.util.DateTimeUtils.now

/**
  * Specs for [[ru.yandex.vertis.billing.status.LimitHelper]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class LimitHelperSpec extends AnyWordSpec with Matchers {

  "Limit checker" should {
    val time = now()

    "correctly process daily weekly spending" in {
      limitHelper.safeIsLimitExceeded(CampaignWithDailyLimitOverdrafted, time) match {
        case Some(DailyLimitExceeded) =>
        case other => fail(s"Unpredicted $other")
      }

      limitHelper.safeIsLimitExceeded(CampaignWithWeeklyLimitOverdrafted, time) match {
        case Some(WeeklyLimitExceeded) =>
        case other => fail(s"Unpredicted $other")
      }

      limitHelper.safeIsLimitExceeded(CampaignWithLimitNotOverdrafted, time) match {
        case None =>
        case other => fail(s"Unpredicted $other")
      }
    }

    "correctly filter campaigns with no limit exceeded" in {
      val source =
        Iterable(CampaignWithDailyLimitOverdrafted, CampaignWithWeeklyLimitOverdrafted, CampaignWithLimitNotOverdrafted)

      val filtered = limitHelper.filterNoLimitExceeded(source, time)

      filtered.size should be(1)
      filtered.head should be(CampaignWithLimitNotOverdrafted)
    }

    "weekly limit exceeded when weekly overdraft more than daily" in {
      limitHelper.safeIsLimitExceeded(CampaignWithWeeklyHighOverdraft, time) match {
        case Some(WeeklyLimitExceeded) =>
        case other => fail(s"Unexpected status $other")
      }
    }

    "daily limit exceeded when daily overdraft more than weekly" in {
      limitHelper.safeIsLimitExceeded(CampaignWithDailyHighOverdraft, time) match {
        case Some(DailyLimitExceeded) =>
        case other => fail(s"Unexpected status $other")
      }
    }

  }

}
