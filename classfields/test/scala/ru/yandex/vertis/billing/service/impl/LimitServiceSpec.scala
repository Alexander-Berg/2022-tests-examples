package ru.yandex.vertis.billing.service.impl

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CampaignDao.DuplicationPolicy
import ru.yandex.vertis.billing.dao.impl.jdbc.order.JdbcOrderDao
import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcCampaignDao, JdbcCustomerDao, JdbcLimitDao, JdbcSpecTemplate}
import ru.yandex.vertis.billing.model_core.LimitSetting.{Daily, Monthly, Weekly}
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{CustomerHeaderGen, Producer, ProductGen, WithdrawRequest2Gen}
import ru.yandex.vertis.billing.service.tskv.TskvLoggedLimitService
import ru.yandex.vertis.billing.service.{CampaignService, SpendingsAreGreaterThanDesiredLimit}
import ru.yandex.vertis.billing.util.{AutomatedContext, DateTimeInterval}
import ru.yandex.vertis.billing.util.DateTimeUtils.{now, wholeDay, wholeMonth, wholeWeek}

import scala.annotation.nowarn
import scala.util.{Failure, Success, Try}

/**
  * Spec for [[LimitServiceImpl]]
  *
  * @author ruslansd
  * @author alesavin
  */
class LimitServiceSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate {

  private val customerDao = new JdbcCustomerDao(billingDatabase)
  private val campaignDao = new JdbcCampaignDao(billingDatabase)
  private val limitDao = new JdbcLimitDao(billingDatabase)
  private val orderDao = new JdbcOrderDao(billingDualDatabase)

  private val limitService =
    new LimitServiceImpl(limitDao, orderDao) with TskvLoggedLimitService {
      override def serviceName: String = "autoru"
    }

  implicit private val ac = AutomatedContext("limits-test")

  private def createCustomer(clientId: ClientId): CustomerHeader = {
    val customer = CustomerHeaderGen.next.copy(id = CustomerId(clientId, None))
    customerDao.create(customer).get
  }

  private def createOrder(customerId: CustomerId): Order = {
    val order = orderDao.create(customerId, OrderProperties("Order", Some("Description"))).get
    orderDao.upsertIncome(order.id.toString, order.id, 1000).get
    order
  }

  private def createCampaign(customerHeader: CustomerHeader, orderId: OrderId): CampaignHeader = {
    val product = ProductGen.next
    val source = CampaignService.Source(
      Some("Campaign for SimpleBalanceDao"),
      orderId,
      product,
      CampaignSettings.Default,
      None,
      Iterable.empty[OfferId]
    )
    campaignDao.create(customerHeader.id, source, DuplicationPolicy.AllowDuplicates).get
  }

  private def withCampaignIdAndAmount(campaignId: CampaignId, orderId: OrderId, amount: Funds) = {
    val w = WithdrawRequest2Gen(orderId).next
    w.copy(
      amount = amount,
      snapshot = w.snapshot.copy(campaignId = campaignId, time = now())
    )
  }

  private var curId = 0

  def createAll: (CustomerHeader, Order, CampaignHeader) = {
    val customer = createCustomer(curId)
    val order = createOrder(customer.id)
    val campaign = createCampaign(customer, order.id)
    curId = curId + 1
    (customer, order, campaign)
  }

  val currentDay = DateTimeInterval.currentDay
  val currentWeek = wholeWeek(currentDay.from)
  val currentMonth = wholeMonth(currentDay.from)
  val nextDay = wholeDay(currentDay.from.plusDays(1))
  val nextWeek = wholeWeek(currentDay.from.plusDays(7))
  val nextMonth = wholeMonth(currentDay.from.plusMonths(1))
  val prevDay = currentDay.from.minusDays(1)
  val prevWeek = currentWeek.from.minusWeeks(1)
  val prevMonth = currentMonth.from.minusMonths(1)

  def limit(
      currentDailyOpt: Option[Funds] = None,
      currentWeeklyOpt: Option[Funds] = None,
      currentMonthlyOpt: Option[Funds] = None,
      comingMonthlyOpt: Option[Funds] = None): LimitSource = {
    LimitSource(
      Some(TypedLimitSource(currentDailyOpt, None)),
      Some(TypedLimitSource(currentWeeklyOpt, None)),
      Some(TypedLimitSource(currentMonthlyOpt, comingMonthlyOpt))
    )
  }

