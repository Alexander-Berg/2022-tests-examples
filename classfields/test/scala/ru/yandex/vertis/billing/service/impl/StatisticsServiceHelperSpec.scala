package ru.yandex.vertis.billing.service.impl

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.GroupStatisticPoint.ProductCostTypeGroup
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.service.StatisticsService.Precisions
import ru.yandex.vertis.billing.service.impl.StatisticsServiceHelper._
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.DateTimeUtils.now

import scala.util.Success

/**
  * Spec on [[StatisticsServiceHelper]]
  *
  * @author ruslansd
  * @author alesavin
  */
class StatisticsServiceHelperSpec extends AnyWordSpec with Matchers {

  "StatisticsServiceHelper" should {
    val Now = now()

    val statistics = Iterable(
      getStatistic(Now.withHourOfDay(0), 1L),
      getStatistic(Now.withHourOfDay(1), 2L),
      getStatistic(Now.withHourOfDay(2), 3L)
    )
    val groups = Iterable(
      getGroup(CostTypes.CostPerDay, Now.withHourOfDay(0), 1L),
      getGroup(CostTypes.CostPerDay, Now.withHourOfDay(0), 2L),
      getGroup(CostTypes.CostPerCall, Now.withHourOfDay(0), 3L)
    )
    val fundsGroups = Iterable(
      getFundsGroup(CostTypes.CostPerDay, Now.withHourOfDay(0), 1L),
      getFundsGroup(CostTypes.CostPerDay, Now.withHourOfDay(0), 2L),
      getFundsGroup(CostTypes.CostPerCall, Now.withHourOfDay(0), 3L),
      getFundsGroup(CostTypes.CostPerCall, Now.withHourOfDay(0), 4L)
    )

    "calculate correct hourly group" in {
      precise(statistics, Precisions.Hour) match {
        case Success(`statistics`) => info("Done")
        case other => fail(s"Unexpected $other")
      }

      val statistics2 = statistics ++
        Iterable(getStatistic(Now.withHourOfDay(1), 4L))
      precise(statistics2, Precisions.Hour) match {
        case Success(result) =>
          result.toList match {
            case StatisticPoint(_, 1, 1, 1, 1, 1, 1) ::
                StatisticPoint(_, 6, 6, 6, 6, 3, 6) ::
                StatisticPoint(_, 3, 3, 3, 3, 3, 3) :: Nil =>
              info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }

      val statistics3 = statistics ++
        Iterable(getStatistic(Now.withHourOfDay(10), 4L))
      precise(statistics3, Precisions.Hour) match {
        case Success(result) =>
          result.toList match {
            case StatisticPoint(_, 1, 1, 1, 1, 1, 1) ::
                StatisticPoint(_, 2, 2, 2, 2, 2, 2) ::
                StatisticPoint(_, 3, 3, 3, 3, 3, 3) ::
                StatisticPoint(_, 4, 4, 4, 4, 4, 4) :: Nil =>
              info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }

    }
    "calculate correct daily group" in {
      val averagePosition = StatisticUtils.average(1 * 1 + 2 * 2 + 3 * 3, 6)
      precise(statistics, Precisions.Day) match {
        case Success(result) =>
          result.toList match {
            case StatisticPoint(_, 6, 6, 6, 6, `averagePosition`, 6) :: Nil =>
              info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }

      val averagePosition2 = StatisticUtils.average(2 * 2 + 3 * 3, 5)
      precise(statistics.tail, Precisions.Day) match {
        case Success(result) =>
          result.toList match {
            case StatisticPoint(_, 5, 5, 5, 5, `averagePosition2`, 5) :: Nil =>
              info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }
    "calculate correct daily group for empty" in {
      precise(Iterable.empty, Precisions.Day) match {
        case Success(result) =>
          result.toList match {
            case Nil => info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }

    "calculate correct grouped for empty statistics" in {
      preciseGrouped(Iterable.empty, Precisions.Day) match {
        case Success(result) =>
          result.toList match {
            case Nil => info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }
    "calculate correct grouped" in {
      preciseGrouped(groups, Precisions.Day) match {
        case Success(result) =>
          result.toList match {
            case GroupStatisticPoint(_, StatisticPoint(_, 3, 3, 3, 3, 2, 3)) ::
                GroupStatisticPoint(_, StatisticPoint(_, 3, 3, 3, 3, 3, 3)) :: Nil =>
              info("Done")
            case GroupStatisticPoint(_, StatisticPoint(_, 3, 3, 3, 3, 3, 3)) ::
                GroupStatisticPoint(_, StatisticPoint(_, 3, 3, 3, 3, 2, 3)) :: Nil =>
              info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }

    "calculate correct grouped funds for empty" in {
      preciseGroupedFunds(Iterable.empty, Precisions.Day) match {
        case Success(result) =>
          result.toList match {
            case Nil => info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }
    "calculate correct grouped funds" in {
      preciseGroupedFunds(fundsGroups, Precisions.Day) match {
        case Success(result) =>
          result.toList match {
            case GroupFundsPoint(_, FundsPoint(_, 3)) ::
                GroupFundsPoint(_, FundsPoint(_, 7)) :: Nil =>
              info("Done")
            case GroupFundsPoint(_, FundsPoint(_, 7)) ::
                GroupFundsPoint(_, FundsPoint(_, 3)) :: Nil =>
              info("Done")
            case other => fail(s"Unexpected $other")
          }
        case other => fail(s"Unexpected $other")
      }
    }

  }

  def getStatistic(date: DateTime, count: Long) =
    StatisticPoint(DateTimeInterval.hourIntervalFrom(date), count, count, count, count, count, count)

  def getGroup(costType: CostTypes.Value, date: DateTime, count: Long) =
    GroupStatisticPoint(ProductCostTypeGroup(Iterable(costType)), getStatistic(date, count))

  def getFundsGroup(costType: CostTypes.Value, date: DateTime, count: Long) =
    GroupFundsPoint(
      ProductCostTypeGroup(Iterable(costType)),
      FundsPoint(DateTimeInterval.hourIntervalFrom(date), count)
    )
}
