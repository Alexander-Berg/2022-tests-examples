package ru.yandex.vertis.billing.service.impl

/**
  *
  * @author Anton Volokhov @literal{<logab@yandex-team.ru <mailto:logab@yandex-team.ru>>}
  * @author ruslansd
  */
import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.{BindingDao, CampaignCallDao, CampaignDao, OrderDao, TransactionContext}
import ru.yandex.vertis.billing.model_core.CampaignStatus.{Active, Inactive}
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.InactiveReasons._
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{costGen, CampaignHeaderGen, OrderGen, Producer}
import ru.yandex.vertis.billing.service.impl.CampaignServiceWithStatusAndLimitsSpec._
import ru.yandex.vertis.billing.service.{CampaignService, LimitService}
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, wholeMonth, wholeWeek}
import ru.yandex.vertis.billing.util._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.language.implicitConversions
import scala.util.Success

class CampaignServiceWithStatusAndLimitsSpec extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val operatorContext = OperatorContext("test", Uid(0L))

  val log = LoggerFactory.getLogger(classOf[CampaignServiceWithStatusAndLimitsSpec])

  private val noSpends = Spendings(Some(Spent.Daily(0)), Some(Spent.Weekly(0)), Some(Spent.Monthly(0)))

  private def spendings(daily: Option[Funds] = None, weekly: Option[Funds] = None, monthly: Option[Funds] = None) =
    Spendings(
      daily.map(Spent.Daily),
      weekly.map(Spent.Weekly),
      monthly.map(Spent.Monthly)
    )

  "Campaign " should {
    "be active when campaign Enabled" in {
      val limit = EnabledCampaign.product.totalCost
      val campaignService = mockCampaignService(
        EnabledCampaign,
        limit,
        limit,
        limit,
        noSpends
      )
      val campaign = campaignService.get(EnabledCampaign.id).get
      campaign.status shouldEqual Some(Active(noSpends))
    }

    "be ManuallyDisabled when campaign disabled" in {
      val campaignService = mockCampaignService(DisabledCampaign, 0, 0, 0, noSpends)
      val campaign = campaignService.get(DisabledCampaign.id)
      campaign.get.status shouldEqual Some(Inactive(ManuallyDisabledCampaign))
    }

    "be NoEnoughFunds when balance non positive" in {
      val campaignService = mockCampaignService(NonPositiveBalanceCampaign, 0, 0, 0, noSpends)
      val campaign = campaignService.get(NonPositiveBalanceCampaign.id)
      campaign.get.status shouldEqual Some(Inactive(NoEnoughFunds))
    }

    "be NoEnoughFunds when no enough funds" in {
      val campaignService = mockCampaignService(NotEnoughFundsCampaign, 0, 0, 0, noSpends)
      val campaign = campaignService.get(NotEnoughFundsCampaign.id)
      campaign.get.status shouldEqual Some(Inactive(NoEnoughFunds))
    }

    "be Active when enough funds" in {
      val limit = EnoughFundsCampaign.product.totalCost
      val campaignService = mockCampaignService(
        EnoughFundsCampaign,
        limit,
        limit,
        limit,
        noSpends
      )
      val campaign = campaignService.get(EnoughFundsCampaign.id).get
      campaign.status shouldEqual Some(Active(noSpends))
    }

    "be DailyLimitExceeded when daily limit exceeded" in {
      val ss = spendings(Some(CampaignWithDailyLimitOverdrafted.product.totalCost + 1))
      val campaignService = mockCampaignService(
        CampaignWithDailyLimitOverdrafted,
        CampaignWithDailyLimitOverdrafted.product.totalCost,
        CampaignWithDailyLimitOverdrafted.product.totalCost,
        CampaignWithDailyLimitOverdrafted.product.totalCost,
        ss
      )
      val campaign = campaignService.get(CampaignWithDailyLimitOverdrafted.id)
      campaign.get.status shouldEqual Some(Inactive(DailyLimitExceeded))
    }

    "be Active when daily limit not exceeded" in {
      val limit = EnoughFundsCampaign.product.totalCost
      val campaignService = mockCampaignService(
        CampaignWithLimitNotOverdrafted,
        limit,
        limit,
        limit,
        noSpends
      )
      val campaign = campaignService.get(CampaignWithLimitNotOverdrafted.id).get
      campaign.status shouldEqual Some(Active(noSpends))
    }

    "be Active with specified spendings" in {
      val campaignService = mockCampaignService(
        CampaignWithLimitNotOverdrafted,
        0L,
        0L,
        0L,
        noSpends
      )
      val campaign = campaignService.get(CampaignWithLimitNotOverdrafted.id).get
      val without = noSpends.copy(weekly = None, daily = None, monthly = None)
      campaign.status shouldEqual Some(Active(without))
    }

    "be Active if campaign is CPA even balance is 0" in {
      val campaignService = mockCampaignService(
        CPACampaign,
        CPACampaign.product.totalCost,
        CPACampaign.product.totalCost,
        CPACampaign.product.totalCost,
        noSpends
      )
      val campaign = campaignService.get(CPACampaign.id)
      val without = noSpends.copy(daily = None, weekly = None, monthly = None)
      campaign.get.status shouldEqual Some(Active(without))
    }

    "be inactive if weekly balance exceeded" in {
      val campaignService = mockCampaignService(
        CampaignWithWeeklyLimitOverdrafted,
        CampaignWithWeeklyLimitOverdrafted.product.totalCost,
        CampaignWithWeeklyLimitOverdrafted.product.totalCost,
        CampaignWithWeeklyLimitOverdrafted.product.totalCost,
        spendings(weekly = Some(CampaignWithWeeklyLimitOverdrafted.product.totalCost + 1))
      )
      val campaign = campaignService.get(CampaignWithWeeklyLimitOverdrafted.id)
      campaign.get.status shouldEqual Some(Inactive(WeeklyLimitExceeded))
    }

    "be inactive if monthly balance exceeded" in {
      val campaignService = mockCampaignService(
        CampaignWithMonthlyLimitOverdrafted,
        CampaignWithMonthlyLimitOverdrafted.product.totalCost,
        CampaignWithMonthlyLimitOverdrafted.product.totalCost,
        CampaignWithMonthlyLimitOverdrafted.product.totalCost,
        spendings(monthly = Some(CampaignWithMonthlyLimitOverdrafted.product.totalCost + 1))
      )
      val campaign = campaignService.get(CampaignWithMonthlyLimitOverdrafted.id)
      campaign.get.status shouldEqual Some(Inactive(MonthlyLimitExceeded))
    }

    "monthly limit exceeded when weekly and monthly overdrafts more than daily" in {
      val campaignService = mockCampaignService(
        CampaignWithLimitsOverdrafted,
        CampaignWithLimitsOverdrafted.product.totalCost,
        CampaignWithLimitsOverdrafted.product.totalCost,
        CampaignWithLimitsOverdrafted.product.totalCost,
        spendings(
          Some(CampaignWithLimitsOverdrafted.product.totalCost + 1),
          Some(CampaignWithLimitsOverdrafted.product.totalCost + 2),
          Some(CampaignWithLimitsOverdrafted.product.totalCost + 3)
        )
      )
      val campaign = campaignService.get(CampaignWithLimitsOverdrafted.id)
      campaign.get.status shouldEqual Some(Inactive(MonthlyLimitExceeded))
    }

    "be inactive with daily/weekly limit exceeded if product cost > balance" in {
      val campaignServiceDaily = mockCampaignService(
        CampaignWithProductCostMoreThanBalance,
        CampaignWithProductCostMoreThanBalance.product.totalCost - 1,
        CampaignWithProductCostMoreThanBalance.product.totalCost,
        CampaignWithProductCostMoreThanBalance.product.totalCost,
        noSpends
      )
      val campaignDaily = campaignServiceDaily.get(CampaignWithProductCostMoreThanBalance.id)
      campaignDaily.get.status shouldEqual Some(Inactive(DailyLimitExceeded))

      val campaignServiceWeekly = mockCampaignService(
        CampaignWithProductCostMoreThanBalance,
        CampaignWithProductCostMoreThanBalance.product.totalCost,
        CampaignWithProductCostMoreThanBalance.product.totalCost - 1,
        CampaignWithProductCostMoreThanBalance.product.totalCost,
        noSpends
      )
      val campaignWeekly = campaignServiceWeekly.get(CampaignWithProductCostMoreThanBalance.id)
      campaignWeekly.get.status shouldEqual Some(Inactive(WeeklyLimitExceeded))

      val campaignServiceMonthly = mockCampaignService(
        CampaignWithProductCostMoreThanBalance,
        CampaignWithProductCostMoreThanBalance.product.totalCost,
        CampaignWithProductCostMoreThanBalance.product.totalCost,
        CampaignWithProductCostMoreThanBalance.product.totalCost - 1,
        noSpends
      )
      val campaignMonthly = campaignServiceMonthly.get(CampaignWithProductCostMoreThanBalance.id)
      campaignMonthly.get.status shouldEqual Some(Inactive(MonthlyLimitExceeded))
    }

    "be inactive with limit exceeded if product with dynamic price with non defined cost" in {
      val campaignServiceDaily = mockCampaignService(
        CampaignWithDynamicPrice,
        100,
        0,
        100,
        spendings(
          Some(100),
          Some(100),
          Some(100)
        )
      )
      val campaignDaily = campaignServiceDaily.get(CampaignWithProductCostMoreThanBalance.id)
      campaignDaily.get.status shouldEqual Some(Inactive(DailyLimitExceeded))

      val campaignServiceWeekly = mockCampaignService(
        CampaignWithDynamicPrice,
        0,
        100,
        100,
        spendings(
          Some(100),
          Some(100),
          Some(100)
        )
      )
      val campaignWeekly = campaignServiceWeekly.get(CampaignWithProductCostMoreThanBalance.id)
      campaignWeekly.get.status shouldEqual Some(Inactive(WeeklyLimitExceeded))
    }

  }

}