  "Limit service" should {

    "create current limits with spent less then limits" in {
      val (_, order, campaign) = createAll
      orderDao.withdraw2(curId.toString, withCampaignIdAndAmount(campaign.id, order.id, 99))
      val limitSource = limit(Some(100), Some(100), Some(100))
      limitService.update(campaign.id, limitSource) match {
        case Success(
              Limit(
                Some(LimitSetting.Daily(100, currentDay.from)),
                None,
                Some(LimitSetting.Weekly(100, currentWeek.from)),
                None,
                Some(LimitSetting.Monthly(100, currentMonth.from)),
                None
              )
            ) =>
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "create current limits with spent equal to limits" in {
      val (_, order, campaign) = createAll
      orderDao.withdraw2(curId.toString, withCampaignIdAndAmount(campaign.id, order.id, 100))
      val limitSource = limit(Some(100), Some(100), Some(100))
      limitService.update(campaign.id, limitSource) match {
        case Success(
              Limit(
                Some(LimitSetting.Daily(100, currentDay.from)),
                None,
                Some(LimitSetting.Weekly(100, currentWeek.from)),
                None,
                Some(LimitSetting.Monthly(100, currentMonth.from)),
                None
              )
            ) =>
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "create coming limits with spent greater than limits (except monthly)" in {
      val (_, order, campaign) = createAll
      orderDao.withdraw2(curId.toString, withCampaignIdAndAmount(campaign.id, order.id, 101))
      val limitSource = limit(Some(100), Some(100))
      limitService.update(campaign.id, limitSource) match {
        case Success(
              Limit(
                None,
                Some(LimitSetting.Daily(100, nextDay.from)),
                None,
                Some(LimitSetting.Weekly(100, nextWeek.from)),
                None,
                None
              )
            ) =>
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "fail with spent greater than limits for monthly limits" in {
      val (_, order, campaign) = createAll
      orderDao.withdraw2(curId.toString, withCampaignIdAndAmount(campaign.id, order.id, 101))
      val limitSource = limit(currentMonthlyOpt = Some(100))
      limitService.update(campaign.id, limitSource) match {
        case Failure(_: SpendingsAreGreaterThanDesiredLimit) =>
          info("Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "update daily and weekly limits correctly" in {
      val (_, _, campaign) = createAll

      @nowarn("msg=local default argument in value <local LimitServiceSpec> is never used")
      def checkCorrectness(
          result: Try[Limit],
          currentLimit: Option[Funds] = None,
          comingLimit: Option[Funds] = None): Assertion = result match {
        case Success(l) =>
          val expectedCurrentDailyLimit = currentLimit.map { l =>
            LimitSetting.Daily(l, currentDay.from)
          }
          val expectedComingDailyLimit = comingLimit.map { l =>
            LimitSetting.Daily(l, nextDay.from)
          }
          val expectedCurrentWeeklyLimit = currentLimit.map { l =>
            LimitSetting.Weekly(l, currentWeek.from)
          }
          val expectedComingWeeklyLimit = comingLimit.map { l =>
            LimitSetting.Weekly(l, nextWeek.from)
          }
          val limit = Limit(
            expectedCurrentDailyLimit,
            expectedComingDailyLimit,
            expectedCurrentWeeklyLimit,
            expectedComingWeeklyLimit,
            None,
            None
          )
          l shouldBe limit
        case other =>
          fail(s"Unexpected $other")
      }

      val shouldBeCurrent = limit(Some(100), Some(100))
      checkCorrectness(limitService.update(campaign.id, shouldBeCurrent), Some(100))

      val shouldBeComing = limit(Some(99), Some(99))
      checkCorrectness(limitService.update(campaign.id, shouldBeComing), Some(100), Some(99))

      val shouldDropCurrent = limit(Some(100), Some(100))
      checkCorrectness(limitService.update(campaign.id, shouldDropCurrent), Some(100))

      val shouldUpCurrent = limit(Some(200), Some(200))
      checkCorrectness(limitService.update(campaign.id, shouldUpCurrent), Some(200))

      val shouldBeComing2 = limit(Some(100), Some(100))
      checkCorrectness(limitService.update(campaign.id, shouldBeComing2), Some(200), Some(100))

      val shouldUpCurrentAndDropComing = limit(Some(300), Some(300))
      checkCorrectness(limitService.update(campaign.id, shouldUpCurrentAndDropComing), Some(300))
    }

    "not update old current limits" in {
      val (_, _, campaign) = createAll

      val settings = Iterable(
        LimitSetting.Daily(100, prevDay),
        LimitSetting.Weekly(100, prevWeek),
        LimitSetting.Monthly(100, prevMonth)
      )
      (limitDao.update(campaign.id, settings) should be).a(Symbol("Success"))

      val shouldUpCurrent = limit(Some(200), Some(200), Some(200))
      limitService.update(campaign.id, shouldUpCurrent)

      limitService.getCurrent(campaign.id, now()) match {
        case Success(
              CurrentLimit(
                Some(Daily(200, currentDay.from)),
                Some(Weekly(200, currentWeek.from)),
                Some(Monthly(200, currentMonth.from))
              )
            ) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "update monthly limit correctly" in {
      val (_, _, campaign) = createAll

      def checkCorrectness(result: Try[Limit], currentLimit: Option[Funds], comingLimit: Option[Funds]): Assertion =
        result match {
          case Success(l) =>
            val expectedCurrentMonthlyLimit = currentLimit.map { l =>
              LimitSetting.Monthly(l, currentMonth.from)
            }
            val expectedComingMonthlyLimit = comingLimit.map { l =>
              LimitSetting.Monthly(l, nextMonth.from)
            }
            val limit = Limit(
              None,
              None,
              None,
              None,
              expectedCurrentMonthlyLimit,
              expectedComingMonthlyLimit
            )
            l shouldBe limit
          case other =>
            fail(s"Unexpected $other")
        }

      val shouldCreateBoth = limit(currentMonthlyOpt = Some(100), comingMonthlyOpt = Some(100))
      checkCorrectness(limitService.update(campaign.id, shouldCreateBoth), Some(100), Some(100))

      val shouldNotChange1 = limit(currentMonthlyOpt = Some(100))
      checkCorrectness(limitService.update(campaign.id, shouldNotChange1), Some(100), Some(100))

      val shouldNotChange2 = limit(comingMonthlyOpt = Some(100))
      checkCorrectness(limitService.update(campaign.id, shouldNotChange2), Some(100), Some(100))

      val shouldNotChange3 = limit()
      checkCorrectness(limitService.update(campaign.id, shouldNotChange3), Some(100), Some(100))

      val shouldIncreaseCurrent = limit(currentMonthlyOpt = Some(101))
      checkCorrectness(limitService.update(campaign.id, shouldIncreaseCurrent), Some(101), Some(100))

      val shouldIncreaseComing = limit(comingMonthlyOpt = Some(101))
      checkCorrectness(limitService.update(campaign.id, shouldIncreaseComing), Some(101), Some(101))

      val shouldIncreaseDecreaseComing = limit(comingMonthlyOpt = Some(100))
      checkCorrectness(limitService.update(campaign.id, shouldIncreaseDecreaseComing), Some(101), Some(100))

      val shouldDropComing = limit(comingMonthlyOpt = Some(0))
      checkCorrectness(limitService.update(campaign.id, shouldDropComing), Some(101), None)
    }

    "drop limits" in {
      val (_, _, campaign) = createAll
      val Now = now()
      (limitService.update(campaign.id, limit(Some(20), Some(200), Some(1000))) should be).a(Symbol("Success"))
      limitService.getCurrent(campaign.id, Now) match {
        case Success(CurrentLimit(Some(Daily(20, _)), Some(Weekly(200, _)), Some(Monthly(1000, _)))) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
      (limitService.dropLimit(campaign.id, Iterable(LimitTypes.Daily)) should be).a(Symbol("Success"))

      limitService.getCurrent(campaign.id, Now) match {
        case Success(CurrentLimit(None, Some(Weekly(200, _)), Some(Monthly(1000, _)))) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }

      (limitService.dropLimit(campaign.id, Iterable(LimitTypes.Weekly)) should be).a(Symbol("Success"))

      limitService.getCurrent(campaign.id, Now) match {
        case Success(CurrentLimit(None, None, Some(Monthly(1000, _)))) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }

      (limitService.dropLimit(campaign.id, Iterable(LimitTypes.Monthly)) should be).a(Symbol("Success"))

      limitService.getCurrent(campaign.id, Now) match {
        case Success(CurrentLimit(None, None, None)) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "drop old limits" in {
      val (_, _, campaign) = createAll
      val Now = now()

      val settings = Iterable(
        LimitSetting.Daily(100, prevDay),
        LimitSetting.Weekly(200, prevWeek),
        LimitSetting.Monthly(1000, prevMonth)
      )
      (limitDao.update(campaign.id, settings) should be).a(Symbol("Success"))

      limitService.getCurrent(campaign.id, Now) match {
        case Success(CurrentLimit(Some(Daily(100, day)), Some(Weekly(200, week)), Some(Monthly(1000, month))))
            if day == prevDay && week == prevWeek && month == prevMonth =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
      (limitService.dropLimit(campaign.id, Iterable(LimitTypes.Daily)) should be).a(Symbol("Success"))

      limitService.getCurrent(campaign.id, Now) match {
        case Success(CurrentLimit(None, Some(Weekly(200, week)), Some(Monthly(1000, month))))
            if week == prevWeek && month == prevMonth =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }

      (limitService.dropLimit(campaign.id, Iterable(LimitTypes.Weekly)) should be).a(Symbol("Success"))

      limitService.getCurrent(campaign.id, Now) match {
        case Success(CurrentLimit(None, None, Some(Monthly(1000, month)))) if month == prevMonth =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }

      (limitService.dropLimit(campaign.id, Iterable(LimitTypes.Monthly)) should be).a(Symbol("Success"))

      limitService.getCurrent(campaign.id, Now) match {
        case Success(CurrentLimit(None, None, None)) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "delete current and coming limits" in {
      val (_, _, campaign) = createAll
      val Now = now()
      val settings = Iterable(
        LimitSetting.Daily(100, currentDay.from),
        LimitSetting.Weekly(200, currentWeek.from),
        LimitSetting.Monthly(1000, currentMonth.from),
        LimitSetting.Monthly(999, nextMonth.from)
      )
      (limitDao.update(campaign.id, settings) should be).a(Symbol("Success"))

      limitService.getCurrent(campaign.id, Now) match {
        case Success(
              CurrentLimit(
                Some(Daily(100, currentDay.from)),
                Some(Weekly(200, currentWeek.from)),
                Some(Monthly(1000, currentMonth.from))
              )
            ) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }

      val coming = limit(Some(50), Some(100))
      (limitService.update(campaign.id, coming) should be).a(Symbol("Success"))

      limitService.get(campaign.id, Now) match {
        case Success(
              Limit(
                Some(Daily(100, _)),
                Some(Daily(50, _)),
                Some(Weekly(200, _)),
                Some(Weekly(100, _)),
                Some(Monthly(1000, _)),
                Some(Monthly(999, _))
              )
            ) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }

      (limitService.dropLimit(
        campaign.id,
        Iterable(LimitTypes.Daily, LimitTypes.Weekly, LimitTypes.Monthly)
      ) should be).a(Symbol("Success"))

      limitService.get(campaign.id, Now) match {
        case Success(Limit.Empty) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

    "set limit to value1, then to value2, and return to value1 (VSBILLING-2848)" in {
      val (_, _, campaign) = createAll
      val Now = now()
      val prevDay = currentDay.from.minusDays(1)
      val prevWeek = currentWeek.from.minusWeeks(1)
      val settings = Iterable(
        LimitSetting.Daily(100, prevDay),
        LimitSetting.Weekly(200, prevWeek)
      )
      (limitDao.update(campaign.id, settings) should be).a(Symbol("Success"))

      (limitService.update(campaign.id, limit(Some(50), Some(100))) should be).a(Symbol("Success"))
      limitService.get(campaign.id, Now) match {
        case Success(
              Limit(Some(Daily(100, _)), Some(Daily(50, _)), Some(Weekly(200, _)), Some(Weekly(100, _)), None, None)
            ) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
      (limitService.update(campaign.id, limit(Some(100), Some(200))) should be).a(Symbol("Success"))
      limitService.get(campaign.id, Now) match {
        case Success(Limit(Some(Daily(100, _)), None, Some(Weekly(200, _)), None, None, None)) =>
          info(s"Done")
        case other =>
          fail(s"Unexpected $other")
      }
    }

  }
}
