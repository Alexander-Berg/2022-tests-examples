package ru.yandex.realty.mortgages.services

import com.google.protobuf.{FloatValue, Int32Value, Int64Value}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.palma.MortgageProgramConditionOuterClass.MortgageProgramCondition
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.message.Mortgages.{MortgageProgram, MortgageProgramCreditParams}
import ru.yandex.realty.mortgages.model.{MortgageProgramCalculationParams, MortgageProgramCalculationResult}
import ru.yandex.realty.mortgages.mortgage.MortgageProgramPaymentParamsTransformer
import ru.yandex.realty.mortgages.mortgage.core.{MortgageProgramRuleApplier, MortgageProgramRuleMatcher}
import ru.yandex.realty.mortgages.mortgage.domain.{
  MortgageFactorStatuses,
  MortgageProgramContext,
  MortgageProgramPaymentParams
}
import ru.yandex.realty.mortgages.util.{MortgageCreditAmountTooLargeException, MortgageCreditAmountTooSmallException}
import ru.yandex.realty.proto.mortgage.api.MortgageCalculatorLimits
import ru.yandex.realty.storage.{
  MortgageProgramConditionStorage,
  MortgageProgramConditionStorageImpl,
  MortgageProgramStorage
}

import scala.util.Success

@RunWith(classOf[JUnitRunner])
class MortgageCalculationServiceSpec extends SpecBase with MortgageCalculatorParamsProviderTestComponents {
  private val mortgageProgramProvider = stub[Provider[MortgageProgramStorage]]
  private val mortgageProgramConditionsProvider = mock[Provider[MortgageProgramConditionStorage]]
  private val mortgageProgramRuleMatcher = mock[MortgageProgramRuleMatcher]
  private val mortgageProgramRuleApplier = mock[MortgageProgramRuleApplier]
  private val paymentParamsTransformer = new MortgageProgramPaymentParamsTransformer(
    mortgageProgramConditionsProvider,
    mortgageProgramRuleMatcher,
    mortgageProgramRuleApplier
  )
  private val mortgageCalculationService =
    new MortgageCalculationService(mortgageProgramProvider, mortgageCalculatorParamsProvider, paymentParamsTransformer)

  private val defaultLimits = buildDefaultLimits
  private val mskLimits = buildMskLimits

  private val emptyCalculationParams = MortgageProgramCalculationParams.empty()
  private val notEmptyCalculationParams = buildNotEmptyCalculationParams

  private val defaultCalculationResult = buildDefaultCalculationResult
  private val paramsCalculationResult = buildParamsCalculationResult
  private val mskCalculationResult = buildMskCalculationResult

  "MortgageCalculationService in calculateDefaultLimits" should {
    "return default limits if region is not specified" in {
      mortgageCalculationService.calculateDefaultLimits() shouldEqual defaultLimits
    }

    "return default limits if there is no config with specified region" in {
      mortgageCalculationService.calculateDefaultLimits(spbRegionId) shouldEqual defaultLimits
    }

    "return default limits if regionId.size > 1" in {
      mortgageCalculationService.calculateDefaultLimits(mskAndSpbRegionId) shouldEqual defaultLimits
    }

    "return region default limits if there is config with specified region" in {
      mortgageCalculationService.calculateDefaultLimits(mskRegionId) shouldEqual mskLimits
    }
  }

  "MortgageCalculationService in calculatePayments" should {
    "return default calculation result if params and region are empty" in {
      mortgageCalculationService.calculatePayments(emptyCalculationParams, None) shouldEqual defaultCalculationResult
    }

    "return default calculation result if params is empty and region is specified" +
      " but there is no config for specified region" in {
      mortgageCalculationService.calculatePayments(emptyCalculationParams, spbRegionId) shouldEqual
        defaultCalculationResult
    }

    "return params calculation result if params are specified and region is empty" in {
      mortgageCalculationService.calculatePayments(notEmptyCalculationParams, None) shouldEqual paramsCalculationResult
    }

    "return params calculation result if params and region are specified" in {
      mortgageCalculationService.calculatePayments(notEmptyCalculationParams, mskRegionId) shouldEqual
        paramsCalculationResult
    }

    "return region calculation result if params is empty and region is specified" in {
      mortgageCalculationService.calculatePayments(emptyCalculationParams, mskRegionId) shouldEqual
        mskCalculationResult
    }
  }

  "MortgageCalculationService in calculatePayments" should {
    val mortgageProgramId = 123
    val mortgageProgram = buildMortgageProgram(mortgageProgramId)
    val mortgageProgramConditionStorage = buildMortgageProgramConditionStorage(mortgageProgramId)

    "return result if credit amount between minCreditAmount and maxCreditAmount" in {
      (mortgageProgramConditionsProvider.get _).expects().returning(mortgageProgramConditionStorage)
      (mortgageProgramRuleMatcher.doMatch _).expects(*, *).returning(Success(true))

      val paymentParams = buildMortgageProgramPaymentParams(1000000)
      (mortgageProgramRuleApplier.apply _)
        .expects(*, *)
        .returning(Success(buildMortgageProgramContext(mortgageProgram, paymentParams)))

      val expectedResult = buildMortgageProgramCalculationResult
      mortgageCalculationService.calculatePayments(mortgageProgram, paymentParams) shouldEqual expectedResult
    }

    "throw MortgageCreditAmountTooSmallException if credit amount < minCreditAmount" in {
      (mortgageProgramConditionsProvider.get _).expects().returning(mortgageProgramConditionStorage)
      (mortgageProgramRuleMatcher.doMatch _).expects(*, *).returning(Success(true))

      val paymentParams = buildMortgageProgramPaymentParams(5000)
      (mortgageProgramRuleApplier.apply _)
        .expects(*, *)
        .returning(Success(buildMortgageProgramContext(mortgageProgram, paymentParams)))

      assertThrows[MortgageCreditAmountTooSmallException] {
        mortgageCalculationService.calculatePayments(mortgageProgram, paymentParams)
      }
    }

    "throw MortgageCreditAmountTooLargeException if credit amount > maxCreditAmount" in {
      (mortgageProgramConditionsProvider.get _).expects().returning(mortgageProgramConditionStorage)
      (mortgageProgramRuleMatcher.doMatch _).expects(*, *).returning(Success(true))

      val paymentParams = buildMortgageProgramPaymentParams(50000000)
      (mortgageProgramRuleApplier.apply _)
        .expects(*, *)
        .returning(Success(buildMortgageProgramContext(mortgageProgram, paymentParams)))

      assertThrows[MortgageCreditAmountTooLargeException] {
        mortgageCalculationService.calculatePayments(mortgageProgram, paymentParams)
      }
    }
  }