object CampaignServiceWithStatusAndLimitsSpec {

  import ru.yandex.vertis.mockito.MockitoSupport.{?, mock, stub, when}

  val CurrentDay = DateTimeInterval.currentDay
  val CurrentWeek = wholeWeek(now())
  val CurrentMonth = wholeMonth(now())

  private val DefaultProduct = Product(Raising(CostPerClick(100L)))

  def withLimit(daily: Funds, weekly: Funds, monthly: Funds) =
    CurrentLimit(
      if (daily != 0) Some(LimitSetting.Daily(daily, CurrentDay.from)) else None,
      if (weekly != 0) Some(LimitSetting.Weekly(weekly, CurrentWeek.from)) else None,
      if (monthly != 0) Some(LimitSetting.Monthly(monthly, CurrentMonth.from)) else None
    )

  val EnabledSettings = CampaignSettings.Default
  val DisabledSettings = CampaignSettings.Default.copy(isEnabled = false)

  val EnabledCampaign = {
    val c = CampaignHeaderGen.next
    val header = c.copy(
      id = "enabled",
      settings = EnabledSettings,
      product = DefaultProduct,
      status = None
    )
    withPositiveBalance(header, header.product.totalCost + 1)
  }

  val DisabledCampaign = withPositiveBalance(
    CampaignHeaderGen.next
      .copy(
        id = "disabled",
        settings = DisabledSettings,
        product = DefaultProduct,
        status = None
      ),
    1000
  )

