package ru.yandex.realty.rent.stage.contract

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.backend.payment.periods.NewRentPaymentPeriodGenerator
import ru.yandex.realty.rent.backend.payment.DefaultRentPaymentsGenerator
import ru.yandex.realty.rent.model.enums.PaymentStatus
import ru.yandex.realty.rent.model.{ContractWithPayments, Payment}
import ru.yandex.realty.rent.proto.model.payment.FullnessTypeNamespace.FullnessType
import ru.yandex.realty.rent.util.Money
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState

import ru.yandex.realty.rent.TestUtil

@RunWith(classOf[JUnitRunner])
class CreatePaymentStageSpec extends AsyncSpecBase with RentPaymentsData {

  import FullnessType._
  import TestUtil.dt
  import RentPaymentsData._

  private val AP: Int = RentAmount
  private val K1: Float = DefaultCommissions.getMonthlyTenantCommission
  private val S: Float = InsuranceAmount

  private def invokeStage(contract: ContractWithPayments): ProcessingState[ContractWithPayments] = {
    val state = ProcessingState(contract)
    val stage = new CreatePaymentStage(new DefaultRentPaymentsGenerator(NewRentPaymentPeriodGenerator))
    stage.process(state)(Traced.empty).futureValue
  }

  "CreatePaymentStage" should {
    "create first two payments before rent start date" in {
      val paymentDate = TodayDate.plusDays(11)
      val secondPaymentDate = paymentDate.plusMonths(1)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth, nowMoment = Some(TodayDate))
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val newState = invokeStage(contractWithPayments)
      val firstExpectedPayment = createPayment(contract, paymentDate, secondPaymentDate.minusDays(1))
      val secondExpectedPayment =
        createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
      firstExpectedPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstExpectedPayment.data.getOwnerPaymentAmount shouldBe AP
      newState.entry.payments.zip(Seq(firstExpectedPayment, secondExpectedPayment)).map {
        case (actual, expected) => checkEquality(actual, expected)
      }
    }

