package ru.yandex.realty.rent.backend.payment

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.TestUtil.dt
import ru.yandex.realty.rent.backend.InsuranceManager.calculateRentInsuranceInKopecks
import ru.yandex.realty.rent.backend.RentPaymentsData
import ru.yandex.realty.rent.model.RentContract
import ru.yandex.realty.rent.model.enums.ContractStatus
import ru.yandex.realty.rent.proto.model.contract.CalculationStrategyNamespace.CalculationStrategy
import ru.yandex.realty.rent.proto.model.contract.{ChangedContractFields, Insurance, TemporaryAmounts}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ConditionPeriodsGeneratorSpec extends AsyncSpecBase with RentPaymentsData {

  import RentPaymentsData._

  val generator: ConditionPeriodsGenerator = DefaultConditionPeriodsGenerator

  private val conditionTemplate = RentPaymentConditions(
    rentAmount = RentAmount,
    rentStartDate = TodayDate,
    paymentDayOfMonth = TodayDate.getDayOfMonth,
    insuranceAmount = InsuranceAmount,
    calculationStrategy = CalculationStrategy.STRATEGY_2,
    commissions = DefaultCommissions
  )
  private val termOptionsTemplate = TerminationAttributes(
    endDate = TodayDate.plusMonths(3),
    notificationDate = TodayDate.plusMonths(1),
    tenantRefusedPayFor30Days = true,
    checkOutWithoutAdditionalPayments = false
  )

  "ConditionPeriodsGenerator" should {

    "generate single condition period from contract without history" in {
      // 1st period: 10.01 - ...

      val contract = createContract(TodayDate, TodayDate.getDayOfMonth)

      val Seq(first) = generator.buildConditionPeriods(contract)
      first shouldBe LastConditionPeriod(
        startDate = TodayDate,
        conditions = conditionTemplate,
        terminationAttributes = None
      )
    }

    "generate condition periods from contract without history with temporary conditions" in {
      // 1st period: 10.01 - 10.03
      // 2st period: 10.03 - ...
      val tempConditions = TemporaryRentConditions(NewRentAmount, None, 2)
      val contract = createContract(TodayDate, TodayDate.getDayOfMonth, temporaryRentConditions = Some(tempConditions))

      val Seq(temporary, current) = generator.buildConditionPeriods(contract)
      temporary shouldBe RegularConditionPeriod(
        startDate = TodayDate,
        conditions = conditionTemplate.copy(rentAmount = tempConditions.rentAmount),
        next = current
      )
      current shouldBe LastConditionPeriod(
        startDate = TodayDate.plusMonths(2),
        conditions = conditionTemplate,
        terminationAttributes = None
      )
    }

    "generate single condition period from contract without history with termination" in {
      // 1st period: 10.01 - 15.0

      val terminationDate = TodayDate.plusMonths(1).plusDays(5)

      val contract = createContract(
        rentStartDate = TodayDate,
        paymentDayOfMonth = 20,
        terminationDate = Some(terminationDate),
        notificationDate = Some(terminationDate)
      )

      val Seq(first) = generator.buildConditionPeriods(contract)

      first shouldBe LastConditionPeriod(
        startDate = TodayDate,
        conditions = conditionTemplate.copy(paymentDayOfMonth = 20),
        terminationAttributes = Some(
          termOptionsTemplate.copy(
            endDate = terminationDate plusDays 1,
            notificationDate = terminationDate
          )
        )
      )
    }

    "generate single condition period from contract without history with temporary conditions and termination" in {
      // 1st period: 10.01 - 15.02

      val temporaryConditions = TemporaryRentConditions(5000000, None, 2)

      val terminationDate = TodayDate.plusMonths(1).plusDays(5)

      val contract = createContract(
        rentStartDate = TodayDate,
        paymentDayOfMonth = 20,
        terminationDate = Some(terminationDate),
        notificationDate = Some(terminationDate),
        temporaryRentConditions = Some(temporaryConditions)
      )

      val Seq(firstTemporary) = generator.buildConditionPeriods(contract)
      assertPeriodForTemporaryAmounts(firstTemporary, contract)
    }

    "generate several condition periods from contract with history without temporary conditions" in {
      // 1st period: 10.01 - 10.02
      // 2nd period: 10.02 - 15.03
      // 3nd period: 15.03 - ...

      val firstPeriodEndDate = TodayDate.plusMonths(1)
      val secondPeriodEndDate = firstPeriodEndDate.plusMonths(1).plusDays(15)

      val contract = createContract(TodayDate, 5)
      val firstHistoryItem = createContractHistoryItem(contract, TodayDate, firstPeriodEndDate, 10)
      val secondHistoryItem = createContractHistoryItem(contract, firstPeriodEndDate, secondPeriodEndDate, 25)
      val contractWithHistory = contract.withHistory(firstHistoryItem, secondHistoryItem)

      val Seq(first, second, last) = generator.buildConditionPeriods(contractWithHistory)

      assertPeriod(first, firstHistoryItem)
      assertPeriod(second, secondHistoryItem)
      assertPeriod(last, contractWithHistory)
    }

    "generate several condition from contractual agreements with rent change and payment day movement" in {
      // 1st period: 22.06 - 22.08
      // 2nd period: 22.08 - 22.09
      // 3nd period: 22.09 - 30.09
      // 4nd period: 30.09 - 30.10
      // 5nd period: 30.11 - ...
      val temporaryConditions = TemporaryRentConditions(4000000, None, 1)
      val contract = createContract(
        rentStartDate = dt(2022, 6, 22),
        paymentDayOfMonth = 30,
        temporaryRentConditions = Some(temporaryConditions)
      )
      val firstHistoryItem = createContractHistoryItem(contract, dt(2022, 6, 22), dt(2022, 9, 22), 22)
      val firstTemporaryAmounts = TemporaryAmounts
        .newBuilder()
        .setRentAmount(5000000)
        .setDuration(2)
        .setStartDate(dt(2022, 6, 22))
        .build()
      val firstHistoryItemWithTemporaryAmounts = firstHistoryItem.toBuilder
        .setFields(firstHistoryItem.getFields.toBuilder.setTemporaryAmounts(firstTemporaryAmounts))
        .build()

      val secondHistoryItem = createContractHistoryItem(contract, dt(2022, 9, 22), dt(2022, 9, 30), 30)

      val contractWithHistory =
        contract.withHistory(firstHistoryItemWithTemporaryAmounts, secondHistoryItem)

      val Seq(first, second, third, forth, fifth) = generator.buildConditionPeriods(contractWithHistory)
      assertPeriodForTemporaryAmounts(first, firstHistoryItemWithTemporaryAmounts)
      assertPeriod(second, firstHistoryItemWithTemporaryAmounts)
      assertPeriod(third, secondHistoryItem)
      assertPeriodForTemporaryAmounts(forth, contractWithHistory)
      assertPeriod(fifth, contractWithHistory)
    }

    "generate several condition from contractual agreements with temporary rent change and payment day movement" in {
      // 1st period: 22.06 - 22.09
      // 3nd period: 22.09 - 30.09
      // 4nd period: 30.09 - 30.10
      // 5nd period: 30.11 - ...
      val temporaryConditions = TemporaryRentConditions(4000000, None, 1)
      val contract = createContract(
        rentStartDate = dt(2022, 6, 22),
        paymentDayOfMonth = 30,
        temporaryRentConditions = Some(temporaryConditions)
      )
      val firstHistoryItem = createContractHistoryItem(contract, dt(2022, 6, 22), dt(2022, 9, 22), 22)
      val firstTemporaryAmounts = TemporaryAmounts
        .newBuilder()
        .setRentAmount(5000000)
        .setDuration(3)
        .setStartDate(dt(2022, 6, 22))
        .build()
      val firstHistoryItemWithTemporaryAmounts = firstHistoryItem.toBuilder
        .setFields(firstHistoryItem.getFields.toBuilder.setTemporaryAmounts(firstTemporaryAmounts))
        .build()

      val secondHistoryItem = createContractHistoryItem(contract, dt(2022, 9, 22), dt(2022, 9, 30), 30)

      val contractWithHistory =
        contract.withHistory(firstHistoryItemWithTemporaryAmounts, secondHistoryItem)

      val Seq(first, third, forth, fifth) = generator.buildConditionPeriods(contractWithHistory)
      assertPeriodForTemporaryAmounts(first, firstHistoryItemWithTemporaryAmounts)
      assertPeriod(third, secondHistoryItem)
      assertPeriodForTemporaryAmounts(forth, contractWithHistory)
      assertPeriod(fifth, contractWithHistory)
    }

    "generate several condition periods from contract with history and temporary rent conditions in history and contract" in {
      // 1st period: 10.01 - 10.03 - temporary rent amount=10010, insurance = 141100 - 1st history item
      // 2st period: 10.03 - 10.04 - regular rent amount=10000, insurance = 141100   - 1st history item
      // 3nd period: 10.04 - 10.05 - temporary rent amount=31000, insurance = 81100  - 2st history item
      // 4nd period: 10.05 - 15.07 - regular rent amount=10000, insurance = 141100   - 2st history item
      //  no period: 15.07 - 15.07 - regular rent amount=10000, insurance = 141100   - 3st history item
      // 5nd period: 15.07 - 15.08 - temporary rent amount=10010, insurance = 141100 - contract
      // 6nd period: 15.08 - ...   - regular rent amount=10000, insurance = 141100   - contract

      val firstPeriodEndDate = TodayDate.plusMonths(3)
      val secondPeriodEndDate = firstPeriodEndDate.plusMonths(3).plusDays(5)

      val temporaryRentConditions = TemporaryRentConditions(NewRentAmount, None, 1)
      val contract = createContract(TodayDate, 5, temporaryRentConditions = Some(temporaryRentConditions))

      val firstHistoryItem = createContractHistoryItem(contract, TodayDate, firstPeriodEndDate, 10)
      val firstTemporaryAmounts = TemporaryAmounts
        .newBuilder()
        .setRentAmount(NewRentAmount)
        .setDuration(2)
        .build()
      val firstHistoryItemWithTemporaryAmounts = firstHistoryItem.toBuilder
        .setFields(firstHistoryItem.getFields.toBuilder.setTemporaryAmounts(firstTemporaryAmounts))
        .build()

      val secondHistoryItem = createContractHistoryItem(contract, firstPeriodEndDate, secondPeriodEndDate, 25)
      val secondTemporaryAmounts = TemporaryAmounts
        .newBuilder()
        .setRentAmount(RentAmountForInsuranceChanged)
        .setInsurance(
          Insurance
            .newBuilder()
            .setInsuranceAmount(calculateRentInsuranceInKopecks(RentAmountForInsuranceChanged))
            .build()
        )
        .setDuration(1)
        .build()
      val secondHistoryItemWithTemporaryAmounts = secondHistoryItem.toBuilder
        .setFields(firstHistoryItem.getFields.toBuilder.setTemporaryAmounts(secondTemporaryAmounts))
        .build()
      val thirdHistoryItem = createContractHistoryItem(contract, secondPeriodEndDate, secondPeriodEndDate, 25)
      val thirdHistoryItemWithTemporaryAmounts = thirdHistoryItem.toBuilder
        .setFields(firstHistoryItem.getFields.toBuilder.setTemporaryAmounts(secondTemporaryAmounts))
        .build()

      val contractWithHistory = contract.withHistory(
        firstHistoryItemWithTemporaryAmounts,
        secondHistoryItemWithTemporaryAmounts,
        thirdHistoryItemWithTemporaryAmounts
      )
      val Seq(firstTemporary, first, secondTemporary, second, lastTemporary, last) =
        generator.buildConditionPeriods(contractWithHistory)

      assertPeriod(first, firstHistoryItemWithTemporaryAmounts)
      assertPeriodForTemporaryAmounts(firstTemporary, firstHistoryItemWithTemporaryAmounts)

      assertPeriod(second, secondHistoryItemWithTemporaryAmounts)
      assertPeriodForTemporaryAmounts(secondTemporary, secondHistoryItemWithTemporaryAmounts)

      assertPeriod(last, contractWithHistory)
      assertPeriodForTemporaryAmounts(lastTemporary, contractWithHistory)

    }

    "generate several condition periods from contract with history and temporary rent conditions only in contract" in {
      // 1st contractual agreement period: 10.01 - 10.03
      // no period for: 10.03 - 10.03
      // 3nd contractual agreement period: 10.03 - 15.04
      // 4nd contract period: 15.04 - 15.05
      // 4nd contract period: 15.05 - ...

      val firstPeriodEndDate = TodayDate.plusMonths(2)
      val secondPeriodEndDate = firstPeriodEndDate.plusMonths(1).plusDays(15)

      val temporaryRentConditions = TemporaryRentConditions(12300L, None, 1)
      val contract = createContract(TodayDate, 5, temporaryRentConditions = Some(temporaryRentConditions))
      val firstHistoryItem = createContractHistoryItem(contract, TodayDate, firstPeriodEndDate, 10)
      val shortHistoryItem = createContractHistoryItem(contract, firstPeriodEndDate, firstPeriodEndDate, 25)
      val secondHistoryItem = createContractHistoryItem(contract, firstPeriodEndDate, secondPeriodEndDate, 25)
      val contractWithHistory = contract.withHistory(firstHistoryItem, shortHistoryItem, secondHistoryItem)

      val Seq(first, second, lastTemporary, last) = generator.buildConditionPeriods(contractWithHistory)

      assertPeriod(first, firstHistoryItem)
      assertPeriod(second, secondHistoryItem)
      assertPeriod(last, contractWithHistory)
      assertPeriodForTemporaryAmounts(lastTemporary, contractWithHistory)
    }

    "generate several condition periods from contract with history and temporary rent conditions only in history" in {
      // 1st period: 10.01 - 10.03 - temporary rent amount=10010, insurance = 141100 - 1st history item
      // 2st period: 10.03 - 10.04 - regular rent amount=10000, insurance = 141100   - 1st history item
      // 3nd period: 10.04 - 10.05 - temporary rent amount=31000, insurance = 81100  - 2st history item
      // 4nd period: 10.05 - 15.07 - regular rent amount=10000, insurance = 141100   - 2st history item
      // 5nd period: 15.07 - ...   - regular rent amount=10000, insurance = 141100   - contract

      val firstPeriodEndDate = TodayDate.plusMonths(3)
      val secondPeriodEndDate = firstPeriodEndDate.plusMonths(3).plusDays(5)

      val contract = createContract(TodayDate, 5)

      val firstHistoryItem = createContractHistoryItem(contract, TodayDate, firstPeriodEndDate, 10)
      val firstTemporaryAmounts = TemporaryAmounts
        .newBuilder()
        .setRentAmount(NewRentAmount)
        .setDuration(2)
        .build()
      val firstHistoryItemWithTemporaryAmounts = firstHistoryItem.toBuilder
        .setFields(firstHistoryItem.getFields.toBuilder.setTemporaryAmounts(firstTemporaryAmounts))
        .build()

      val secondHistoryItem = createContractHistoryItem(contract, firstPeriodEndDate, secondPeriodEndDate, 25)
      val secondTemporaryAmounts = TemporaryAmounts
        .newBuilder()
        .setRentAmount(RentAmountForInsuranceChanged)
        .setInsurance(
          Insurance
            .newBuilder()
            .setInsuranceAmount(calculateRentInsuranceInKopecks(RentAmountForInsuranceChanged))
            .build()
        )
        .setDuration(1)
        .build()
      val secondHistoryItemWithTemporaryAmounts = secondHistoryItem.toBuilder
        .setFields(firstHistoryItem.getFields.toBuilder.setTemporaryAmounts(secondTemporaryAmounts))
        .build()

      val contractWithHistory =
        contract.withHistory(firstHistoryItemWithTemporaryAmounts, secondHistoryItemWithTemporaryAmounts)
      val Seq(firstTemporary, first, secondTemporary, second, last) =
        generator.buildConditionPeriods(contractWithHistory)

      assertPeriod(first, firstHistoryItemWithTemporaryAmounts)
      assertPeriodForTemporaryAmounts(firstTemporary, firstHistoryItemWithTemporaryAmounts)

      assertPeriod(second, secondHistoryItemWithTemporaryAmounts)
      assertPeriodForTemporaryAmounts(secondTemporary, secondHistoryItemWithTemporaryAmounts)

      assertPeriod(last, contractWithHistory)
    }

    "generate final condition period from contract with termination date" in {
      // 1st period: 10.01 - 10.02
      // 2nd period: 10.02 - 15.03

      val firstPeriodEndDate = TodayDate.plusMonths(1)
      val terminationDate = firstPeriodEndDate.plusMonths(1).plusDays(5)

      val contract = createContract(TodayDate, 20, Some(terminationDate), notificationDate = Some(terminationDate))
      val historyItem = createContractHistoryItem(contract, TodayDate, firstPeriodEndDate, 10)
      val contractWithHistory = contract.withHistory(historyItem)

      val Seq(first, last) = generator.buildConditionPeriods(contractWithHistory)
      first shouldBe RegularConditionPeriod(
        startDate = TodayDate,
        conditions = conditionTemplate,
        next = last
      )
      last shouldBe LastConditionPeriod(
        startDate = firstPeriodEndDate,
        conditions = conditionTemplate.copy(paymentDayOfMonth = 20),
        terminationAttributes = Some(
          termOptionsTemplate.copy(
            endDate = terminationDate plusDays 1,
            notificationDate = terminationDate
          )
        )
      )
    }

    "generate condition periods from contract with history with temporary conditions and termination" in {
      // 1st period: 15.06 - 15.07 - rent amount=23423             - 1st history item
      // 2st period: 15.07 - 15.08 - temporary rent amount=60000   - 2st history item
      // 3nd period: 15.08 - 15.10 - rent amount=23423             - 2st history item
      // 4nd period: 15.10 - 26.10 - temporary rent amount=60000   - contract

      val contract = createContract(
        rentStartDate = dt(2022, 6, 15),
        paymentDayOfMonth = 15,
        terminationDate = Some(dt(2022, 10, 26)),
        notificationDate = Some(dt(2022, 10, 16)),
        nowMoment = Some(dt(2022, 10, 16)),
        status = ContractStatus.FixedTerm
      )
      val contractWithTemporaryAmounts = contract.copy(
        data = contract.data.toBuilder
          .setRentAmount(2342300)
          .setTemporaryAmounts(
            TemporaryAmounts
              .newBuilder()
              .setRentAmount(6000000)
              .setStartDate(DateTimeFormat.write(dt(2022, 10, 15)))
              .setDuration(1)
          )
          .build()
      )

      val firstItem = createContractHistoryItem(contractWithTemporaryAmounts, dt(2022, 6, 15), dt(2022, 7, 15), 15)
      val secondItem = createContractHistoryItem(contractWithTemporaryAmounts, dt(2022, 7, 15), dt(2022, 10, 15), 15)
      val secondTemporaryAmounts = TemporaryAmounts
        .newBuilder()
        .setRentAmount(6000000)
        .setStartDate(dt(2022, 7, 15))
        .setInsurance(Insurance.newBuilder().setInsuranceAmount(calculateRentInsuranceInKopecks(6000000)).build())
        .setDuration(1)
        .build()
      val secondHistoryItemWithTemporaryAmounts = secondItem.toBuilder
        .setFields(secondItem.getFields.toBuilder.setTemporaryAmounts(secondTemporaryAmounts))
        .build()

      val thirdItem = createContractHistoryItem(contractWithTemporaryAmounts, dt(2022, 10, 15), dt(2022, 10, 15), 15)
      val thirdTemporaryAmounts = TemporaryAmounts
        .newBuilder()
        .setRentAmount(6000000)
        .setStartDate(dt(2022, 10, 15))
        .setDuration(2)
        .setInsurance(
          Insurance.newBuilder().setInsuranceAmount(calculateRentInsuranceInKopecks(6000000)).build()
        )
        .setDuration(1)
        .build()
      val thirdHistoryItemWithTemporaryAmounts = thirdItem.toBuilder
        .setFields(thirdItem.getFields.toBuilder.setTemporaryAmounts(thirdTemporaryAmounts))
        .build()

      val contractWithHistory = contractWithTemporaryAmounts.withHistory(
        firstItem,
        secondHistoryItemWithTemporaryAmounts,
        thirdHistoryItemWithTemporaryAmounts
      )

      val Seq(first, secondTemporary, second, last) = generator.buildConditionPeriods(contractWithHistory)

      assertPeriod(first, firstItem)

      assertPeriod(second, secondHistoryItemWithTemporaryAmounts)
      assertPeriodForTemporaryAmounts(secondTemporary, secondHistoryItemWithTemporaryAmounts)

      assertPeriodForTemporaryAmounts(last, contractWithHistory)
    }
  }

  private def assertPeriod(period: ConditionPeriod, contract: RentContract): Unit = {
    val startDate = contract.data.getChangeHistoryList.asScala.lastOption
      .map(_.getActualToDate: DateTime)
      .getOrElse(contract.getRentStartDate.get)
      .plusMonths(contract.data.getTemporaryAmounts.getDuration)

    period.startDate shouldBe startDate
    period.endDateOpt shouldBe contract.terminationDate.map(_.plusDays(1))

    period.conditions.rentAmount shouldBe contract.data.getRentAmount
    period.conditions.rentStartDate shouldBe contract.getRentStartDate.get
    period.conditions.paymentDayOfMonth shouldBe contract.data.getPaymentDayOfMonth
    period.conditions.insuranceAmount shouldBe contract.data.getInsurance.getInsuranceAmount
    period.conditions.calculationStrategy shouldBe contract.data.getCalculationStrategy
    period.conditions.commissions shouldBe contract.data.getCommissions
  }

  private def assertPeriod(period: ConditionPeriod, historyItem: ChangedContractFields): Unit = {
    val endDate = DateTimeFormat
      .read(historyItem.getActualFromDate)
      .plusMonths(historyItem.getFields.getTemporaryAmounts.getDuration)
    period.startDate shouldBe endDate
    period.endDateOpt shouldBe Some(historyItem.getActualToDate: DateTime)

    period.conditions.rentAmount shouldBe historyItem.getFields.getRentAmount
    period.conditions.rentStartDate shouldBe (historyItem.getFields.getRentStartDate: DateTime)
    period.conditions.paymentDayOfMonth shouldBe historyItem.getFields.getPaymentDayOfMonth
    period.conditions.insuranceAmount shouldBe historyItem.getFields.getInsurance.getInsuranceAmount
    period.conditions.calculationStrategy shouldBe historyItem.getFields.getCalculationStrategy
    period.conditions.commissions shouldBe historyItem.getFields.getCommissions
  }

  private def assertPeriodForTemporaryAmounts(
    periodForTemporaryAmounts: ConditionPeriod,
    historyItem: ChangedContractFields
  ): Unit = {
    val temporaryAmounts = historyItem.getFields.getTemporaryAmounts
    val startDate = DateTimeFormat.read(historyItem.getActualFromDate)
    val endDate = startDate.plusMonths(temporaryAmounts.getDuration)
    val insuranceAmount =
      if (temporaryAmounts.getInsurance.getInsuranceAmount > 0) temporaryAmounts.getInsurance.getInsuranceAmount
      else historyItem.getFields.getInsurance.getInsuranceAmount
    periodForTemporaryAmounts.startDate shouldBe startDate
    periodForTemporaryAmounts.endDateOpt shouldBe Some(endDate)
    periodForTemporaryAmounts.conditions.rentAmount shouldBe temporaryAmounts.getRentAmount
    periodForTemporaryAmounts.conditions.insuranceAmount shouldBe insuranceAmount
    periodForTemporaryAmounts.conditions.calculationStrategy shouldBe historyItem.getFields.getCalculationStrategy
    periodForTemporaryAmounts.conditions.commissions shouldBe historyItem.getFields.getCommissions
    periodForTemporaryAmounts.conditions.paymentDayOfMonth shouldBe historyItem.getFields.getPaymentDayOfMonth
    periodForTemporaryAmounts.conditions.rentStartDate shouldBe (historyItem.getFields.getRentStartDate: DateTime)
  }

  private def assertPeriodForTemporaryAmounts(
    periodForTemporaryAmounts: ConditionPeriod,
    contract: RentContract
  ): Unit = {
    val temporaryAmounts = contract.data.getTemporaryAmounts
    val startDate = contract.data.getChangeHistoryList.asScala.lastOption
      .map(_.getActualToDate: DateTime)
      .getOrElse(contract.getRentStartDate.get)
    val temporaryPeriodEndDate = startDate.plusMonths(temporaryAmounts.getDuration)
    val endDate = contract.terminationDate
      .map { terminationDate =>
        if (terminationDate.isBefore(temporaryPeriodEndDate)) terminationDate.plusDays(1)
        else temporaryPeriodEndDate
      }
      .getOrElse(temporaryPeriodEndDate)
    val insuranceAmount =
      if (temporaryAmounts.getInsurance.getInsuranceAmount > 0) temporaryAmounts.getInsurance.getInsuranceAmount
      else contract.data.getInsurance.getInsuranceAmount
    periodForTemporaryAmounts.startDate shouldBe startDate
    periodForTemporaryAmounts.endDateOpt shouldBe Some(endDate)
    periodForTemporaryAmounts.conditions.rentAmount shouldBe temporaryAmounts.getRentAmount
    periodForTemporaryAmounts.conditions.insuranceAmount shouldBe insuranceAmount
    periodForTemporaryAmounts.conditions.calculationStrategy shouldBe contract.data.getCalculationStrategy
    periodForTemporaryAmounts.conditions.commissions shouldBe contract.data.getCommissions
    periodForTemporaryAmounts.conditions.paymentDayOfMonth shouldBe contract.data.getPaymentDayOfMonth
    periodForTemporaryAmounts.conditions.rentStartDate shouldBe contract.getRentStartDate.get
  }
}