  val NotEnoughFundsCampaign = {
    val order = OrderGen.next
      .copy(
        balance2 = OrderBalance2(totalIncome = 500, totalSpent = 0)
      )
    val cost = costGen(1000).next
    val goods: Set[Good] = Set(
      Highlighting(cost),
      Raising(CostPerAction)
    )
    CampaignHeaderGen.next
      .copy(
        id = "not-enough-funds",
        order = order,
        product = Product(goods),
        settings = CampaignSettings.Default,
        status = None
      )
  }

  val EnoughFundsCampaign = {
    val order = OrderGen.next.copy(balance2 = OrderBalance2(totalIncome = 1000, totalSpent = 0))
    val cost1 = costGen(300).next
    val cost2 = costGen(700).next
    val goods: Set[Good] = Set(
      Highlighting(cost1),
      `Raise+Highlighting`(cost2),
      Raising(CostPerAction)
    )
    CampaignHeaderGen.next
      .copy(
        id = "enough-funds",
        order = order,
        product = Product(goods),
        settings = CampaignSettings.Default,
        status = None
      )
  }

  val NonPositiveBalanceCampaign = withNegativeBalance(
    CampaignHeaderGen.next
      .copy(
        id = "non-positive-balance",
        settings = EnabledSettings,
        product = Product(Raising(CostPerClick(100L))),
        status = None
      ),
    10L
  )

  val CampaignWithDailyLimitOverdrafted = {
    val header = CampaignHeaderGen.next
      .copy(
        id = "with-daily-limit-overdraft",
        settings = EnabledSettings,
        product = DefaultProduct,
        status = None
      )
    withPositiveBalance(header, header.product.totalCost + 1)
  }

  val CampaignWithWeeklyLimitOverdrafted = {
    val header = CampaignHeaderGen.next
      .copy(
        id = "with-weekly-limit-overdraft",
        settings = EnabledSettings,
        product = DefaultProduct,
        status = None
      )
    withPositiveBalance(header, header.product.totalCost + 1)
  }

  val CampaignWithMonthlyLimitOverdrafted = {
    val header = CampaignHeaderGen.next
      .copy(
        id = "with-monthly-limit-overdraft",
        settings = EnabledSettings,
        product = DefaultProduct,
        status = None
      )
    withPositiveBalance(header, header.product.totalCost + 1)
  }

