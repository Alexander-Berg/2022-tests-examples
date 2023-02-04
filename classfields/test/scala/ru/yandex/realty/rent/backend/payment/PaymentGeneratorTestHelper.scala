package ru.yandex.realty.rent.backend.payment

import org.joda.time.{DateTime, LocalDate}
import ru.yandex.realty.rent.TestUtil
import ru.yandex.realty.rent.proto.model.contract.CalculationStrategyNamespace.CalculationStrategy
import ru.yandex.realty.rent.proto.model.contract.Commissions

import scala.language.implicitConversions

object PaymentGeneratorTestHelper {
  import TestUtil.dt

  case class ConditionPeriodBuilder() {
    private var list: List[Param] = Nil
    case class Param(
      periodStartDate: DateTime,
      paymentDayOfMonth: Option[Int]
    )

    //добавляет дату начала следующего периода и конца предыдущего
    def next(startDate: String, paymentDayOfMonth: Option[Int] = None): ConditionPeriodBuilder = {
      list ::= Param(dt(startDate), paymentDayOfMonth)
      this
    }

    //праметры для последнего периода
    def end(
      endDate: Option[String] = None,
      notificationDate: Option[String] = None,
      tenantRefusedPayFor30Days: Boolean = false,
      checkOutWithoutAdditionalPayments: Boolean = false
    ): List[ConditionPeriod] = {
      val lp :: tail = list
      val last = LastConditionPeriod(
        startDate = lp.periodStartDate,
        conditions = paymentConditions(
          list.last.periodStartDate,
          lp.paymentDayOfMonth getOrElse lp.periodStartDate.getDayOfMonth
        ),
        terminationAttributes = endDate.map(dt).map { d =>
          TerminationAttributes(
            endDate = d,
            notificationDate = notificationDate.map(dt) getOrElse d.minusDays(31),
            tenantRefusedPayFor30Days = tenantRefusedPayFor30Days,
            checkOutWithoutAdditionalPayments = checkOutWithoutAdditionalPayments
          )
        }
      )
      tail
        .foldLeft[List[ConditionPeriod]](last :: Nil) { (acc, p) =>
          RegularConditionPeriod(
            p.periodStartDate,
            paymentConditions(
              list.last.periodStartDate,
              p.paymentDayOfMonth getOrElse p.periodStartDate.getDayOfMonth
            ),
            acc.head
          ) :: acc
        }
    }
  }

  private def paymentConditions(
    rentStartDate: DateTime,
    paymentDayOfMonth: Int
  ): RentPaymentConditions =
    RentPaymentConditions(
      3000000,
      rentStartDate,
      paymentDayOfMonth,
      141100,
      CalculationStrategy.STRATEGY_2,
      Commissions
        .newBuilder()
        .setMonthlyTenantCommission(0.05f)
        .setMonthlyTenantHouseServiceCommission(0.0f)
        .build()
    )
}