    "not create first two payments if they already exist" in {
      val paymentDate = TodayDate.plusDays(11)
      val secondPaymentDate = paymentDate.plusMonths(1)
      val contract = createContract(paymentDate, paymentDate.getDayOfMonth, nowMoment = Some(TodayDate))
      val firstExpectedPayment = createPayment(contract, paymentDate, secondPaymentDate.minusDays(1))
      val secondExpectedPayment =
        createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
      firstExpectedPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstExpectedPayment.data.getOwnerPaymentAmount shouldBe AP

      val contractWithPayments = ContractWithPayments(contract, List(firstExpectedPayment, secondExpectedPayment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.zip(contractWithPayments.payments).map {
        case (actual, expected) => checkEquality(actual, expected)
      }
    }

    "create 3rd payment at the show date of 2nd payment" in {
      val secondPaymentDate = TodayDate.plusDays(10)
      val firstPaymentDate = secondPaymentDate.minusMonths(1)
      val contract = createContract(firstPaymentDate, firstPaymentDate.getDayOfMonth, nowMoment = Some(TodayDate))
      val firstPayment = createPayment(contract, firstPaymentDate, firstPaymentDate.plusMonths(1).minusDays(1))
      firstPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstPayment.data.getOwnerPaymentAmount shouldBe AP
      val secondPayment = createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
      secondPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      secondPayment.data.getOwnerPaymentAmount shouldBe AP
      val contractWithPayments = ContractWithPayments(contract, List(firstPayment, secondPayment))
      val newState = invokeStage(contractWithPayments)

      val thirdPaymentDate = secondPaymentDate.plusMonths(1)
      val thirdPayment = createPayment(contract, thirdPaymentDate, thirdPaymentDate.plusMonths(1).minusDays(1))
      thirdPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      thirdPayment.data.getOwnerPaymentAmount shouldBe AP
      newState.entry.payments.zip(Seq(firstPayment, secondPayment, thirdPayment)).map {
        case (actual, expected) => checkEquality(actual, expected)
      }
    }

    "create 4th payment at the show date of 3rd payment" in {
      val thirdPaymentDate = TodayDate.plusDays(10)
      val secondPaymentDate = thirdPaymentDate.minusMonths(1)
      val firstPaymentDate = secondPaymentDate.minusMonths(1)
      val contract = createContract(firstPaymentDate, firstPaymentDate.getDayOfMonth, nowMoment = Some(TodayDate))
      val firstPayment = createPayment(contract, firstPaymentDate, firstPaymentDate.plusMonths(1).minusDays(1))
      firstPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstPayment.data.getOwnerPaymentAmount shouldBe AP
      val secondPayment = createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
      secondPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      secondPayment.data.getOwnerPaymentAmount shouldBe AP
      val thirdPayment = createPayment(contract, thirdPaymentDate, thirdPaymentDate.plusMonths(1).minusDays(1))
      val contractWithPayments = ContractWithPayments(contract, List(firstPayment, secondPayment, thirdPayment))
      val newState = invokeStage(contractWithPayments)

      val fourthPaymentDate = thirdPaymentDate.plusMonths(1)
      val fourthPayment = createPayment(contract, fourthPaymentDate, fourthPaymentDate.plusMonths(1).minusDays(1))
      fourthPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      fourthPayment.data.getOwnerPaymentAmount shouldBe AP
      newState.entry.payments.zip(Seq(firstPayment, secondPayment, thirdPayment, fourthPayment)).map {
        case (actual, expected) => checkEquality(actual, expected)
      }
    }

    "create first payments for contract activation after rent start date" in {
      val rentStartDate = TodayDate.minusDays(2)
      val contract = createContract(rentStartDate, rentStartDate.getDayOfMonth, nowMoment = Some(TodayDate))
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val newState = invokeStage(contractWithPayments)
      val firstPayment = createPayment(contract, rentStartDate, rentStartDate.plusMonths(1).minusDays(1))
      firstPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstPayment.data.getOwnerPaymentAmount shouldBe AP
      val secondPaymentDate = rentStartDate.plusMonths(1)
      val secondPayment = createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
      secondPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      secondPayment.data.getOwnerPaymentAmount shouldBe AP
      newState.entry.payments.zip(Seq(firstPayment, secondPayment)).map {
        case (actual, expected) => checkEquality(actual, expected)
      }
    }

    "correctly calculate amounts for short rent period in one day" in {
      val rentStartDate = dt(2021, 3, 30)
      val nowMoment = dt(2021, 4, 20)
      val contract = createContract(rentStartDate, 1, nowMoment = Some(nowMoment))
      val contractWithPayments = ContractWithPayments(contract, Nil)
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.size should be(3)
      val first :: second :: third :: Nil = newState.entry.payments
      assertPaymentDates(first, dt(2021, 3, 30), dt(2021, 4, 29))
      first.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles(AP * (1 + K1) + S))
      first.data.getOwnerPaymentAmount should be(AP)
      assertPaymentDates(second, dt(2021, 4, 30), dt(2021, 4, 30), InitiallyShortType)
      // todo: should be reverted later when users accept new pricing conditions
//      second.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles(AP / 30.0f * (1 + K1) + S))
      second.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles((AP * (1 + K1) + S) / 30.0f))
      second.data.getOwnerPaymentAmount should be(Money.roundFloatKopecksToRubles(AP / 30.0f))
      assertPaymentDates(third, dt(2021, 5, 1), dt(2021, 5, 31))
      third.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles(AP * (1 + K1) + S))
      third.data.getOwnerPaymentAmount should be(AP)
    }

    "clear payments after termination day if last day" in {
      val rentStartDate = dt(2021, 3, 10)
      val terminationDate = dt(2021, 4, 9)
      val contract = createContract(rentStartDate, 10, Some(terminationDate), nowMoment = Some(rentStartDate))
      val firstPayment = createPayment(contract, rentStartDate, rentStartDate.plusMonths(1).minusDays(1))
      val secondPayment = createPayment(contract, firstPayment.endTime.plusDays(1), firstPayment.endTime.plusMonths(1))
      val payments = List(firstPayment, secondPayment)
      val contractWithPayments = ContractWithPayments(contract, payments)
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.size should be(1)
      val first :: Nil = newState.entry.payments
      assertPaymentDates(first, dt(2021, 3, 10), dt(2021, 4, 9))
      first.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles(AP * (1 + K1) + S))
      first.data.getOwnerPaymentAmount should be(AP)
    }

    "update current unpaid payment in middle of period" in {
      val rentStartDate = dt(2021, 3, 10)
      val terminationDate = dt(2021, 3, 25)
      val contract = createContract(
        rentStartDate,
        rentStartDate.getDayOfMonth,
        Some(terminationDate),
        nowMoment = Some(rentStartDate)
      )
      val payments = List(createPayment(contract, rentStartDate, rentStartDate.plusMonths(1)))
      val contractWithPayments = ContractWithPayments(contract, payments)
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.size should be(1)
      val first :: Nil = newState.entry.payments
      assertPaymentDates(first, dt(2021, 3, 10), dt(2021, 3, 25), TerminationShortType)
      // todo: should be reverted later when users accept new pricing conditions
//      first.data.getTenantPaymentAmount should be(683000)
      first.data.getTenantPaymentAmount should be(614800)
      first.data.getOwnerPaymentAmount should be(516100)
    }

    "correct payment for changed termination date" in {
      val rentStartDate = dt(2021, 3, 10)
      val previousTerminationDate = dt(2021, 4, 20)
      val newTerminationDate = dt(2021, 4, 27)
      val contract = createContract(
        rentStartDate,
        rentStartDate.getDayOfMonth,
        Some(newTerminationDate),
        nowMoment = Some(rentStartDate)
      )
      val payments = List(
        createPayment(contract, rentStartDate, rentStartDate.plusMonths(1).minusDays(1)),
        createPayment(contract, dt(2021, 4, 10), previousTerminationDate, TerminationShortType)
      )
      val contractWithPayments = ContractWithPayments(contract, payments)
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.size should be(2)

      val first :: second :: Nil = newState.entry.payments

      assertPaymentDates(first, dt(2021, 3, 10), dt(2021, 4, 9))
      first.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles(AP * (1 + K1) + S))
      first.data.getOwnerPaymentAmount should be(AP)

      assertPaymentDates(second, dt(2021, 4, 10), newTerminationDate, TerminationShortType)
      // todo: should be reverted later when users accept new pricing conditions
