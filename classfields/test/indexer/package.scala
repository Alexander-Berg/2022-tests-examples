package ru.yandex.vertis.billing

import org.scalacheck.Gen
import ru.yandex.vertis.billing.dao.OrderDao
import ru.yandex.vertis.billing.model_core.CampaignStatus.Inactive
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.service.{LimitService, PaidOffersService}
import ru.yandex.vertis.billing.status.LimitHelper
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, wholeWeek}

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * Test data and mocks.
  *
  * @author dimas
  */
package object indexer {

  import ru.yandex.vertis.mockito.MockitoSupport.{mock, stub}

  val offer1 = PartnerOfferId("p1", "o1")
  val offer2 = PartnerOfferId("p2", "o2")
  val offer3 = PartnerOfferId("p3", "o3")

  val NonExistsCampaignId = "non-exists"
  val FailedCampaignId = "failed"

  val DailySpendingLimit: Funds = 100L
  val WeeklySpentLimit: Funds = 1000L
  val CampaignProduct = Product(Placement(CostPerCall(FixPrice(10))))

  val CurrentDay = DateTimeInterval.currentDay
  val CurrentWeek = wholeWeek(now())

  def withLimit(daily: Funds, weekly: Funds) =
    CurrentLimit(
      Some(LimitSetting.Daily(daily, CurrentDay.from)),
      Some(LimitSetting.Weekly(weekly, CurrentWeek.from))
    )

  val CurrentSpendingLimit = withLimit(DailySpendingLimit, WeeklySpentLimit)

  val EnabledSettings = CampaignSettings.Default
  val DisabledSettings = CampaignSettings.Default.copy(isEnabled = false)

  val EnabledCampaign = {
    val header = CampaignHeaderGen.next.copy(
      id = "enabled",
      settings = EnabledSettings,
      status = None
    )
    val withPartner = withPartnerId(
      header
        .copy(order = withPositiveBalance(header.order, 1000), product = ProductGen.suchThat(_.hasDefinedCost).next),
      offer1
    )
    withPositiveBalance(
      withPartner,
      withPartner.product.totalCost + 1
    )
  }

  val InactiveCampaign = {
    val header = CampaignHeaderGen.next.copy(
      id = "inactive",
      settings = EnabledSettings,
      status = Some(Inactive(InactiveReasons.MismatchPartnerId))
    )
    val withPartner = withPartnerId(
      header
        .copy(order = withPositiveBalance(header.order, 1000), product = ProductGen.suchThat(_.hasDefinedCost).next),
      offer1
    )
    withPositiveBalance(
      withPartner,
      withPartner.product.totalCost + 1
    )
  }

  val EnabledCampaignGen: Gen[CampaignHeader] =
    for {
      campaignId <- CampaignIdGen
      name <- Gen.option(Gen.alphaStr)
      customerHeader <- CustomerHeaderGen
      product <- ProductGen
      callSettings <- Gen.option(CallSettingsGen)
      attachRule <- Gen.option(AttachRuleGen)
      platforms <- Gen.option(EnabledPlatformsGen)
      deposit <- Gen.option(DepositGen)
      order <- {
        val minForProduct = math.max(1, product.totalCost)
        val minForDeposit = deposit match {
          case Some(CoefficientDeposit(coefficient)) =>
            coefficient * product.totalCost
          case Some(FixAmountDeposit(fixAmount)) =>
            fixAmount
          case _ =>
            0L
        }
        val neededBalance = math.max(minForProduct, minForDeposit)
        orderGen(customerHeader.id, 3).map { order =>
          val diff = neededBalance - order.balance2.current
          if (diff > 0) {
            val b = order.balance2.copy(totalIncome = order.balance2.totalIncome + diff)
            order.copy(balance2 = b)
          } else {
            order
          }
        }
      }
      settings = CampaignSettings(
        isEnabled = true,
        Limit.Empty,
        callSettings,
        None,
        attachRule,
        platforms,
        deposit
      )
    } yield CampaignHeader(
      campaignId,
      name,
      customerHeader,
      order,
      product,
      settings,
      None
    )

  val DisabledCampaign = {
    val header = CampaignHeaderGen.next.copy(id = "disabled").copy(settings = DisabledSettings)
    withPartnerId(
      header.copy(
        order = withPositiveBalance(header.order, 1000),
        status = Some(Inactive(InactiveReasons.ManuallyDisabledCampaign))
      ),
      offer1
    )
  }

  val NotEnoughFundsCampaign = {
    val order = OrderGen.next.copy(balance2 = OrderBalance2(totalIncome = 500, totalSpent = 0))
    val cost = costGen(1000).next
    val goods: Set[Good] = Set(
      Highlighting(cost),
      Raising(CostPerAction)
    )
    val header = CampaignHeaderGen.next
      .copy(status = Some(Inactive(InactiveReasons.NoEnoughFunds)))

    withPartnerId(
      header.copy(order = order, product = Product(goods), settings = CampaignSettings.Default),
      offer1
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
    val header = CampaignHeaderGen.next
    withPartnerId(
      header.copy(order = order, product = Product(goods), settings = CampaignSettings.Default, status = None),
      offer1
    )
  }

  val NoFundsPerDayCampaign = {
    val header = CampaignHeaderGen.next
    withPartnerId(
      header.copy(
        id = "no-funds",
        settings = EnabledSettings,
        product = header.product.copy(goods = header.product.goods + Highlighting(CostPerDay(1L))),
        order = withNegativeBalance(header.order, 10),
        status = None
      ),
      offer1
    )
  }

  val NoFundsPerIndexingCampaign = {
    val header = CampaignHeaderGen.next
    withPartnerId(
      header.copy(
        id = "no-funds",
        settings = EnabledSettings,
        status = None,
        product = header.product.copy(goods = header.product.goods + Placement(CostPerIndexing(2L))),
        order = withNegativeBalance(header.order, 10)
      ),
      offer1
    )
  }

  val CampaignWithDailyLimitOverdrafted =
    withPartnerId(
      withPositiveBalance(
        CampaignHeaderGen.next.copy(
          settings = EnabledSettings,
          product = CampaignProduct,
          id = "with-daily-limit-overdraft",
          status = Some(Inactive(InactiveReasons.DailyLimitExceeded))
        ),
        1000
      ),
      offer3
    )

  val CampaignWithWeeklyLimitOverdrafted =
    withPartnerId(
      withPositiveBalance(
        CampaignHeaderGen.next.copy(
          settings = EnabledSettings,
          product = CampaignProduct,
          id = "with-weekly-limit-overdraft",
          status = Some(Inactive(InactiveReasons.WeeklyLimitExceeded))
        ),
        10000
      ),
      offer3
    )

  val CampaignWithLimitNotOverdrafted =
    withPartnerId(
      withPositiveBalance(
        CampaignHeaderGen.next
          .copy(id = "with-limit-not-overdraft", product = CampaignProduct, settings = EnabledSettings, status = None),
        1000
      ),
      offer3
    )

  val CampaignWithWeeklyHighOverdraft =
    withPartnerId(
      withPositiveBalance(
        CampaignHeaderGen.next.copy(
          settings = EnabledSettings,
          product = CampaignProduct,
          id = "with-weekly-high-overdraft",
          status = None
        ),
        1000
      ),
      offer3
    )

  val CampaignWithDailyHighOverdraft =
    withPartnerId(
      withPositiveBalance(
        CampaignHeaderGen.next
          .copy(settings = EnabledSettings, product = CampaignProduct, id = "with-daily-high-overdraft", status = None),
        1000
      ),
      offer3
    )

  implicit def asSuccess[T](t: T): Try[T] = Success(t)

  val campaigns = {
    val m = mock[CampaignProvider]
    stub(m.get _)(
      Map(
        EnabledCampaign.id -> Some(EnabledCampaign),
        NoFundsPerDayCampaign.id -> Some(NoFundsPerDayCampaign),
        NonExistsCampaignId -> None,
        FailedCampaignId -> Failure(new NoSuchElementException("artificial")),
        CampaignWithWeeklyLimitOverdrafted.id -> Some(CampaignWithWeeklyLimitOverdrafted),
        CampaignWithDailyLimitOverdrafted.id -> Some(CampaignWithDailyLimitOverdrafted),
        CampaignWithLimitNotOverdrafted.id -> Some(CampaignWithLimitNotOverdrafted),
        CampaignWithWeeklyHighOverdraft.id -> Some(CampaignWithWeeklyHighOverdraft),
        CampaignWithDailyHighOverdraft.id -> Some(CampaignWithDailyHighOverdraft)
      )
    )
    m
  }

  val paidOffers = {
    val m = mock[PaidOffersService]
    stub(m.isPaidToday _) {
      case (`offer1`, id) if id == NoFundsPerDayCampaign.id => true
      case _ => false
    }
    m
  }

  val limitService = {
    val m = mock[LimitService]
    val limit =
      Limit(CurrentSpendingLimit.daily, None, CurrentSpendingLimit.weekly, None, CurrentSpendingLimit.monthly, None)
    stub(m.getAllCurrent _) { case (ids, _) =>
      Success(ids.map((_, CurrentSpendingLimit)).toMap)
    }
    stub(m.getCurrent _) {
      case (id, _)
          if id == CampaignWithDailyLimitOverdrafted.id ||
            id == CampaignWithWeeklyLimitOverdrafted.id ||
            id == CampaignWithLimitNotOverdrafted.id ||
            id == CampaignWithWeeklyHighOverdraft.id ||
            id == CampaignWithDailyHighOverdraft.id =>
        CurrentSpendingLimit
      case _ => CurrentLimit.Empty
    }

    stub(m.getAll _) {
      case (ids, _)
          if ids.toSet.contains(CampaignWithDailyLimitOverdrafted.id) ||
            ids.toSet.contains(CampaignWithWeeklyLimitOverdrafted.id) ||
            ids.toSet.contains(CampaignWithLimitNotOverdrafted.id) ||
            ids.toSet.contains(CampaignWithWeeklyHighOverdraft.id) ||
            ids.toSet.contains(CampaignWithDailyHighOverdraft.id) =>
        Success(ids.map(_ -> limit).toMap)
      case (ids, _) => Success(ids.map(_ -> Limit.Empty).toMap)
    }

    m
  }

  val orderDao = {
    val m = mock[OrderDao]
    val map = Map[(CampaignId, DateTimeInterval), Try[Funds]](
      (CampaignWithDailyLimitOverdrafted.id, CurrentDay) -> (DailySpendingLimit + 1 - CampaignProduct.totalCost),
      (CampaignWithDailyLimitOverdrafted.id, CurrentWeek) -> (WeeklySpentLimit - 1 - CampaignProduct.totalCost),
      (CampaignWithWeeklyHighOverdraft.id, CurrentDay) -> (DailySpendingLimit + 10 - CampaignProduct.totalCost),
      (CampaignWithWeeklyHighOverdraft.id, CurrentWeek) -> (WeeklySpentLimit + 20 - CampaignProduct.totalCost),
      (CampaignWithDailyHighOverdraft.id, CurrentDay) -> (DailySpendingLimit + 50 - CampaignProduct.totalCost),
      (CampaignWithDailyHighOverdraft.id, CurrentWeek) -> (WeeklySpentLimit + 10 - CampaignProduct.totalCost),
      (CampaignWithWeeklyLimitOverdrafted.id, CurrentDay) -> (DailySpendingLimit - 1 - CampaignProduct.totalCost),
      (CampaignWithWeeklyLimitOverdrafted.id, CurrentWeek) -> (WeeklySpentLimit + 1 - CampaignProduct.totalCost)
    )
    stub(m.getSpent _)(map.orElse {
      case (id, _) if id == CampaignWithLimitNotOverdrafted.id => DailySpendingLimit - 1 - CampaignProduct.totalCost
      case _ => 0L
    })

    stub(m.getAllSpent _) { case (_, interval) =>
      map.collect {
        case ((id, intr), funds) if intr == interval =>
          id -> funds.get
      }
    }
    m
  }

  val limitHelper =
    new LimitHelper(limitService, orderDao)

  private def withPositiveBalance(header: CampaignHeader, value: Funds): CampaignHeader =
    header.copy(order = withPositiveBalance(header.order, value))

  private def withNegativeBalance(header: CampaignHeader, value: Funds): CampaignHeader =
    header.copy(order = withNegativeBalance(header.order, value))

  private def withPositiveBalance(order: Order, value: Funds): Order =
    order.copy(balance2 = order.balance2.copy(totalIncome = value, totalSpent = 0))

  private def withNegativeBalance(order: Order, value: Funds): Order =
    order.copy(balance2 = order.balance2.copy(totalIncome = 0, totalSpent = value))

  private def withPartnerId(campaign: CampaignHeader, offer: PartnerOfferId) =
    campaign.copy(customer = campaign.customer.copy(resources = Seq(PartnerRef(offer.partnerId))))

}