  val CampaignWithLimitNotOverdrafted = {
    val header = CampaignHeaderGen.next
      .copy(
        id = "with-limit-not-overdraft",
        settings = EnabledSettings,
        product = DefaultProduct,
        status = None
      )
    withPositiveBalance(header, header.product.totalCost + 1)
  }

  val CPACampaign = withPositiveBalance(
    CampaignHeaderGen.next
      .copy(id = "CPA-campaign", settings = EnabledSettings, product = Product(Raising(CostPerAction)), status = None),
    0
  )

  val CampaignWithLimitsOverdrafted = {
    val header = CampaignHeaderGen.next
      .copy(
        id = "with-limits-overdraft",
        settings = EnabledSettings,
        product = DefaultProduct,
        status = None
      )
    withPositiveBalance(header, header.product.totalCost + 1)
  }

  val CampaignWithProductCostMoreThanBalance = {
    val header = CampaignHeaderGen.next
      .copy(
        id = "product-cost-more-than-balance",
        settings = EnabledSettings,
        product = DefaultProduct,
        status = None
      )
    withPositiveBalance(header, header.product.totalCost + 1)
  }

  val CampaignWithDynamicPrice = {
    val product = Product(Placement(CostPerClick(DynamicPrice(None))))
    val header = CampaignHeaderGen.next
      .copy(
        id = "with dynamic price with undefined cost",
        settings = EnabledSettings,
        product = product,
        status = None
      )
    withPositiveBalance(header, 1000)
  }

  private val bindingDao = mock[BindingDao]
  private val campaignCallDao = mock[CampaignCallDao]

  private def mockCampaignService(
      campaignHeader: CampaignHeader,
      dailyLimit: Funds,
      weeklyLimit: Funds,
      monthlyLimit: Funds,
      spendings: Spendings): CampaignServiceWithStatusAndLimits = {
    val campaignDao = {
      val m = mock[CampaignDao]
      stub(m.get(_: CampaignService.Filter)(_: TransactionContext)) { case (_, _) =>
        Success(Some(campaignHeader))
      }
      m
    }

    val currentSpendingLimit = withLimit(dailyLimit, weeklyLimit, monthlyLimit)

    val limitServiceMock = {
      val m = mock[LimitService]
      val limit = Limit(
        currentSpendingLimit.daily,
        None,
        currentSpendingLimit.weekly,
        None,
        currentSpendingLimit.monthly,
        None
      )
      stub(m.get(_: CampaignId, _: DateTime)) { case (_, _) =>
        Success(limit)
      }
      when(m.getAllCurrent(?, ?)).thenReturn {
        Success(Map(campaignHeader.id -> currentSpendingLimit))
      }

      when(m.getAll(?, ?)).thenReturn {
        Success(Map(campaignHeader.id -> limit))
      }
      m
    }

    def getSpent(spent: Option[Spent]): Funds = spent.map(_.amount).getOrElse(0)

    val orderDaoMock = {
      val m = mock[OrderDao]
      stub(m.getAllSpent _) {
        case (ids, `CurrentDay`) if ids.toSet.contains(campaignHeader.id) =>
          Success(Map(campaignHeader.id -> getSpent(spendings.daily)))
        case (ids, `CurrentWeek`) if ids.toSet.contains(campaignHeader.id) =>
          Success(Map(campaignHeader.id -> getSpent(spendings.weekly)))
        case (ids, `CurrentMonth`) if ids.toSet.contains(campaignHeader.id) =>
          Success(Map(campaignHeader.id -> getSpent(spendings.monthly)))
        case _ =>
          Success(Map.empty)
      }
      m
    }

    val campaignEnricher = new CampaignWithStatusAndLimitsEnricher(limitServiceMock, orderDaoMock)
    new CampaignServiceImpl(campaignDao, bindingDao, campaignCallDao, DuplicationPolicy.AllowDuplicates)
      with CampaignServiceWithStatusAndLimits {

      def limitService: LimitService = limitServiceMock

      override def enricher: CampaignWithStatusAndLimitsEnricher = campaignEnricher
    }
  }

  private def withPositiveBalance(header: CampaignHeader, value: Funds): CampaignHeader =
    header.copy(order = withPositiveBalance(header.order, value))

  private def withNegativeBalance(order: Order, value: Funds): Order =
    order.copy(balance2 = order.balance2.copy(totalIncome = 0L, totalSpent = value))

  private def withNegativeBalance(header: CampaignHeader, value: Funds): CampaignHeader =
    header.copy(order = withNegativeBalance(header.order, value))

  private def withPositiveBalance(order: Order, value: Funds): Order =
    order.copy(balance2 = order.balance2.copy(totalIncome = value, totalSpent = 0L))
}
