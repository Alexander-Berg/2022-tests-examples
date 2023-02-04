package ru.yandex.realty.mortgages.services

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.palma.MortgageCalculatorParamsOuterClass.MortgageCalculatorParams
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class MortgageCalculatorParamsProviderSpec extends SpecBase with MortgageCalculatorParamsProviderTestComponents {
  private val defaultPropertyCost: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.defaultPropertyCost
  private val maxPropertyCost: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.maxPropertyCost
  private val minCreditAmount: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.minCreditAmount
  private val maxCreditAmount: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.maxCreditAmount
  private val defaultPeriodYears: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.defaultPeriodYears
  private val minPeriodYears: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.minPeriodYears
  private val maxPeriodYears: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.maxPeriodYears

  private val defaultDownPaymentPercents: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal =
    _.defaultDownPaymentPercents
  private val minDownPaymentPercents: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal =
    _.minDownPaymentPercents
  private val maxDownPaymentPercents: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal =
    _.maxDownPaymentPercents
  private val defaultRate: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.defaultRate
  private val minRate: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.minRate
  private val maxRate: MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal = _.maxRate
  private val methodsMap =
    Map[MortgageCalculatorParamsProvider => Iterable[Int] => AnyVal, MortgageCalculatorParams => AnyVal](
      defaultPropertyCost -> (_.getCostDefault),
      maxPropertyCost -> (_.getCostMax),
      minCreditAmount -> (_.getSumMin.longValue),
      maxCreditAmount -> (_.getSumMax.longValue),
      defaultPeriodYears -> (_.getPeriodDefault),
      minPeriodYears -> (_.getPeriodMin),
      maxPeriodYears -> (_.getPeriodMax),
      defaultDownPaymentPercents -> (_.getDownpaymentDefault.toFloat),
      minDownPaymentPercents -> (_.getDownpaymentMin),
      maxDownPaymentPercents -> (_.getDownpaymentMax),
      defaultRate -> (_.getRateDefault.toFloat),
      minRate -> (_.getRateMin.toFloat),
      maxRate -> (_.getRateMax.toFloat)
    )

  "MortgageCalculatorParamsProvider in all methods" should {
    "return msk default param value if region is msk" in {
      val result = for {
        (f, g) <- methodsMap
        result = f(mortgageCalculatorParamsProvider)(mskRegionId)
        expectedResult = g(mskParams)
      } yield result == expectedResult
      result.forall(_ == true) shouldEqual true
    }

    "return default param value if region is not specified" in {
      val result = for {
        (f, g) <- methodsMap
        result = f(mortgageCalculatorParamsProvider)(None)
        expectedResult = g(defaultParams)
      } yield result == expectedResult
      result.forall(_ == true) shouldEqual true
    }

    "return default param value if region is specified but there is no config for specified region" in {
      val result = for {
        (f, g) <- methodsMap
        result = f(mortgageCalculatorParamsProvider)(spbRegionId)
        expectedResult = g(defaultParams)
      } yield result == expectedResult
      result.forall(_ == true) shouldEqual true
    }

    "return default param value if regionId.size > 1" in {
      val result = for {
        (f, g) <- methodsMap
        result = f(mortgageCalculatorParamsProvider)(mskAndSpbRegionId)
        expectedResult = g(defaultParams)
      } yield result == expectedResult
      result.forall(_ == true) shouldEqual true
    }
  }
}
