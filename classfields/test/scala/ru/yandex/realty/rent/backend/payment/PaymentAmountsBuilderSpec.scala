package ru.yandex.realty.rent.backend.payment

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.TestUtil
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.backend.payment.periods._
import ru.yandex.realty.rent.proto.model.payment.FullnessTypeNamespace.FullnessType

@RunWith(classOf[JUnitRunner])
class PaymentAmountsBuilderSpec extends AsyncSpecBase with RentPaymentsData {

  import TestUtil.dt

  def conditionPeriod(conditions: RentPaymentConditions): ConditionPeriod =
    LastConditionPeriod(dt(2000, 1, 1), conditions, terminationAttributes = None)

  "PaymentAmountsBuilder" should {
    "calculate amounts for full period" in {
      val cp = conditionPeriod(paymentConditions(paymentDayOfMonth = Some(10)))
      val dates = GenericRentPaymentPeriod(dt(2020, 5, 10), dt(2020, 6, 10), FullnessType.FULL, cp)
      val amounts = DefaultPaymentAmountsBuilder.buildPaymentAmounts(dates, cp.conditions, Seq.empty)

      assertAmounts(
        amounts,
        11911, // 10000 * 1.05 + 1411
        10000,
        1411
      )
    }

    "calculate amounts for short period inside month (may)" in {
      val cp = conditionPeriod(paymentConditions(paymentDayOfMonth = Some(25)))
      val dates = GenericRentPaymentPeriod(dt(2020, 5, 10), dt(2020, 5, 25), MovedShortType, cp)
      val amounts = DefaultPaymentAmountsBuilder.buildPaymentAmounts(dates, cp.conditions, Seq.empty)

      assertAmounts(
        amounts,
        5763, // (10000 * 1.05 + 1411) / 31 * 15
        4839, // 10000 / 31 * 15
        683 // 1411 / 31 * 15
      )
    }

    "calculate amounts for short period inside month (february in leap year)" in {
      val cp = conditionPeriod(paymentConditions(paymentDayOfMonth = Some(25)))
      val dates = GenericRentPaymentPeriod(dt(2020, 2, 10), dt(2020, 2, 25), MovedShortType, cp)
      val amounts = DefaultPaymentAmountsBuilder.buildPaymentAmounts(dates, cp.conditions, Seq.empty)

      assertAmounts(
        amounts,
        6161, // (10000 * 1.05 + 1411) / 29 * 15
        5172, // 10000 / 29 * 15
        730 // 1411 / 29 * 15
      )
    }

    "calculate amounts for short period on months bound (may + june)" in {
      val cp = conditionPeriod(paymentConditions(paymentDayOfMonth = Some(5)))
      val dates = GenericRentPaymentPeriod(dt(2020, 5, 10), dt(2020, 6, 5), MovedShortType, cp)
      val amounts = DefaultPaymentAmountsBuilder.buildPaymentAmounts(dates, cp.conditions, Seq.empty)

      assertAmounts(
        amounts,
        10041, // (10000 * 1.05 + 1411) / 31 * 22 + (10000 * 1.05 + 1411) / 30 * 4
        8430, // 10000 / 31 * 22 + 10000 / 30 * 4
        1189 // 1411 / 31 * 22 + 1411 / 30 * 4
      )
    }

    "calculate amounts for short period on months bound (february in leap year + march)" in {
      val cp = conditionPeriod(paymentConditions(paymentDayOfMonth = Some(5)))
      val dates = GenericRentPaymentPeriod(dt(2020, 2, 10), dt(2020, 3, 5), MovedShortType, cp)
      val amounts = DefaultPaymentAmountsBuilder.buildPaymentAmounts(dates, cp.conditions, Seq.empty)

      assertAmounts(
        amounts,
        9751, // (10000 * 1.05 + 1411) / 29 * 20 + (10000 * 1.05 + 1411) / 31 * 4
        8187, // 10000 / 29 * 20 + 10000 / 31 * 4
        1155 // 1411 / 29 * 20 + 1411 / 31 * 4
      )
    }

    // todo REALTYBACK-6562: add tests for cases with new user agreements - new pricing strategy for insurance
  }

  private def assertAmounts(
    amounts: RentPaymentAmounts,
    expectedTenantPaymentAmount: Long, // In rubles
    expectedOwnerPaymentAmount: Long, // In rubles
    expectedTenantInsuranceAmount: Long // In rubles
  ): Unit = {
    amounts.tenantPaymentAmount shouldBe expectedTenantPaymentAmount * 100
    amounts.ownerPaymentAmount shouldBe expectedOwnerPaymentAmount * 100
    amounts.insuranceStrategy shouldBe InsuranceProportional
    amounts.tenantInsuranceAmount shouldBe expectedTenantInsuranceAmount * 100
    amounts.fullMonthInsuranceAmount shouldBe InsuranceAmount
  }
}
