package ru.yandex.vertis.billing.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.LimitSetting.{Daily, Monthly, Weekly}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{CampaignHeaderGen, OrderGen, Producer}
import ru.yandex.vertis.billing.util.CampaignFundUtils.{available, balanceExceeded, depositExceeded, Available}
import ru.yandex.vertis.billing.util.CampaignFundUtilsSpec.withPositiveBalance
import ru.yandex.vertis.billing.util.DateTimeUtils.now

/**
  * Spec on [[CampaignFundUtils]]
  *
  * @author ruslansd
  */
class CampaignFundUtilsSpec extends AnyWordSpec with Matchers {

  val product = Product(Placement(CostPerClick(1000L)))
  val CPA = Product(Placement(CostPerAction))

  val positive = withPositiveBalance(10000, 0)
  val zeroBalance = withPositiveBalance(1000, 1000)
  val notEnough = withPositiveBalance(100, 0)

  val noLimit = CurrentLimit.Empty
  val limitDaily = Daily(10L, now())
  val limitWeekly = Weekly(100L, now())
  val limitMonthly = Monthly(1000L, now())
  val limit = CurrentLimit(Some(limitDaily), Some(limitWeekly), Some(limitMonthly))

  val noSpent = Spendings(None, None, None)
  val spentDaily = Spent.Daily(10L)
  val spentWeekly = Spent.Weekly(30L)
  val spentMonthly = Spent.Monthly(100L)
  val spent = Spendings(Some(spentDaily), Some(spentWeekly), Some(spentMonthly))

  def withDeposit(deposit: Deposit): CampaignHeader = {
    val header = CampaignHeaderGen.next
    val settings = header.settings.copy(deposit = Some(deposit))
    header.copy(settings = settings)
  }

  def withoutDeposit: CampaignHeader = {
    val header = CampaignHeaderGen.next
    val settings = header.settings.copy(deposit = None)
    header.copy(settings = settings)
  }