//      second.data.getTenantPaymentAmount should be(771100)
      second.data.getTenantPaymentAmount should be(714700)
      second.data.getOwnerPaymentAmount should be(600000)
    }

    "create payment for changed termination date" in {
      val rentStartDate = dt(2021, 3, 10)
      val previousTerminationDate = dt(2021, 3, 27)
      val newTerminationDate = dt(2021, 4, 27)
      val contract = createContract(
        rentStartDate,
        rentStartDate.getDayOfMonth,
        Some(newTerminationDate),
        nowMoment = Some(rentStartDate)
      )
      val payments = List(
        createPayment(contract, rentStartDate, previousTerminationDate, TerminationShortType)
      )
      val contractWithPayments = ContractWithPayments(contract, payments)
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.size should be(2)

      val first :: second :: Nil = newState.entry.payments

      assertPaymentDates(first, dt(2021, 3, 10), dt(2021, 4, 9))
      first.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles(AP * (1 + K1) + S))
      first.data.getOwnerPaymentAmount should be(AP)

      assertPaymentDates(second, dt(2021, 4, 10), newTerminationDate, TerminationShortType)
      // todo: should be reverted later when users accept new pricing conditions
//      second.data.getTenantPaymentAmount should be(771100)
      second.data.getTenantPaymentAmount should be(714700)
      second.data.getOwnerPaymentAmount should be(600000)
    }

    "create payment after termination cancelling" in {
      val rentStartDate = dt(2021, 3, 10)
      val previousTerminationDate = dt(2021, 3, 27)
      val contract = createContract(rentStartDate, rentStartDate.getDayOfMonth, nowMoment = Some(rentStartDate))
      val payments = List(
        createPayment(contract, rentStartDate, previousTerminationDate, TerminationShortType)
      )
      val contractWithPayments = ContractWithPayments(contract, payments)
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.size should be(2)

      val first :: second :: Nil = newState.entry.payments

      assertPaymentDates(first, dt(2021, 3, 10), dt(2021, 4, 9))
      first.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles(AP * (1 + K1) + S))
      first.data.getOwnerPaymentAmount should be(AP)

      assertPaymentDates(second, dt(2021, 4, 10), dt(2021, 5, 9))
      first.data.getTenantPaymentAmount should be(Money.roundFloatKopecksToRubles(AP * (1 + K1) + S))
      first.data.getOwnerPaymentAmount should be(AP)
    }

    "recreate payments after adding contractual agreement" in {
      val now = dt(2020, 3, 1)
      val contractWithoutHistory = createContract(TodayDate, 10, nowMoment = Some(now))
      val historyItem = createContractHistoryItem(contractWithoutHistory, TodayDate, TodayDate.plusMonths(1), 10)
      val contractWithHistory = contractWithoutHistory
        .withHistory(historyItem)
        .withPaymentDayOfMonth(25)
      val payments = List(
        createPayment(contractWithoutHistory, TodayDate, dt(2020, 2, 9)),
        createPayment(contractWithoutHistory, dt(2020, 2, 10), dt(2020, 3, 9)),
        createPayment(contractWithoutHistory, dt(2020, 3, 10), dt(2020, 4, 9))
      )
      val contractWithPayments = ContractWithPayments(contractWithHistory, payments)
      val newState = invokeStage(contractWithPayments)

      newState.entry.payments.size shouldBe 4
      val first :: second :: third :: fourth :: Nil = newState.entry.payments
      assertPaymentDates(first, TodayDate, dt(2020, 2, 9))
      assertPaymentDates(second, dt(2020, 2, 10), dt(2020, 2, 24), SHORT_MOVED_PAYMENT_DAY_OF_MONTH)
      assertPaymentDates(third, dt(2020, 2, 25), dt(2020, 3, 24))
      assertPaymentDates(fourth, dt(2020, 3, 25), dt(2020, 4, 24))
    }

    "recreate payments after adding second contractual agreement" in {
      val now = dt(2020, 3, 1)
      val contractWithoutHistory = createContract(TodayDate, 10, nowMoment = Some(now))

      val firstHistoryItem = createContractHistoryItem(contractWithoutHistory, TodayDate, dt(2020, 2, 10), 10)
      val firstContractWithHistory = contractWithoutHistory
        .withHistory(firstHistoryItem)
        .withPaymentDayOfMonth(25)
      val secondHistoryItem = createContractHistoryItem(contractWithoutHistory, dt(2020, 2, 10), dt(2020, 3, 25), 25)
      val secondContractWithHistory = firstContractWithHistory
        .withHistory(secondHistoryItem)
        .withPaymentDayOfMonth(5)

      val payments = List(
        createPayment(contractWithoutHistory, TodayDate, dt(2020, 2, 9)),
        createPayment(firstContractWithHistory, dt(2020, 2, 10), dt(2020, 2, 24), SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
        createPayment(firstContractWithHistory, dt(2020, 2, 25), dt(2020, 3, 24)),
        createPayment(firstContractWithHistory, dt(2020, 3, 25), dt(2020, 4, 24))
      )
      val contractWithPayments = ContractWithPayments(secondContractWithHistory, payments)
      val newState = invokeStage(contractWithPayments)

      newState.entry.payments.size shouldBe 4
      val first :: second :: third :: fourth :: Nil = newState.entry.payments
      assertPaymentDates(first, TodayDate, dt(2020, 2, 9))
      assertPaymentDates(second, dt(2020, 2, 10), dt(2020, 2, 24), SHORT_MOVED_PAYMENT_DAY_OF_MONTH)
      assertPaymentDates(third, dt(2020, 2, 25), dt(2020, 3, 24))
      assertPaymentDates(fourth, dt(2020, 3, 25), dt(2020, 4, 4), SHORT_MOVED_PAYMENT_DAY_OF_MONTH)
    }

    "recreate payments after adding second contractual agreement right after the first one" in {
      val now = dt(2020, 3, 1)
      val contractWithoutHistory = createContract(TodayDate, 10, nowMoment = Some(now))
      val firstHistoryItem = createContractHistoryItem(contractWithoutHistory, TodayDate, dt(2020, 2, 10), 10)
      val secondHistoryItem = createContractHistoryItem(contractWithoutHistory, dt(2020, 2, 10), dt(2020, 2, 25), 25)
      val firstContractWithHistory = contractWithoutHistory
        .withHistory(firstHistoryItem)
        .withPaymentDayOfMonth(25)
      val secondContractWithHistory = firstContractWithHistory
        .withHistory(secondHistoryItem)
        .withPaymentDayOfMonth(5)
      val payments = List(
        createPayment(contractWithoutHistory, TodayDate, dt(2020, 2, 9)),
        createPayment(firstContractWithHistory, dt(2020, 2, 10), dt(2020, 2, 24), SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
        createPayment(firstContractWithHistory, dt(2020, 2, 25), dt(2020, 3, 24)),
        createPayment(firstContractWithHistory, dt(2020, 3, 25), dt(2020, 4, 24))
      )
      val contractWithPayments = ContractWithPayments(secondContractWithHistory, payments)
      val newState = invokeStage(contractWithPayments)

      newState.entry.payments.size shouldBe 5
      val first :: second :: third :: fourth :: fifth :: Nil = newState.entry.payments
      assertPaymentDates(first, TodayDate, dt(2020, 2, 9)) // 1
      assertPaymentDates(second, dt(2020, 2, 10), dt(2020, 2, 24), SHORT_TERMINATION) // 2
      assertPaymentDates(third, dt(2020, 2, 25), dt(2020, 3, 4), SHORT_MOVED_PAYMENT_DAY_OF_MONTH)
      assertPaymentDates(fourth, dt(2020, 3, 5), dt(2020, 4, 4))
      assertPaymentDates(fifth, dt(2020, 4, 5), dt(2020, 5, 4))
    }

    "correct payment for changed termination date after added contractual agreement" in {
      val now = dt(2020, 3, 1)
      val contractWithoutHistory = createContract(TodayDate, 10, nowMoment = Some(now))
      val historyItem = createContractHistoryItem(contractWithoutHistory, TodayDate, TodayDate.plusMonths(1), 10)
      val contractWithHistory = contractWithoutHistory
        .withHistory(historyItem)
        .withPaymentDayOfMonth(25)
      val payments = List(
        createPayment(contractWithoutHistory, TodayDate, dt(2020, 2, 9)),
        createPayment(contractWithHistory, dt(2020, 2, 10), dt(2020, 2, 24), SHORT_MOVED_PAYMENT_DAY_OF_MONTH),
        createPayment(contractWithHistory, dt(2020, 2, 25), dt(2020, 3, 24)),
        createPayment(contractWithHistory, dt(2020, 3, 25), dt(2020, 4, 24))
      )
      val fixedTermContract = contractWithHistory.copy(terminationDate = Some(dt(2020, 3, 20)))
      val contractWithPayments = ContractWithPayments(fixedTermContract, payments)
      val newState = invokeStage(contractWithPayments)

      newState.entry.payments.size shouldBe 3
      val first :: second :: third :: Nil = newState.entry.payments
      assertPaymentDates(first, TodayDate, dt(2020, 2, 9))
      assertPaymentDates(second, dt(2020, 2, 10), dt(2020, 2, 24), SHORT_MOVED_PAYMENT_DAY_OF_MONTH)
      assertPaymentDates(third, dt(2020, 2, 25), dt(2020, 3, 20), TerminationShortType)
    }

    // This is the strangest case, which mustn't happen anywhere (nor testing, nor production).
    // Covering it just to be sure.
    "not change paid payment if conditions were changed unexpectedly" in {
      val now = dt(2020, 1, 15)
      val contractWithoutHistory = createContract(TodayDate, 10, nowMoment = Some(now))
      val historyItem = createContractHistoryItem(contractWithoutHistory, TodayDate, TodayDate.plusMonths(1), 10)
      val contractWithHistory = contractWithoutHistory
        .withHistory(historyItem)
        .withPaymentDayOfMonth(25)
      val payments = List(
        createPayment(contractWithoutHistory, TodayDate, dt(2020, 2, 9)).copy(status = PaymentStatus.PaidToOwner),
        createPayment(contractWithoutHistory, dt(2020, 2, 10), dt(2020, 3, 9)).copy(status = PaymentStatus.PaidToOwner)
      )
      val contractWithPayments = ContractWithPayments(contractWithHistory, payments)
      val newState = invokeStage(contractWithPayments)

      newState.entry.payments.size shouldBe 2
      val first :: second :: Nil = newState.entry.payments
      assertPaymentDates(first, TodayDate, dt(2020, 2, 9))
      assertPaymentDates(second, dt(2020, 2, 10), dt(2020, 3, 9))
    }

    "update payment, that was unpaid for 3 days, with the penalty for the contract with guaranteed payout" in {
      val secondPaymentDate = TodayDate.minusDays(3)
      val firstPaymentDate = secondPaymentDate.minusMonths(1)
      val contract = createContract(
        firstPaymentDate,
        firstPaymentDate.getDayOfMonth,
        nowMoment = Some(TodayDate),
        usePayoutUnderGuarantee = true
      )
      val firstPayment = createPayment(contract, firstPaymentDate, firstPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidToOwner, isPaidOutUnderGuarantee = true)
      firstPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstPayment.data.getOwnerPaymentAmount shouldBe AP
      val secondPayment = createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidOutUnderGuarantee, isPaidOutUnderGuarantee = true)
      secondPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      secondPayment.data.getOwnerPaymentAmount shouldBe AP
      val contractWithPayments = ContractWithPayments(contract, List(firstPayment, secondPayment))
      val newState = invokeStage(contractWithPayments)

      val expectedSecondPayment = secondPayment.copy(
        data = secondPayment.data.toBuilder
          .setTenantPaymentAmount((AP * (1.0f + K1) + S + 0.005f * AP).round)
          .setTenantPenaltyAmount((0.005f * AP).round)
          .build()
      )
      newState.entry.payments.head shouldBe firstPayment
      newState.entry.payments(1) shouldBe expectedSecondPayment
    }

    "update payment, that was unpaid for 25 days, with the penalty for the contract with guaranteed payout" in {
      val secondPaymentDate = TodayDate.minusDays(25)
      val firstPaymentDate = secondPaymentDate.minusMonths(1)
      val contract = createContract(
        firstPaymentDate,
        firstPaymentDate.getDayOfMonth,
        nowMoment = Some(TodayDate),
        usePayoutUnderGuarantee = true
      )
      val firstPayment = createPayment(contract, firstPaymentDate, firstPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidToOwner, isPaidOutUnderGuarantee = true)
      firstPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstPayment.data.getOwnerPaymentAmount shouldBe AP
      val secondPayment = createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidOutUnderGuarantee, isPaidOutUnderGuarantee = true)
      secondPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      secondPayment.data.getOwnerPaymentAmount shouldBe AP
      val contractWithPayments = ContractWithPayments(contract, List(firstPayment, secondPayment))
      val newState = invokeStage(contractWithPayments)

      val expectedSecondPayment = secondPayment.copy(
        data = secondPayment.data.toBuilder
          .setTenantPaymentAmount((AP * (1.0f + K1) + S + 12 * 0.005f * AP).round)
          .setTenantPenaltyAmount((12 * 0.005f * AP).round)
          .build()
      )
      newState.entry.payments.head shouldBe firstPayment
      newState.entry.payments(1) shouldBe expectedSecondPayment
    }

    "not update payment, that was paid at 7 day after payment date for the contract with guaranteed payout" in {
      val secondPaymentDate = TodayDate.minusDays(15)
      val firstPaymentDate = secondPaymentDate.minusMonths(1)
      val contract = createContract(
        firstPaymentDate,
        firstPaymentDate.getDayOfMonth,
        nowMoment = Some(TodayDate),
        usePayoutUnderGuarantee = true
      )
      val firstPayment = createPayment(contract, firstPaymentDate, firstPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidToOwner, isPaidOutUnderGuarantee = true)
      firstPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstPayment.data.getOwnerPaymentAmount shouldBe AP
      val secondTemplatePayment =
        createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
      val secondPayment = secondTemplatePayment.copy(
        status = PaymentStatus.PaidToOwner,
        isPaidOutUnderGuarantee = true,
        data = secondTemplatePayment.data.toBuilder
          .setTenantPaymentAmount((AP * (1.0f + K1) + S + 5 * 0.005f * AP).round)
          .build()
      )
      secondPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S + 5 * 0.005f * AP).round
      secondPayment.data.getOwnerPaymentAmount shouldBe AP
      val contractWithPayments = ContractWithPayments(contract, List(firstPayment, secondPayment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.head shouldBe firstPayment
      newState.entry.payments(1) shouldBe secondPayment
    }

    "not update unpaid payment before the starting penalty date for the contract with guaranteed payout" in {
      val secondPaymentDate = TodayDate.minusDays(2)
      val firstPaymentDate = secondPaymentDate.minusMonths(1)
      val contract = createContract(
        firstPaymentDate,
        firstPaymentDate.getDayOfMonth,
        nowMoment = Some(TodayDate),
        usePayoutUnderGuarantee = true
      )
      val firstPayment = createPayment(contract, firstPaymentDate, firstPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidToOwner, isPaidOutUnderGuarantee = true)
      firstPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstPayment.data.getOwnerPaymentAmount shouldBe AP
      val secondPayment = createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidOutUnderGuarantee, isPaidOutUnderGuarantee = true)
      secondPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      secondPayment.data.getOwnerPaymentAmount shouldBe AP
      val contractWithPayments = ContractWithPayments(contract, List(firstPayment, secondPayment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.head shouldBe firstPayment
      newState.entry.payments(1) shouldBe secondPayment
    }

    "not update payment, that was unpaid for 3 days, for the contract without guaranteed payout" in {
      val secondPaymentDate = TodayDate.minusDays(15)
      val firstPaymentDate = secondPaymentDate.minusMonths(1)
      val contract = createContract(
        firstPaymentDate,
        firstPaymentDate.getDayOfMonth,
        nowMoment = Some(TodayDate),
        usePayoutUnderGuarantee = true
      )
      val firstPayment = createPayment(contract, firstPaymentDate, firstPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.PaidToOwner, isPaidOutUnderGuarantee = true)
      firstPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      firstPayment.data.getOwnerPaymentAmount shouldBe AP
      val secondPayment = createPayment(contract, secondPaymentDate, secondPaymentDate.plusMonths(1).minusDays(1))
        .copy(status = PaymentStatus.New)
      secondPayment.data.getTenantPaymentAmount shouldBe (AP * (1.0f + K1) + S).round
      secondPayment.data.getOwnerPaymentAmount shouldBe AP
      val contractWithPayments = ContractWithPayments(contract, List(firstPayment, secondPayment))
      val newState = invokeStage(contractWithPayments)
      newState.entry.payments.head shouldBe firstPayment
      newState.entry.payments(1) shouldBe secondPayment
    }
  }

  private def assertPaymentDates(
    payment: Payment,
    startDate: DateTime,
    endDate: DateTime,
    fullnessType: FullnessType = FullnessType.FULL
  ): Unit = {
    payment.startTime shouldEqual startDate
    payment.endTime shouldEqual endDate
    payment.data.getRentPayment.getFullnessType shouldEqual fullnessType
  }

  private def checkEquality(actual: Payment, expected: Payment): Unit = {
    actual.id shouldBe expected.id
    actual.contractId shouldBe expected.contractId
    actual.`type` shouldBe expected.`type`
    actual.isPaidOutUnderGuarantee shouldBe expected.isPaidOutUnderGuarantee
    actual.paymentDate shouldBe expected.paymentDate
    actual.startTime shouldBe expected.startTime
    actual.endTime shouldBe expected.endTime
    actual.status shouldBe expected.status
    actual.data shouldBe expected.data
  }
}
