package ru.yandex.realty.mortgages

import com.google.protobuf.{FloatValue, Int32Value, Int64Value, StringValue}
import org.scalacheck.Gen
import realty.palma.MortgageProgramConditionOuterClass.IfConditionOperatorTypeNamespace.IfConditionOperatorType
import realty.palma.MortgageProgramConditionOuterClass.MortgageParamTypeNamespace.MortgageParamType
import realty.palma.MortgageProgramConditionOuterClass.{IfCondition, MortgageParamValue}
import ru.yandex.realty.model.message.Mortgages.{MortgageProgram, MortgageProgramCreditParams}
import ru.yandex.realty.mortgages.mortgage.domain.{
  MortgageFactorStatuses,
  MortgageProgramContext,
  MortgageProgramPaymentParams
}
import ru.yandex.realty.mortgages.services.MortgageCalculationService.calculateDownPaymentSum
import ru.yandex.extdata.core.gens.Producer._

import scala.collection.JavaConverters._

object MortgageProgramSpecUtils {

  val creditSumGen: Gen[Long] = Gen.chooseNum(100000, 100000000)
  val propertyCostGen: Gen[Long] = Gen.chooseNum(1000000, 30000000)
  val periodYearsGen: Gen[Int] = Gen.chooseNum(10, 30)
  val downPaymentPercentsGen: Gen[Float] = Gen.chooseNum(100, 1000).map(_.toFloat).map(_ / 100)
  val rateGen: Gen[Float] = Gen.chooseNum(10, 100).map(_.toFloat).map(_ / 10)

  def mortgageProgramParamsGen(
    rate: Option[Float] = None,
    downPaymentPercents: Option[Float] = None
  ): Gen[MortgageProgramPaymentParams] =
    for {
      propertyCost <- propertyCostGen
      downPaymentPercents <- downPaymentPercents.map(Gen.const).getOrElse(downPaymentPercentsGen)
      downPaymentSum = calculateDownPaymentSum(propertyCost, downPaymentPercents)
      creditAmount = propertyCost - downPaymentSum
      rate <- rate.map(Gen.const).getOrElse(rateGen)
      periodYears <- periodYearsGen
    } yield {
      MortgageProgramPaymentParams(
        propertyCost = propertyCost,
        downPaymentPercents = downPaymentPercents,
        downPaymentSum = downPaymentSum,
        creditAmount = creditAmount,
        rate = rate,
        periodYears = periodYears
      )
    }

  def mortgageProgramGen(
    minCreditSum: Option[Long] = None,
    maxCreditSum: Option[Long] = None,
    minDownPaymentPercents: Option[Float] = None,
    minPeriodYears: Option[Int] = None,
    maxPeriodYears: Option[Int] = None
  ): Gen[MortgageProgram] =
    for {
      id <- Gen.posNum[Long]
      creditParams <- mortgageProgramCreditParamsGen(
        minCreditSum,
        maxCreditSum,
        minDownPaymentPercents,
        minPeriodYears,
        maxPeriodYears
      )
    } yield {
      MortgageProgram
        .newBuilder()
        .setId(id)
        .setCreditParams(creditParams)
        .build()
    }

  def mortgageProgramCreditParamsGen(
    minCreditSum: Option[Long] = None,
    maxCreditSum: Option[Long] = None,
    minDownPaymentPercents: Option[Float] = None,
    minPeriodYears: Option[Int] = None,
    maxPeriodYears: Option[Int] = None
  ): Gen[MortgageProgramCreditParams] =
    for {
      rate <- rateGen
      minCreditSum <- minCreditSum
        .map(Gen.const)
        .orElse(maxCreditSum.map(m => creditSumGen.map(Math.min(_, m))))
        .getOrElse(creditSumGen)
      maxCreditSum <- maxCreditSum
        .map(Gen.const)
        .getOrElse(creditSumGen.map(Math.max(_, minCreditSum)))
      minDownPaymentPercents <- minDownPaymentPercents
        .map(Gen.const)
        .getOrElse(downPaymentPercentsGen)
      minPeriodYears <- minPeriodYears
        .map(Gen.const)
        .orElse(maxPeriodYears.map(m => periodYearsGen.map(Math.min(_, m))))
        .getOrElse(periodYearsGen)
      maxPeriodYears <- maxPeriodYears
        .map(Gen.const)
        .getOrElse(periodYearsGen.map(Math.max(_, minPeriodYears)))
    } yield {
      MortgageProgramCreditParams
        .newBuilder()
        .setMinRate(FloatValue.of(rate))
        .setMinAmount(Int64Value.of(minCreditSum))
        .setMaxAmount(Int64Value.of(maxCreditSum))
        .setMinFirstPay(FloatValue.of(minDownPaymentPercents))
        .setMinPeriodYears(Int32Value.of(minPeriodYears))
        .setMaxPeriodYears(Int32Value.of(maxPeriodYears))
        .build()
    }

  def asMortgageParamValue(value: Long): MortgageParamValue = {
    MortgageParamValue
      .newBuilder()
      .setIntValue(Int64Value.of(value))
      .build()
  }

  def asMortgageParamValue(value: Int): MortgageParamValue = {
    MortgageParamValue
      .newBuilder()
      .setIntValue(Int64Value.of(value.toLong))
      .build()
  }

  def asMortgageParamValue(value: Float): MortgageParamValue = {
    MortgageParamValue
      .newBuilder()
      .setFloatValue(FloatValue.of(value))
      .build()
  }

  def asMortgageParamValue(value: String): MortgageParamValue = {
    MortgageParamValue
      .newBuilder()
      .setStringValue(StringValue.of(value))
      .build()
  }

  def asMortgageProgramContext(
    mortgageProgram: MortgageProgram = mortgageProgramGen().next,
    paymentParams: MortgageProgramPaymentParams = mortgageProgramParamsGen().next,
    siteId: Option[Long] = None,
    developerId: Option[Long] = None,
    factors: MortgageFactorStatuses = MortgageFactorStatuses.Empty
  ): MortgageProgramContext = {
    MortgageProgramContext(
      mortgageProgram = mortgageProgram,
      paymentParams = paymentParams,
      siteId = siteId,
      developerId = developerId,
      factors = factors
    )
  }

  def buildIfCondition(param: MortgageParamType, operatorType: IfConditionOperatorType, value: Int): IfCondition = {
    buildIfConditionBuilder(param, operatorType)
      .addValues(asMortgageParamValue(value))
      .build()
  }

  def buildIfCondition(param: MortgageParamType, operatorType: IfConditionOperatorType, value: Long): IfCondition = {
    buildIfConditionBuilder(param, operatorType)
      .addValues(asMortgageParamValue(value))
      .build()
  }

  def buildIfCondition(param: MortgageParamType, operatorType: IfConditionOperatorType, value: Float): IfCondition = {
    buildIfConditionBuilder(param, operatorType)
      .addValues(asMortgageParamValue(value))
      .build()
  }

  def buildIfCondition(
    param: MortgageParamType,
    operatorType: IfConditionOperatorType,
    values: Seq[Long]
  ): IfCondition = {
    buildIfConditionBuilder(param, operatorType)
      .addAllValues(values.map(asMortgageParamValue).asJava)
      .build()
  }

  private def buildIfConditionBuilder(
    param: MortgageParamType,
    operatorType: IfConditionOperatorType
  ): IfCondition.Builder = {
    IfCondition
      .newBuilder()
      .setParam(param)
      .setOperator(operatorType)
  }
}