  "Campaign funds util" should {

    "active campaign if balance enough" in {
      val enoughCampaign = CampaignHeaderGen.next.copy(order = positive, product = product)

      balanceExceeded(enoughCampaign) shouldBe false
    }

    "inactive campaign if balance exceeded" in {
      val exceededCampaign = CampaignHeaderGen.next.copy(order = zeroBalance, product = product)

      balanceExceeded(exceededCampaign) shouldBe true
    }

    "inactive campaign if balance not enough" in {
      val notEnoughCampaign = CampaignHeaderGen.next.copy(order = notEnough, product = product)

      balanceExceeded(notEnoughCampaign) shouldBe true
    }

    "active campaign if CPA" in {
      val CPACampaign = CampaignHeaderGen.next.copy(order = zeroBalance, product = CPA)

      balanceExceeded(CPACampaign) shouldBe false
    }

    "deposit limit not exceeded" in {
      val notExceededCampaign = withDeposit(CoefficientDeposit(5))
        .copy(order = positive, product = product)
      depositExceeded(notExceededCampaign) shouldBe false
    }

    "deposit not exceeded" in {
      val exceededCampaign = withDeposit(CoefficientDeposit(15))
        .copy(order = positive, product = product)
      depositExceeded(exceededCampaign) shouldBe true
    }

    "without deposit" in {
      val noDeposit = withoutDeposit.copy(order = positive, product = product)
      depositExceeded(noDeposit) shouldBe false
    }

    "fix amount deposit exceeded" in {
      val exceededCampaign = withDeposit(FixAmountDeposit(positive.balance2.current + 100))
        .copy(order = positive, product = product)
      depositExceeded(exceededCampaign) shouldBe true
    }

    "fix amount deposit not exceeded" in {
      val notExceededCampaign = withDeposit(FixAmountDeposit(positive.balance2.current))
        .copy(order = positive, product = product)
      depositExceeded(notExceededCampaign) shouldBe false
    }

    "empty if limit is empty" in {
      val spent = Spendings(Some(Spent.Daily(100L)), Some(Spent.Weekly(400L)), Some(Spent.Monthly(400L)))
      available(spent, noLimit) shouldBe Available.Empty
    }

    "active if spent is empty" in {
      val min = math.min(math.min(limit.daily.get.funds, limit.weekly.get.funds), limit.monthly.get.funds)
      available(noSpent, limit) shouldBe Available.Active(min)
    }

    "provide active status of campaign with daily limits" in {
      val dailyLimit = CurrentLimit(daily = Some(limitDaily.copy(100L)))
      val dailySpentActive = Spendings(Some(spentDaily.copy(50L)), None, None)
      available(dailySpentActive, dailyLimit) shouldBe Available.Active(50L)
    }

    "provide active status of campaign with weekly limits" in {
      val weeklyLimit = CurrentLimit(weekly = Some(limitWeekly.copy(100L)))
      val weeklySpent = Spendings(None, Some(spentWeekly.copy(50L)), None)
      available(weeklySpent, weeklyLimit) shouldBe Available.Active(50L)
    }

    "provide active status of campaign with monthly limits" in {
      val monthlyLimit = CurrentLimit(monthly = Some(limitMonthly.copy(100L)))
      val monthSpent = Spendings(None, None, Some(spentMonthly.copy(50L)))
      available(monthSpent, monthlyLimit) shouldBe Available.Active(50L)
    }

    "provide active status of campaign with all limits" in {
      val limit = CurrentLimit(
        Some(limitDaily.copy(50L)),
        Some(limitWeekly.copy(100L)),
        Some(limitMonthly.copy(1000L))
      )
      val spent = Spendings(
        Some(spentDaily.copy(20L)),
        Some(spentWeekly.copy(50L)),
        Some(spentMonthly.copy(900L))
      )
      available(spent, limit) should be(Available.Active(30L))
    }

    "provide inactive status of campaign with daily limits" in {
      val dailyLimit = CurrentLimit(Some(limitDaily.copy(50L)), None)
      val dailySpentActive = Spendings(Some(spentDaily.copy(100L)), None, None)
      available(dailySpentActive, dailyLimit) shouldBe Available.Inactive(-50, InactiveReasons.DailyLimitExceeded)
    }

    "provide inactive status of campaign with weekly limits" in {
      val weeklyLimit = CurrentLimit(None, Some(limitWeekly.copy(50)))
      val weeklySpent = Spendings(None, Some(spentWeekly.copy(100L)), None)
      available(weeklySpent, weeklyLimit) shouldBe Available.Inactive(-50, InactiveReasons.WeeklyLimitExceeded)
    }

    "provide inactive status of campaign with monthly limits" in {
      val monthlyLimit = CurrentLimit(monthly = Some(limitMonthly.copy(50L)))
      val monthSpent = Spendings(None, None, Some(spentMonthly.copy(100L)))
      available(monthSpent, monthlyLimit) shouldBe Available.Inactive(-50, InactiveReasons.MonthlyLimitExceeded)
    }

    "provide inactive status of campaign with all limits" in {
      val limit = CurrentLimit(
        Some(limitDaily.copy(20L)),
        Some(limitWeekly.copy(50L)),
        Some(limitMonthly.copy(900L))
      )
      val spent = Spendings(
        Some(spentDaily.copy(50L)),
        Some(spentWeekly.copy(100L)),
        Some(spentMonthly.copy(1000L))
      )
      available(spent, limit) shouldBe Available.Inactive(-100, InactiveReasons.MonthlyLimitExceeded)
    }

    "provide inactive status of campaign with same overdrafts" in {
      val limit = CurrentLimit(
        Some(limitDaily.copy(50L)),
        Some(limitWeekly.copy(100L)),
        Some(limitMonthly.copy(1000L))
      )
      val spent = Spendings(
        Some(spentDaily.copy(50L)),
        Some(spentWeekly.copy(100L)),
        Some(spentMonthly.copy(1000L))
      )
      available(spent, limit) shouldBe Available.Inactive(0, InactiveReasons.WeeklyLimitExceeded)
    }

    "provide active status of campaign with independent spents" in {
      val dailyLimit = CurrentLimit(daily = Some(limitDaily.copy(100L)))
      val withoutDailySpent = Spendings(
        None,
        Some(spentWeekly.copy(10000L)),
        Some(spentMonthly.copy(1000L))
      )
      available(withoutDailySpent, dailyLimit) shouldBe Available.Active(100L)

      val weeklyLimit = CurrentLimit(weekly = Some(limitWeekly.copy(100L)))
      val withoutWeeklySpent = Spendings(
        Some(spentDaily.copy(10000L)),
        None,
        Some(spentMonthly.copy(1000L))
      )
      available(withoutWeeklySpent, weeklyLimit) shouldBe Available.Active(100L)

      val monthlyLimit = CurrentLimit(monthly = Some(limitMonthly.copy(100L)))
      val withoutMonthlySpent = Spendings(
        Some(spentDaily.copy(1000L)),
        Some(spentWeekly.copy(10000L)),
        None
      )
      available(withoutMonthlySpent, monthlyLimit) shouldBe Available.Active(100L)
    }

  }
}

object CampaignFundUtilsSpec {

  def withPositiveBalance(income: Funds, spent: Funds): Order =
    OrderGen.next.copy(balance2 = OrderBalance2(income, spent))
}