  private def buildDefaultLimits =
    MortgageCalculatorLimits
      .newBuilder()
      .setMinCreditAmount(FloatValue.of(2000.0f))
      .setMaxCreditAmount(FloatValue.of(5000.0f))
      .setMinPropertyCost(Int64Value.of(2223))
      .setMaxPropertyCost(Int64Value.of(2000))
      .setMinPeriodYears(Int32Value.of(1))
      .setMaxPeriodYears(Int32Value.of(10))
      .setMinDownPayment(FloatValue.of(10.0f))
      .setMinDownPaymentSum(Int64Value.of(223))
      .setMaxDownPayment(FloatValue.of(30.0f))
      .setMaxDownPaymentSum(Int64Value.of(600))
      .setMinRate(FloatValue.of(5.0f))
      .setMaxRate(FloatValue.of(20.0f))
      .build()

  private def buildMskLimits =
    MortgageCalculatorLimits
      .newBuilder()
      .setMinCreditAmount(FloatValue.of(20000.0f))
      .setMaxCreditAmount(FloatValue.of(50000.0f))
      .setMinPropertyCost(Int64Value.of(23530))
      .setMaxPropertyCost(Int64Value.of(4000))
      .setMinPeriodYears(Int32Value.of(5))
      .setMaxPeriodYears(Int32Value.of(20))
      .setMinDownPayment(FloatValue.of(15.0f))
      .setMinDownPaymentSum(Int64Value.of(3530))
      .setMaxDownPayment(FloatValue.of(40.0f))
      .setMaxDownPaymentSum(Int64Value.of(1600))
      .setMinRate(FloatValue.of(7.0f))
      .setMaxRate(FloatValue.of(25.0f))
      .build()

  private def buildNotEmptyCalculationParams = MortgageProgramCalculationParams(
    propertyCost = Some(2000),
    periodYears = Some(5),
    downPaymentPercents = Some(15.0f),
    downPaymentSum = Some(500),
    rate = Some(5.0f)
  )

  private def buildDefaultCalculationResult =
    MortgageProgramCalculationResult(
      propertyCost = 1000,
      periodYears = 5,
      downPaymentPercents = 20.0f,
      downPaymentSum = 200,
      rate = 10.0f,
      creditAmount = 800,
      monthlyPayment = 17,
      overpayment = 220
    )

  private def buildParamsCalculationResult =
    MortgageProgramCalculationResult(
      propertyCost = 2000,
      periodYears = 5,
      downPaymentPercents = 25.0f,
      downPaymentSum = 500,
      rate = 5.0f,
      creditAmount = 1500,
      monthlyPayment = 28,
      overpayment = 180
    )

  private def buildMskCalculationResult =
    MortgageProgramCalculationResult(
      propertyCost = 2000,
      periodYears = 10,
      downPaymentPercents = 30.0f,
      downPaymentSum = 600,
      rate = 15.0f,
      creditAmount = 1400,
      monthlyPayment = 23,
      overpayment = 1360
    )

  private def buildMortgageProgramConditionStorage(programId: Long): MortgageProgramConditionStorage = {
    new MortgageProgramConditionStorageImpl(
      Seq(MortgageProgramCondition.newBuilder().setMortgageProgramId(programId.toString).build())
    )
  }

  private def buildMortgageProgram(programId: Long): MortgageProgram = {
    MortgageProgram
      .newBuilder()
      .setBankId(123)
      .setId(programId)
      .setCreditParams(
        MortgageProgramCreditParams
          .newBuilder()
          .setMinAmount(Int64Value.of(300000))
          .setMaxAmount(Int64Value.of(5000000))
      )
      .build()
  }

  private def buildMortgageProgramPaymentParams(creditAmount: Long) = {
    MortgageProgramPaymentParams(
      propertyCost = creditAmount + 100000,
      downPaymentSum = 100000,
      creditAmount = creditAmount,
      rate = 5.0f,
      periodYears = 10
    )
  }

  private def buildMortgageProgramCalculationResult =
    MortgageProgramCalculationResult(
      propertyCost = 1100000,
      periodYears = 10,
      downPaymentPercents = 9.09f,
      downPaymentSum = 100000,
      rate = 5.0f,
      creditAmount = 1000000,
      monthlyPayment = 10607,
      overpayment = 272840
    )

  private def buildMortgageProgramContext(
    mortgageProgram: MortgageProgram,
    paymentParams: MortgageProgramPaymentParams
  ): MortgageProgramContext = {
    MortgageProgramContext(
      mortgageProgram = mortgageProgram,
      paymentParams = paymentParams,
      siteId = None,
      developerId = None,
      factors = MortgageFactorStatuses(Map.empty)
    )
  }

}
