package ru.yandex.realty.mortgages.services

import realty.palma.MortgageCalculatorParamsOuterClass
import realty.palma.MortgageCalculatorParamsOuterClass.MortgageCalculatorParams
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.storage.MortgageCalculatorParamsStorage

trait MortgageCalculatorParamsProviderTestComponents {

  val mortgageCalculatorParamsProvider = new MortgageCalculatorParamsProvider(
    MortgageCalculatorParamsProviderTestComponents.mortgageCalculatorParamsProvider
  )

  val mskRegionId = Option(1)
  val spbRegionId = Option(10174)
  val mskAndSpbRegionId = Seq(1, 10174)
  val mskParams = MortgageCalculatorParamsProviderTestComponents.mskParams
  val defaultParams = MortgageCalculatorParamsProviderTestComponents.defaultParams
}

object MortgageCalculatorParamsProviderTestComponents {
  val defaultCode = "default"

  val defaultParams = MortgageCalculatorParams
    .newBuilder()
    .setCode(defaultCode)
    .setCostDefault(1000)
    .setCostMax(2000)
    .setDownpaymentDefault(20)
    .setDownpaymentMax(30)
    .setDownpaymentMin(10)
    .setPeriodDefault(5)
    .setPeriodMax(10)
    .setPeriodMin(1)
    .setRateDefault(10.0)
    .setRateMax(20.0)
    .setRateMin(5.0)
    .setSumMax(5000)
    .setSumMin(2000)
    .build()

  val mskCode = "msk"

  val mskParams = MortgageCalculatorParams
    .newBuilder()
    .setCode(mskCode)
    .setCostDefault(2000)
    .setCostMax(4000)
    .setDownpaymentDefault(30)
    .setDownpaymentMax(40)
    .setDownpaymentMin(15)
    .setPeriodDefault(10)
    .setPeriodMax(20)
    .setPeriodMin(5)
    .setRateDefault(15.0)
    .setRateMax(25.0)
    .setRateMin(7.0)
    .setSumMax(50000)
    .setSumMin(20000)
    .build()

  val mortgageCalculationParamsStorage = new MortgageCalculatorParamsStorage {
    override def default(): MortgageCalculatorParamsOuterClass.MortgageCalculatorParams = defaultParams

    override def getByCode(code: String): Option[MortgageCalculatorParamsOuterClass.MortgageCalculatorParams] =
      code match {
        case "default" => Some(defaultParams)
        case "1" => Some(mskParams)
        case _ => None
      }
  }

  val mortgageCalculatorParamsProvider = new Provider[MortgageCalculatorParamsStorage] {
    override def get(): MortgageCalculatorParamsStorage = mortgageCalculationParamsStorage
  }
}
